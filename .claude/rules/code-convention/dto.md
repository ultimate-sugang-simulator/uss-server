---
description: DTO(Request/Response) 클래스 작성 패턴
paths:
  - "src/main/java/**/dto/**/*.java"
---

# DTO Convention

- DTO는 `record` 타입으로 선언하라
- 패키지는 요청/응답으로 분리하라: `{domain}/dto/request/`, `{domain}/dto/response/`
  - 여러 조회에서 공유하거나 Repository projection으로 받는 record는 `{domain}/dto/common/`에 둔다 (예: `cart/dto/common/CartCount`)
- record 컴포넌트(필드) 사이는 빈 줄로 구분하라
- 컴포넌트명에 그 DTO가 표현하는 대상의 이름을 반복하지 마라
  (`MajorCourseResponse.courseGrade` 대신 `grade`. 자세한 규칙과 예외는 `common.md`)
- **같은 값은 모든 DTO에서 같은 이름을 써라.** 한쪽은 `department`, 다른 쪽은 `courseDepartment`처럼 갈리면
  클라이언트가 응답마다 다르게 파싱해야 한다

## 목록 응답

- 목록을 감싸는 DTO의 컴포넌트명은 `{단수형}Responses`로 통일하라
  (`majorCourseResponses`, `searchedCourseResponses`)

## Request

- `@Schema` + validation 어노테이션(`@NotNull` 등)을 포함하라

## Response

- validation 어노테이션을 붙이지 마라. validation은 Request 전용이다
- 객체 생성(정적 팩토리 + private `@Builder`)은 `common.md`를 따른다

### @Schema 포맷

- `@Schema` 속성이 2개 이상이면 한 줄에 몰아쓰지 말고 속성당 한 줄로 작성하라

```java
@Schema(
        description = "팔로워 목록",
        requiredMode = Schema.RequiredMode.REQUIRED
)
public List<FollowerResponse> contents;
```

- 속성이 1개면 한 줄로 작성해도 된다 (예: `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)`)
