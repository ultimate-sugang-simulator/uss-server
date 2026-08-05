# [PLAN-68] 인증 필터에 막히는 CORS, actuator 요청 개방

> 이슈: #68
> 브랜치: fix/68-cors-preflight

## 목표

CORS 처리를 `JwtAuthenticationFilter`보다 앞선 서블릿 필터로 끌어올려 프리플라이트가 401로 끊기지 않게 하고, 인증이 필요 없는 경로(`/actuator/**`)를 화이트리스트에 반영한다. 허용 오리진에 로컬 프론트 개발 서버를 추가한다.

## 영향 범위

### 신규 파일
- 없음

### 수정 파일
- `src/main/java/uss/code/global/config/CorsConfig.java` - `WebMvcConfigurer` 구현을 걷어내고 `CorsConfigurationSource` 빈만 제공하도록 전환, 허용 오리진에 `http://localhost:5173` 추가
- `src/main/java/uss/code/global/config/FilterChainConfig.java` - `CorsFilter`를 최상단 순서로 등록하고 기존 필터 순서 상수를 한 칸씩 밀어냄
- `src/main/java/uss/code/global/http/WhitelistEndpoint.java` - `/actuator/**` 추가, 와일드카드 접두 매칭의 경계 보정
- `.claude/spec/service-policy/auth.md` - 인증 예외 경로 목록에 모니터링 경로 추가

## 구현 계획

### 1. CorsConfig - CorsConfigurationSource 빈으로 전환

`WebMvcConfigurer.addCorsMappings`는 CORS를 DispatcherServlet 안쪽(핸들러 매핑 단계)에서 처리한다. 서블릿 필터인 `JwtAuthenticationFilter`가 그보다 앞서므로 프리플라이트가 여기까지 도달하지 못한다. CORS 설정을 필터가 쓸 수 있는 형태로 분리한다.

```java
@Configuration
public class CorsConfig {

    private static final String ALL_PATH_PATTERN = "/**";
    private static final String ALL_HEADER_PATTERN = "*";
    private static final long MAX_AGE = 3600L;

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://uss.inuappcenter.kr"
    );
    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH");

    @Bean
    public CorsConfigurationSource corsConfigurationSource() { ... }
}
```

- `implements WebMvcConfigurer`와 `addCorsMappings` 오버라이드를 제거한다.
- `CorsConfiguration`에 위 상수를 채우고 `setAllowCredentials(true)`, `setMaxAge(MAX_AGE)`를 건다.
- `UrlBasedCorsConfigurationSource`에 `ALL_PATH_PATTERN`으로 등록해 반환한다.
- 노출 헤더(`exposedHeaders`)는 두지 않는다. 액세스 토큰은 헤더가 아니라 응답 본문(`AuthTokenResponse`)으로 내려간다.

### 2. FilterChainConfig - CorsFilter 등록과 순서 재배치

```java
private static final int CORS_FILTER_ORDER = 0;
private static final int HTTP_LOGGING_FILTER_ORDER = 1;
private static final int JWT_EXCEPTION_FILTER_ORDER = 2;
private static final int JWT_AUTHENTICATION_FILTER_ORDER = 3;

private final CorsConfigurationSource corsConfigurationSource;

@Bean
public FilterRegistrationBean<CorsFilter> corsFilter() { ... }
```

- `org.springframework.web.filter.CorsFilter`를 `corsConfigurationSource`로 생성해 `CORS_FILTER_ORDER`로 등록한다.
- 기존 세 필터의 순서 상수를 1, 2, 3으로 밀어낸다.
- CORS 필터를 최상단에 두는 이유는 두 가지다. 프리플라이트(OPTIONS)는 `CorsFilter`가 응답을 만들고 체인을 더 태우지 않으므로 인증 필터에 닿지 않는다. 그리고 `JwtExceptionFilter`가 만들어내는 401 응답에도 CORS 헤더가 붙어, 브라우저가 인증 실패를 CORS 오류로 뭉개지 않는다.
- 이 구조에서는 `WhitelistEndpoint`에 OPTIONS를 따로 열어줄 필요가 없다. 화이트리스트를 넓히지 않고 프리플라이트만 통과시키는 쪽이 노출면이 좁다.

