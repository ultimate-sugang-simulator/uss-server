# [PLAN-59] 리프레시 토큰 제거와 재발급 API 도입

> 이슈: #59
> 브랜치: refactor/59-remove-refresh-token

## 목표

회원을 식별하지 못해 재발급에 쓸 수 없으면서 매 요청 검증 비용만 발생시키던 리프레시 토큰을 제거하고,
만료된 액세스 토큰을 자격 증명으로 받아 새 액세스 토큰을 발급하는 재발급 경로를 추가한다.
인증 요청은 `access-token` 헤더 하나만 보내면 되고, 로그인 응답도 액세스 토큰 단일 필드가 된다.

## 영향 범위

### 신규 파일

없음. 재발급 API는 기존 `AuthController`, `AuthService`, `AuthTokenResponse`를 재사용한다.

### 수정 파일

인증 핵심

- `src/main/java/uss/code/auth/infra/JwtProvider.java` - 리프레시 토큰 발급·검증 제거, 만료 허용 파싱 메서드 추가
- `src/main/java/uss/code/auth/filter/JwtAuthenticationFilter.java` - `refresh-token` 헤더 조회 제거, 액세스 토큰만 검증
- `src/main/java/uss/code/auth/dto/response/AuthTokenResponse.java` - `refreshToken` 필드 제거, `@Schema` 추가
- `src/main/java/uss/code/auth/service/AuthService.java` - `reIssue` 추가
- `src/main/java/uss/code/auth/controller/AuthController.java` - `POST /re-issue` 핸들러 추가
- `src/main/java/uss/code/auth/controller/AuthControllerDocs.java` - 재발급 API 문서 추가

전역 설정

- `src/main/java/uss/code/global/http/WhitelistEndpoint.java` - 재발급 경로를 인증 예외에 등록
- `src/main/java/uss/code/global/exception/domain/ExceptionCode.java` - 리프레시 토큰 에러코드 5건 제거
- `src/main/java/uss/code/global/exception/handler/GlobalExceptionHandler.java` - `JwtAuthenticationException` 핸들러 추가
- `src/main/java/uss/code/global/config/SwaggerConfig.java` - `refresh-token` 시큐리티 스킴 제거
- `src/main/java/uss/code/global/filter/HttpLoggingFilter.java` - 마스킹 대상에서 `refresh-token` 제거

환경 설정

- `src/main/resources/application-prod.yml` - `refresh-token-expiration-time` 제거
- `src/test/resources/application.yml` - `refresh-token-expiration-time` 제거

문서

- `.claude/spec/service-policy/auth.md` - 토큰 정책과 인증 예외 경로 갱신

## 구현 계획

### 1. Entity / Flyway

DB 변경 없음. 토큰은 저장하지 않으며 이번 작업에서도 저장소를 도입하지 않는다.

### 2. Repository

`MemberRepository` 수정 없음. 재발급 시 회원 존재 확인은 `JpaRepository`가 기본 제공하는 `existsById(Long)`를 쓴다.

### 3. JwtProvider

**제거**

- 필드 `private final long refreshTokenExpirationTime`와 생성자의 `@Value("${security.jwt.refresh-token-expiration-time}")` 파라미터
- 상수 `private static final int NO_SUBJECT = -1`
- `private void validateRefreshToken(final String refreshToken)` 메서드 전체

**변경**

- `generateAuthTokens(long)` → `generateAuthToken(final long memberId)`
  액세스 토큰만 만들어 `AuthTokenResponse.of(accessToken)`를 반환한다.

- `validateTokens(String, String)` → `validateToken(final String accessToken)`
  `accessToken == null`이면 `JwtTokenMissingException(MISSING_ACCESS_TOKEN)`, 아니면 `validateAccessToken(accessToken)` 호출.

**추가**

