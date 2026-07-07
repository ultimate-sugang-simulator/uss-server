---
description: Java 소스 코드를 작성하거나 수정할 때 공통으로 적용되는 컨벤션
paths:
  - "src/main/java/**/*.java"
---

# Common Code Convention

## 레이어 구조

Controller → Service → Repository 순서를 따른다 (Facade 레이어 없음).
역방향 의존을 만들지 마라: Repository가 Service를 참조하면 안 된다.

## 예외 처리

- `throw new RestApiException(ExceptionCode.XXX)` 패턴을 사용하라
- 새 에러코드는 `ExceptionCode` enum에 추가하되, 카테고리별 주석 그룹을 유지하라
- `ExceptionCode` 상수는 static import로 식별자만 노출하라 (`ExceptionCode.XXX` 표기 대신 `XXX`). 단 `ExceptionCode` 타입 자체를 참조할 때(파라미터 타입 등)는 일반 import를 사용한다 (예: `GlobalExceptionHandler`)

## 객체 생성

- 도메인 객체(Entity)와 Response DTO는 정적 팩토리 메서드(`create()` / `of()` / `from()`)로만 생성하라
- 생성자는 노출하지 말고 private + `@Builder(access = AccessLevel.PRIVATE)`로 감춰라

## 상수

- 매직넘버·매직스트링을 코드에 직접 쓰지 말고 `private static final` 상수로 클래스 상단에 선언하라

## 포맷팅

- 메서드 파라미터가 2개 이상이면 각 파라미터를 줄바꿈하여 작성하라
```java
public ReturnType methodName(
        final String param1,
        final String param2
) {
}
```

- 의존성 주입 필드가 여러 개면 도메인·성격별로 빈 줄로 그룹핑하라

## 메서드 본문 구성

메서드 본문에서 논리 단계나 처리 대상 도메인이 바뀌면 빈 줄로 구분해 맥락을 드러내라.

```java
// 한 도메인/단계 처리
final Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

// 다음 단계 처리
final List<Course> courses = courseRepository.findByCourseDepartment(...);

// 응답 조립
return MajorCoursesResponse.of(...);
```

## 네이밍

- 패키지는 도메인 단위로 나눠라 (`course`, `member`, `registration`, `cart`)
- 클래스는 PascalCase로 작성하라 (`CourseService`, `RegistrationController`)
- 메서드는 camelCase + CRUD 동사를 사용하라 (`findByCourseDepartment`, `registerCourse`, `deleteRegisteredCourse`)
- API 경로는 kebab-case 복수형으로 작성하라 (`/api/v1/courses`, `/api/v1/registrations`)
