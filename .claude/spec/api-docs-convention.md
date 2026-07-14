---
description: Swagger API 문서(ControllerDocs 인터페이스) 작성 규칙
---

# API Docs Convention

## 파일 구조

- 각 Controller마다 `controller/{Controller}Docs.java` 인터페이스를 생성하라 (Controller와 같은 `controller/` 패키지)
- Controller는 이 인터페이스를 `implements` 하라
- Swagger 어노테이션은 Docs 인터페이스에만 선언하라. Controller에는 붙이지 마라

## 어노테이션 규칙

### 인터페이스 레벨

| 어노테이션 | 규칙 | 예시 |
|---|---|---|
| `@Tag` | name: `"{Domain} API"`, description: 한글 요약 | `@Tag(name = "Course API", description = "과목 조회 관련 API")` |

### 메서드 레벨

| 어노테이션 | 규칙 | 예시 |
|---|---|---|
| `@Operation` summary | 기능을 한 줄로 요약 | `"전공 과목 조회"` |
| `@Operation` description | JWT 필요 시 `"🔐 <strong>Jwt 필요</strong><br>"` 포함 | — |
| `@ParamValidation` / `@RequestParam` | Controller 시그니처와 동일하게 선언 | — |

### 응답 (`@ApiResponses`)

| 구분 | description 형식 | 비고 |
|---|---|---|
| 성공 | `"✅ {성공 메시지}"` | — |
| 실패 | `"🚨 {에러 설명}"` | `schema = @Schema(implementation = ErrorResponse.class)` |

- `@ExampleObject`의 value는 `ExceptionCode` enum의 실제 code/message를 그대로 사용하라 (형식: `{"error" : "MEMBER_1010", "message" : "사용자를 찾을 수 없습니다."}`)
- `ErrorResponse`는 `uss.code.global.exception.dto.response.ErrorResponse`를 사용하라

### 파라미터

- 인증 파라미터 `@Auth final long memberId`는 Docs 인터페이스에도 동일하게 선언하라

## DTO @Schema

- Request/Response record의 각 필드에 `@Schema`를 붙여라
- 속성이 2개 이상이면 줄바꿈:
```java
@Schema(
        description = "과목 아이디",
        example = "1"
)
```