```java
public Long getMemberIdAllowingExpiration(final String accessToken) {
    if (accessToken == null)
        throw new JwtTokenMissingException(MISSING_ACCESS_TOKEN);

    try {
        return Long.valueOf(parseJwt(accessToken).getPayload().getSubject());
    } catch (ExpiredJwtException e) {
        return Long.valueOf(e.getClaims().getSubject());
    } catch (MalformedJwtException e) {
        throw new JwtTokenInvalidException(INVALID_FORM_ACCESS_TOKEN);
    } catch (SignatureException e) {
        throw new JwtTokenInvalidException(INVALID_SIGNATURE_ACCESS_TOKEN);
    } catch (JwtException | IllegalArgumentException e) {
        throw new JwtTokenInvalidException(INVALID_ACCESS_TOKEN);
    }
}
```

> `ExpiredJwtException`을 잡아 회원 식별자를 꺼내도 안전한 근거: jjwt 0.12.6의 `DefaultJwtParser`는
> 서명 검증(`integrityVerified = true`, 579행)을 만료 검사(`throw new ExpiredJwtException`, 691행)보다 **먼저** 수행한다.
> 즉 `ExpiredJwtException`이 던져졌다는 사실 자체가 우리 키로 서명된 토큰이라는 증명이다.
> 위조 토큰은 그 전에 `SignatureException`으로 걸러진다.
> `ExpiredJwtException`은 `ClaimJwtException`을 상속해 `getClaims()`로 payload를 그대로 들고 있다.

`getMemberId(String)`와 `validateAccessToken(String)`, `parseJwt(String)`, `generateToken(long, long)`은 그대로 둔다.

### 4. DTO

**`AuthTokenResponse`** - `refreshToken` 컴포넌트를 제거해 단일 필드 record로 만든다.
로그인과 재발급이 같은 응답을 쓰므로 재발급 전용 DTO는 만들지 않는다.

```java
@Builder(access = AccessLevel.PRIVATE)
public record AuthTokenResponse(
        @Schema(
                description = "액세스 토큰",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String accessToken
) {
    public static AuthTokenResponse of(final String accessToken) {
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
```

`of` 파라미터가 1개가 되므로 줄바꿈 없이 한 줄로 쓴다 (포맷팅 규칙은 2개 이상일 때 적용).

### 5. Service

**`AuthService`**

- `login(LoginRequest)`의 마지막 줄을 `jwtProvider.generateAuthToken(member.getId())`로 바꾼다. 나머지 로직은 그대로.
- `reIssue`를 추가한다.

```java
@Transactional(readOnly = true)
public AuthTokenResponse reIssue(final String accessToken) {
    final Long memberId = jwtProvider.getMemberIdAllowingExpiration(accessToken);

    if (!memberRepository.existsById(memberId))
        throw new RestApiException(MEMBER_NOT_FOUND);

    return jwtProvider.generateAuthToken(memberId);
}
```

> 회원 존재 확인을 넣는 이유: 토큰 저장소가 없어 폐기 수단이 없는 구조에서,
> 탈퇴하거나 삭제된 회원의 토큰이 무기한 재발급되는 것을 막는 유일한 방어선이다.

### 6. Controller

**`AuthController`** - 헤더명은 클래스 상단 상수로 뽑는다 (`@RequestHeader` 값은 상수 표현식이어야 하므로 `private static final String`이어야 한다).

```java
private static final String ACCESS_TOKEN_HEADER = "access-token";

@PostMapping("/re-issue")
public ResponseEntity<AuthTokenResponse> reIssue(
        @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) final String accessToken
) {
    return ResponseEntity.status(OK).body(authService.reIssue(accessToken));
}
```

> `required = false`로 두는 이유: 기본값(`true`)이면 헤더 누락 시 Spring이 `MissingRequestHeaderException`을 던져
> `GlobalExceptionHandler`의 `Exception.class` 핸들러에 걸려 9999/500이 나간다.
> `null`을 그대로 넘겨 `JwtProvider`가 `MISSING_ACCESS_TOKEN`(1000, 401)을 던지게 해야 필터 경로와 응답이 일치한다.

**`JwtAuthenticationFilter`** - `refresh-token` 헤더 조회 줄을 지우고 `validateToken(accessToken)`만 호출한다.
`"access-token"` 리터럴도 클래스 상수로 뽑는다.

```java
final String accessToken = request.getHeader(ACCESS_TOKEN_HEADER);

jwtProvider.validateToken(accessToken);

final Long memberId = jwtProvider.getMemberId(accessToken);
request.setAttribute("member-id", memberId);

filterChain.doFilter(request, response);
```

