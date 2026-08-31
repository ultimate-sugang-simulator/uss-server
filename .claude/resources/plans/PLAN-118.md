# [PLAN-118] 에러 코드와 메시지 표기 체계 통일

> 이슈: #118
> 브랜치: refactor/118-exception-code-format

## 목표

`ExceptionCode`의 정수 코드를 `{도메인 접두사}-{순번}` 문자열로 바꾸고, 갈려 있는 메시지 어투를 "-요"체로 통일한다. 코드 값만 보고 어느 도메인의 오류인지 알 수 있게 해 프론트엔드와의 대조 비용을 없앤다.

## 영향 범위

### 신규 파일

없음.

### 수정 파일

**핵심 (타입 변경)**

- `src/main/java/uss/code/global/exception/domain/ExceptionCode.java` — `code` 필드를 `int`에서 `String`으로. 34개 상수의 코드 값과 메시지 문구를 아래 매핑표대로 교체
- `src/main/java/uss/code/global/exception/dto/response/ErrorResponse.java` — 컴포넌트 `int code`를 `String code`로, `of()` 파라미터도 같이
- `src/main/java/uss/code/global/exception/domain/JwtAuthenticationException.java` — 필드 `private final int code`를 `String`으로
- `src/main/java/uss/code/auth/filter/JwtExceptionFilter.java` — `setErrorResponse`의 `final int code` 파라미터를 `String`으로
- `src/main/java/uss/code/global/exception/handler/GlobalExceptionHandler.java` — `makeErrorResponse(String message)`의 하드코딩된 `400`을 실제 코드 값으로

**Swagger 예시 (48개)**

- `src/main/java/uss/code/registration/controller/RegistrationControllerDocs.java` (9개)
- `src/main/java/uss/code/auth/controller/AuthControllerDocs.java` (10개)
- `src/main/java/uss/code/cart/controller/CartControllerDocs.java` (8개)
- `src/main/java/uss/code/admin/controller/AdminSyncControllerDocs.java` (6개)
- `src/main/java/uss/code/admin/controller/AdminAuthControllerDocs.java` (5개)
- `src/main/java/uss/code/course/controller/CourseControllerDocs.java` (4개)
- `src/main/java/uss/code/member/controller/MemberControllerDocs.java` (3개)
- `src/main/java/uss/code/admin/controller/AdminSemesterControllerDocs.java` (3개)

**컨벤션 문서**

- `.claude/spec/api-docs-convention.md` — "응답" 절의 `code`는 정수라는 규칙(36-39행)을 문자열 규칙으로 교체
- `.claude/rules/code-convention/common.md` — "예외 처리" 절에 코드 형식 규칙 한 줄 추가

**수정하지 않는 것**

- `.claude/spec/service-policy/*.md` — 6개 파일 모두 에러 코드와 메시지 문구를 인용하지 않는다(grep 확인). 정책 변경 없음
- `src/test/**` — 코드 값을 리터럴로 단언하는 테스트가 없다. `hasFieldOrPropertyWithValue("code", MISSING_ACCESS_TOKEN.getCode())` 형태라 타입이 바뀌어도 그대로 통과한다

## 구현 계획

### 1. `ExceptionCode` 코드와 메시지 교체

`private final int code;` → `private final String code;`

접두사는 도메인 패키지 단위 3-4자 축약, 순번은 그룹별 `001`부터. 관리자 인증, 표시 학기, 강의 동기화는 `admin` 패키지 하나이므로 `ADM`으로 통합한다.

