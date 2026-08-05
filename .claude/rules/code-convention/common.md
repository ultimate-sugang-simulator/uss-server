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
- 메서드는 camelCase + CRUD 동사를 사용하라 (`findByDepartment`, `registerCourse`, `deleteRegisteredCourse`)
- API 경로는 kebab-case 복수형으로 작성하라 (`/api/v1/courses`, `/api/v1/registrations`)
- 연속된 대문자를 쓰지 마라 (`lastSemesterGPA` 대신 `lastSemesterGpa`)

### 필드명에 클래스명을 반복하지 마라

`Course.courseCode`는 `course.getCourseCode()`처럼 같은 말을 두 번 하게 만든다.
소속이 이미 타입으로 드러나므로 필드명에서 뺀다.

```java
// 지양
public class Course {
    private CourseGrade courseGrade;
    private CourseDepartment courseDepartment;
}

// 지향
public class Course {
    private CourseGrade grade;
    private CourseDepartment department;
}
```

응답 DTO도 같다. `MajorCourseResponse`가 표현하는 대상이 강의이므로 `courseGrade`가 아니라 `grade`다.

예외는 둘뿐이다.

1. **다른 엔티티에서 온 값은 출처를 밝힌다.** `CartedCourseResponse.cartCount`는 강의가 아니라 장바구니의 값이다
2. **같은 종류의 필드가 둘 이상이면 수식어를 남긴다.** `Course`는 과목코드(`courseCode`)와 학수번호(`haksuCode`)를
   함께 가지므로, 한쪽만 `code`로 줄이면 어느 쪽인지 알 수 없다

### enum 타입명은 도메인 접두사를 유지하라

필드명과 반대로, **타입명에서는 접두사를 뺄 수 없다.**
`CourseGrade`(강의 대상 학년)와 `MemberGrade`(회원 학년)처럼 도메인별로 같은 개념이 공존해서
접두사가 없으면 이름이 충돌한다.

```java
private CourseGrade grade;   // 타입은 CourseGrade, 필드는 grade
private MemberGrade grade;
```

## 주석

- 메인 코드에 설명 주석을 달지 마라. 설명이 필요하다고 느끼면 주석 대신 이름과 구조로 드러내라
- 유지하는 예외: `ExceptionCode`의 카테고리 그룹 주석(`// 회원`, `// 수강신청`)처럼 나열을 구획하는 용도의 주석
- 배경과 정책 설명이 필요하면 주석이 아니라 `.claude/spec/service-policy/`의 해당 도메인 파일에 남겨라