### 7. 전역 설정

**`WhitelistEndpoint`** - `WHITELIST`에 재발급 경로를 추가한다. 필터를 타면 만료된 토큰이 401로 걸려 재발급 자체가 불가능하므로 반드시 필요하다.

```java
new EndPoint("/api/v1/auth/re-issue", HttpMethod.POST),
```

**`GlobalExceptionHandler`** - `JwtAuthenticationException` 핸들러를 추가한다.

```java
@ExceptionHandler(JwtAuthenticationException.class)
public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(final JwtAuthenticationException e) {
    log.error("예외 발생: {}", e.getMessage());
    return ResponseEntity.status(UNAUTHORIZED).body(ErrorResponse.of(e.getCode(), e.getMessage()));
}
```

> 지금까지 `JwtAuthenticationException`은 필터에서만 던져져 `JwtExceptionFilter`가 401로 응답했다.
> 재발급은 컨트롤러 안에서 토큰을 파싱하므로 예외가 DispatcherServlet에 잡히고,
> 더 구체적인 핸들러가 없으면 `@ExceptionHandler(Exception.class)`에 걸려 **9999 / 500**이 나간다.
> `JwtAuthenticationException`은 `ExceptionCode`가 아니라 `code`, `message`만 들고 있어
> `JwtExceptionFilter`와 동일하게 상태코드를 401로 고정한다.

**`ExceptionCode`** - `// 리프레시 토큰` 주석 그룹 전체(1005~1009) 제거.

- `MISSING_REFRESH_TOKEN(1005)`, `INVALID_REFRESH_TOKEN(1006)`, `INVALID_FORM_REFRESH_TOKEN(1007)`,
  `INVALID_SIGNATURE_REFRESH_TOKEN(1008)`, `EXPIRED_REFRESH_TOKEN(1009)`

> 비는 번호 1005~1009는 재사용하지 않고 결번으로 남긴다. 뒤 항목을 당겨 쓰면
> `MEMBER_NOT_FOUND(1010)` 등 무관한 코드의 값이 바뀌어 클라이언트 분기가 깨진다.
> PLAN-57에서 1011, 1013~1022를 결번 처리한 것과 같은 기준이다.

**`SwaggerConfig`** - `REFRESH_TOKEN_KEY` 상수, `refreshTokenSecurityScheme` 지역변수,
`components.addSecuritySchemes(REFRESH_TOKEN_KEY, ...)`, `requirement.addList(REFRESH_TOKEN_KEY)`를 모두 제거한다.
`ACCESS_TOKEN_KEY` 관련 코드만 남는다.

**`HttpLoggingFilter`** - `SENSITIVE_KEYS`에서 `"refresh-token"`을 뺀다. `"access-token"`, `"password"`는 유지.

**`application-prod.yml`, `src/test/resources/application.yml`** - `security.jwt.refresh-token-expiration-time` 줄 제거.
`.github/workflows/cd-prod.yml`은 `security.jwt.secret-key`만 주입하므로 수정 대상이 아니다.

### 8. API 문서

**`AuthControllerDocs`** - `reIssue` 메서드 시그니처와 문서를 추가한다.

- `@Operation(summary = "액세스 토큰 재발급", description = "만료된 액세스 토큰으로 새 액세스 토큰을 발급받습니다.<br>🔓 <strong>Jwt 불필요</strong><br>")`
- 응답
  - `200` - `"✅ 재발급 성공"`
  - `401` - `"🚨 액세스 토큰 누락 또는 유효하지 않음"`
    - `{"code" : 1000, "message" : "액세스 토큰이 누락되었습니다."}`
    - `{"code" : 1001, "message" : "액세스 토큰이 유효하지 않습니다."}`
    - `{"code" : 1002, "message" : "액세스 토큰 형식이 올바르지 않습니다."}`
    - `{"code" : 1003, "message" : "액세스 토큰 서명이 유효하지 않습니다."}`
  - `404` - `"🚨 사용자 조회 실패"`
    - `{"code" : 1010, "message" : "사용자를 찾을 수 없습니다."}`