| 상수 | 기존 | 신규 | 메시지 |
|---|---|---|---|
| `UNEXPECTED_SERVER_ERROR` | 9999 | `GLB-001` | 서버 내부 오류가 발생했어요. |
| `INVALID_ENUM_TYPE` | 8888 | `GLB-002` | 유효하지 않은 열거타입이에요. |
| `INVALID_REQUEST_PARAMETER` | 7777 | `GLB-003` | 유효하지 않은 입력 파라미터예요. |
| `MISSING_ACCESS_TOKEN` | 1000 | `AUTH-001` | 액세스 토큰이 누락됐어요. |
| `INVALID_ACCESS_TOKEN` | 1001 | `AUTH-002` | 액세스 토큰이 유효하지 않아요. |
| `INVALID_FORM_ACCESS_TOKEN` | 1002 | `AUTH-003` | 액세스 토큰 형식이 올바르지 않아요. |
| `INVALID_SIGNATURE_ACCESS_TOKEN` | 1003 | `AUTH-004` | 액세스 토큰 서명이 유효하지 않아요. |
| `EXPIRED_ACCESS_TOKEN` | 1004 | `AUTH-005` | 액세스 토큰이 만료됐어요. |
| `MEMBER_NOT_FOUND` | 1010 | `MEM-001` | 사용자를 찾을 수 없어요. |
| `PASSWORD_NOT_MATCH` | 1012 | `MEM-002` | 비밀번호가 일치하지 않아요. |
| `EMAIL_ALREADY_EXISTS` | 1015 | `MEM-003` | 이미 사용 중인 이메일이에요. |
| `COLLEGE_DEPARTMENT_MISMATCH` | 1016 | `MEM-004` | 학과의 소속 단과대학과 일치하지 않아요. |
| `INVALID_GENERAL_EDUCATION_AREA` | 2000 | `CRS-001` | 유효하지 않은 교양 영역이에요. |
| `INVALID_INTERDISCIPLINARY_DEPARTMENT` | 2001 | `CRS-002` | 유효하지 않은 연계전공과목이에요. |
| `COURSE_NOT_FOUND` | 2002 | `CRS-003` | 과목을 찾을 수 없어요. |
| `COURSE_CLOSED` | 2003 | `CRS-004` | 폐강된 과목이에요. |
| `COURSE_SCHEDULE_CONFLICT` | 3002 | `CRS-005` | 과목 시간표가 겹쳐요. |
| `COURSE_TYPE_LIMIT_EXCEEDED` | 3003 | `CRS-006` | 해당 과목 유형의 등록 제한을 초과했어요. |
| `CARTED_COURSE_NOT_FOUND` | 3000 | `CART-001` | 장바구니에 담은 과목을 찾을 수 없어요. |
| `CARTED_COURSE_LIMIT_EXCEEDED` | 3001 | `CART-002` | 장바구니는 최대 10개의 과목을 담을 수 있어요. |
| `COURSE_ALREADY_IN_CART` | 3004 | `CART-003` | 이미 장바구니에 담긴 과목이에요. |
| `CARTED_COURSE_DELETE_CONFLICT` | 3005 | `CART-004` | 장바구니 삭제를 반영할 수 없어요. 다시 확인해주세요. |
| `COURSE_MAX_CAPACITY_EXCEEDED` | 4000 | `REG-001` | 수강 정원이 마감됐어요. |
| `CREDIT_LIMIT_EXCEEDED` | 4001 | `REG-002` | 최대 이수 가능 학점을 초과했어요. |
| `COURSE_ALREADY_REGISTERED` | 4002 | `REG-003` | 이미 신청된 과목이에요. |
| `REGISTERED_COURSE_NOT_FOUND` | 4003 | `REG-004` | 수강신청한 과목을 찾을 수 없어요. |
| `REGISTRATION_CANCEL_CONFLICT` | 4004 | `REG-005` | 수강 취소를 반영할 수 없어요. 다시 확인해주세요. |
| `ADMIN_LOGIN_FAILED` | 5000 | `ADM-001` | 아이디나 비밀번호가 맞지 않아요. |
| `ADMIN_NOT_FOUND` | 5001 | `ADM-002` | 관리자를 찾을 수 없어요. |
| `ADMIN_ACCESS_DENIED` | 5002 | `ADM-003` | 관리자 권한이 없어요. |
| `SYSTEM_SEMESTER_NOT_FOUND` | 5100 | `ADM-004` | 표시 학기 설정을 찾을 수 없어요. |
| `SYNC_JOB_ALREADY_RUNNING` | 5200 | `ADM-005` | 이미 업데이트가 진행 중이에요. |
| `SYNC_STRATEGY_MISMATCH` | 5201 | `ADM-006` | 데이터가 변경됐어요. 다시 확인해주세요. |
| `SYNC_JOB_NOT_FOUND` | 5202 | `ADM-007` | 업데이트 작업을 찾을 수 없어요. |

`COURSE_SCHEDULE_CONFLICT`, `COURSE_TYPE_LIMIT_EXCEEDED`는 `CartService`와 `RegistrationService` 양쪽에서 던진다(`CartService:110,123`, `RegistrationService:138,151`). 장바구니 그룹에 두면 `POST /api/v1/registrations`가 `CART-` 코드를 내려주게 되므로 과목 그룹으로 옮긴다. 옮기면서 장바구니 그룹의 순번도 공백 없이 재부여한다.