### 3. WhitelistEndpoint - actuator 개방과 매칭 경계 보정

```java
private static final String PATH_WILDCARD_SUFFIX = "/**";
private static final String PATH_DELIMITER = "/";

private static final List<EndPoint> WHITELIST = List.of(
        new EndPoint("/api/v1/auth/login", HttpMethod.POST),
        new EndPoint("/api/v1/auth/re-issue", HttpMethod.POST),
        new EndPoint("/actuator/**", null),
        new EndPoint("/swagger-ui/**", null),
        new EndPoint("/v3/api-docs/**", null),
        new EndPoint("/swagger-resources/**", null)
);
```

- `/actuator/**`를 추가한다. `InterceptorConfig`와 `HttpLoggingFilter`가 이미 액추에이터를 제외 경로로 취급하는데 화이트리스트에만 빠져 있어 기준이 어긋나 있었다. prod는 관리 포트가 분리되어 영향이 없고, 관리 포트를 나누지 않는 로컬과 dev에서 401이 난다.
- `EndPoint.isPathMatch`의 경계를 좁힌다. 현재는 `/**`를 잘라낸 접두사로 `startsWith`만 보기 때문에 `/actuator`가 `/actuatorX` 같은 경로까지 통과시킨다. 정확히 기준 경로이거나 그 하위 경로일 때만 통과하도록 바꾼다.

```java
private boolean isPathMatch(final String uri) {
    if (!path.endsWith(PATH_WILDCARD_SUFFIX)) {
        return path.equals(uri);
    }

    final String basePath = path.substring(0, path.length() - PATH_WILDCARD_SUFFIX.length());

    return uri.equals(basePath) || uri.startsWith(basePath + PATH_DELIMITER);
}
```

### 4. 서비스 정책 갱신

`.claude/spec/service-policy/auth.md`의 "인증 예외 경로" 목록에 모니터링 경로를 추가한다. 이 문서가 인증 예외의 단일 출처이고, 예외 경로 추가는 곧 공개 범위 변경이다.

### 범위 밖으로 두는 것

- prod에서 Swagger 경로를 닫는 일. 프로파일 분기가 필요해 설정 파일과 배포 구성을 함께 건드려야 하므로 별도 이슈로 분리한다.
- `allowedHeaders`를 `*`에서 실제 사용 헤더로 좁히는 일. 허용 오리진이 명시적으로 열거되어 있어 실익 대비 회귀 위험이 크다.
- `allowedMethods`에서 미사용 메서드(PUT, PATCH) 제거. 현재 열려 있어 생기는 노출이 없다.

## 결정 필요 (Decisions needed)

- 없음. 접근 방식(Spring Security 미도입, 현행 커스텀 필터 유지)과 허용 오리진(기존 유지 + `http://localhost:5173` 추가)은 이슈 발의 단계에서 확정했다.

## 검증

- 자동 테스트는 이번 작업에서 작성하지 않는다 (사용자 지시).
- `./gradlew build`로 컴파일과 기존 테스트 회귀만 확인한다.
- 수동 확인 항목
  - 인증이 필요한 경로에 `Origin: http://localhost:5173`으로 OPTIONS를 보내면 401이 아니라 CORS 헤더가 담긴 성공 응답이 온다
  - 토큰 없이 인증 경로를 호출하면 401이 나오되 응답에 `Access-Control-Allow-Origin`이 붙는다
  - 관리 포트를 나누지 않은 상태에서 `/actuator/health`가 토큰 없이 200을 준다
  - `/actuator`로 시작하지만 하위 경로가 아닌 URI는 화이트리스트를 통과하지 못한다

## Deviation Log

- `.claude/spec/service-policy/auth.md`: 모니터링 경로 한 줄 추가에 더해 와일드카드 매칭 경계와 프리플라이트 처리 두 문장을 함께 넣음 - 이유: 두 항목 모두 이번 구현으로 실제 판정 기준이 바뀐 부분인데 목록 한 줄로는 드러나지 않아, 문서가 코드와 어긋난 채 남는다.