> `EXPIRED_ACCESS_TOKEN(1004)`은 재발급 응답 예시에 넣지 않는다. 만료는 재발급의 실패 사유가 아니라 정상 입력이다.
> 기존 `login` 문서는 그대로 둔다.

### 9. 문서

**`.claude/spec/service-policy/auth.md`**

- `## 로그인`의 "성공하면 액세스 토큰과 리프레시 토큰을 함께 발급한다" → "성공하면 액세스 토큰을 발급한다"
- `## 토큰` 섹션을 아래 내용으로 교체
  - 액세스 토큰은 회원 식별자를 담는다. 리프레시 토큰은 두지 않는다
  - 인증이 필요한 요청은 액세스 토큰 하나만 보낸다. 없거나 유효하지 않으면 실패한다
  - 만료와 위조는 서로 다른 실패로 구분해 응답한다
- `## 토큰 재발급` 섹션 신설
  - 만료된 액세스 토큰을 보내면 새 액세스 토큰을 발급한다. 서명이 유효해야 하며 만료 여부는 묻지 않는다
  - 토큰의 회원이 존재하지 않으면 실패한다
  - 토큰을 저장하지 않으므로 개별 폐기와 강제 로그아웃은 지원하지 않는다
- `## 인증 예외 경로`에 재발급 항목 추가

## 결정 필요 (Decisions needed)

- [x] **아직 만료되지 않은 액세스 토큰으로도 재발급을 허용할지** - **허용한다.**
  서명만 유효하면 만료 여부와 무관하게 새 토큰을 발급한다. 3번의 `getMemberIdAllowingExpiration` 구현이 그대로 확정안이다.
  클라이언트가 401을 받은 뒤에야 갱신할 수 있는 제약이 없어지고, 새 에러코드도 필요하지 않다.
  만료된 토큰만 받도록 좁히면 멀쩡한 토큰을 쓰는 요청을 굳이 실패시키게 되어 실익이 적다.

## 검증

- `./gradlew build` - 삭제한 `generateAuthTokens`, `validateTokens`, `NO_SUBJECT`, 리프레시 `ExceptionCode` 5건의 잔여 참조가 없는지 컴파일로 확인한다
- `./gradlew test` - 기존 테스트(`CartServiceTest`, `CourseServiceTest`, `MemberServiceTest`, `RegistrationServiceTest`)는 JWT 경로를 타지 않아 그대로 통과해야 한다. `application.yml`에서 프로퍼티를 지운 뒤 컨텍스트가 기동되는지가 실질 확인 지점이다
- auth 도메인에는 현재 테스트가 없다. `AuthServiceTest` 작성은 `write-test` 스킬로 별도 진행한다
- Swagger UI 수동 확인
  - Authorize 목록에 `access-token`만 남는지
  - 로그인 응답에 `refreshToken`이 사라졌는지
  - `refresh-token` 헤더 없이 인증 API가 통과하는지
  - `POST /api/v1/auth/re-issue`에 만료된 토큰을 넣어 200과 새 토큰이 오는지
  - 서명이 깨진 토큰으로는 1003/401, 헤더를 빼면 1000/401이 오는지 (500이 아님을 확인 - 7번 핸들러가 동작하는지 보는 지점)

## Deviation Log

- `src/main/java/uss/code/auth/controller/AuthControllerDocs.java`: `@Operation` description에 "서명이 유효하면 만료 여부와 관계없이 발급됩니다." 한 줄 추가 - 이유: 결정 사항(만료되지 않은 토큰도 허용)이 계획서 8번의 description 문자열에는 반영돼 있지 않았다. 문서만 읽는 클라이언트가 "만료된 토큰만 받는다"로 오해할 여지를 없앴다.
- `src/main/java/uss/code/auth/filter/JwtAuthenticationFilter.java`: `"member-id"`는 리터럴로 그대로 뒀다 - 이유: 상수로 뽑아봤으나 같은 문자열을 쓰는 `AuthArgumentResolver`는 이번 범위가 아니라 리터럴로 남는다. 한쪽만 상수화하면 중앙화된 것처럼 보여 오히려 오해를 만든다. 계획이 지시한 `ACCESS_TOKEN_HEADER`만 상수화했다.