선언 순서는 표 순서를 따르고, 기존 카테고리 주석(`// 전역`, `// 회원`)은 접두사를 드러내도록 고쳐 유지한다 (예: `// 회원 (MEM)`).

### 2. `ErrorResponse` 타입 변경

```java
public record ErrorResponse(
        String code,
        String message
) {
    public static ErrorResponse of(
            final String code,
            final String message
    ) {
        return new ErrorResponse(code, message);
    }
}
```

### 3. `JwtAuthenticationException` 타입 변경

`private final int code;` → `private final String code;`. 생성자 본문은 그대로(`exceptionCode.getCode()`). 하위 3개(`JwtTokenExpiredException`, `JwtTokenInvalidException`, `JwtTokenMissingException`)는 `super(exceptionCode)`만 호출하므로 수정 불필요.

### 4. `JwtExceptionFilter` 시그니처 변경

```java
private void setErrorResponse(
        final HttpServletResponse response,
        final int status,
        final String code,
        final String message
) throws IOException
```

호출부 2곳(`33행`, `36행`)은 그대로 컴파일된다.

### 5. `GlobalExceptionHandler` 하드코딩 제거

`makeErrorResponse(final String message)`가 `ErrorResponse.of(400, message)`로 HTTP 상태값을 코드 자리에 넣고 있다. `int` 시절에는 컴파일됐지만 `String`이 되면 깨진다. `MethodArgumentNotValidException` 경로이므로 `INVALID_REQUEST_PARAMETER`의 코드를 쓰고 필드 상세 메시지는 그대로 내린다.

```java
private ErrorResponse makeErrorResponse(final String message) {
    return ErrorResponse.of(
            INVALID_REQUEST_PARAMETER.getCode(),
            message
    );
}
```

`INVALID_REQUEST_PARAMETER`는 이미 static import되어 있다.

### 6. Swagger 예시 48개 교체

각 `@ExampleObject`의 `value`를 새 코드와 새 메시지로 바꾼다. `code`가 문자열이 되므로 따옴표를 붙인다.

```java
// 변경 전
value = "{\"code\" : 1010, \"message\" : \"사용자를 찾을 수 없습니다.\"}"
// 변경 후
value = "{\"code\" : \"MEM-001\", \"message\" : \"사용자를 찾을 수 없어요.\"}"
```

파일별 교체 대상은 위 매핑표를 기준으로 한다. 특히 `RegistrationControllerDocs`의 3002, 3003은 `CRS-005`, `CRS-006`이 된다(`CART-`가 아니다).

### 7. 컨벤션 문서 갱신

- `.claude/spec/api-docs-convention.md` 36-39행: `code`는 따옴표 없는 정수라는 규칙을 `{접두사}-{순번}` 문자열 규칙으로 교체하고 예시도 `{"code" : "MEM-001", "message" : "사용자를 찾을 수 없어요."}`로 바꾼다
- `.claude/rules/code-convention/common.md` "예외 처리" 절: 새 코드는 해당 도메인 접두사에 그룹 내 마지막 순번 +1을 붙이고, 메시지는 "-요"체로 쓴다는 규칙 추가

## 결정 필요 (Decisions needed)

- [x] `COURSE_SCHEDULE_CONFLICT`, `COURSE_TYPE_LIMIT_EXCEEDED`의 그룹 — **과목(`CRS-005`, `CRS-006`)으로 확정.** 두 서비스가 공유하는 과목 자체의 검증이고, 수강신청 응답에 `CART-`가 섞이는 것을 막는다
- [x] `MethodArgumentNotValidException` 응답 코드 — **`GLB-003`(`INVALID_REQUEST_PARAMETER`) 재사용으로 확정.** 별도 코드를 신설해도 프론트가 같은 처리를 하게 된다

## 검증

- `./gradlew build` — 타입 변경 4곳의 컴파일 통과 확인 (컴파일 에러가 곧 누락 탐지기다)
- `./gradlew test` — 전체 통과. 특히 코드 값을 참조하는 `JwtProviderTest`, `AuthServiceTest`, `AdminAuthServiceTest`
- `grep -rn 'code\\" : [0-9]' src/main/java` — 결과가 0건이어야 한다 (정수 예시 잔존 확인)
- Swagger UI(`/swagger-ui/index.html`)에서 임의 엔드포인트 하나의 에러 예시가 새 형식으로 보이는지 확인
- 확정된 매핑표를 프론트엔드에 공유

## Deviation Log
