---
description: 테스트 코드 작성 규칙 (모든 테스트는 통합 테스트)
---

# Test Convention

모든 서비스 테스트는 통합 테스트(`@IntegrationTest`)로 작성한다. Mockito 기반 단위 테스트는 쓰지 않는다.

## 네이밍 & 설정

- 파일명: `{Class}Test.java` (예: `CourseServiceTest`)
- 클래스 어노테이션: `@IntegrationTest` (커스텀 어노테이션 = `@SpringBootTest` + `@Transactional`, H2 사용)
  - 각 테스트는 `@Transactional` 롤백으로 격리된다. 별도 truncate 스크립트를 쓰지 않는다
- 의존성 주입: `@Autowired` (필드 주입)
- 검증 라이브러리는 AssertJ (`assertThat`, `assertThatThrownBy`)
- 테스트 패키지는 `src/main/java`의 도메인 구조를 미러링하라 (`src/test/java/uss/code/{domain}/...`)

## 메서드 작성

- 메서드명은 한글 서술형 (`컴퓨터공학부_학생이_전공과목을_조회하면_성공한다()`)
- `@Nested` 클래스로 시나리오를 그룹화하라 (클래스명도 한글 서술형)
- 본문은 `//given` / `//when` / `//then` 주석으로 구간을 구분하라

## 예외 검증

`RestApiException`을 던지는 예외 케이스는 반드시 `exceptionCode`까지 검증하라. 타입만 검증하면 다른 코드로 회귀해도 통과해 회귀를 못 잡는다.

- 타입 + 코드: `.isInstanceOf(RestApiException.class).hasFieldOrPropertyWithValue("exceptionCode", {CODE})`
- `ExceptionCode`는 static import로만 사용하라 (`ExceptionCode.X` 표기 금지)

```java
import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

assertThatThrownBy(() -> courseService.getMajorCourses(invalidMemberId))
        .isInstanceOf(RestApiException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
```

## Fixture

| 항목 | 규칙 |
|---|---|
| 위치 | `src/test/java/uss/code/{domain}/fixture/` |
| 클래스명 | `{Domain}Fixture` (예: `CourseFixture`, `MemberFixture`) |
| 생성 방식 | `new {Entity}()` 후 `ReflectionTestUtils.setField(entity, "필드명", 값)`로 필드 채움 |
| 오버로드 | 기본값 세트 + 세부 필드를 받는 팩토리 메서드를 함께 제공 (`createCourse()`, `createCourseWithDetails(...)`) |
