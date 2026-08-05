# [PLAN-65] 강의 시간표 표시 형식을 도메인에서 분리

> 이슈: #65
> 브랜치: refactor/65-schedule-formatter

## 목표

응답 `schedule` 필드의 `[07-407:화(1-2A),목(2B-3)]` 형식을 조립하는 책임을 `Course`, `CourseSchedule` 엔티티에서 걷어내 `CourseScheduleFormatter`로 옮긴다. 이 형식은 도메인 규칙이 아니라 API 표시 형식이며, #62에서 강의실별 그룹핑에 시간표 컬렉션이 필요하다는 이유로 조립 책임까지 함께 엔티티로 올라온 것이다.

응답 형식과 DB 스키마는 바꾸지 않는다. 책임 위치만 옮기는 리팩토링이다.

## 영향 범위

### 신규 파일

- `src/main/java/uss/code/course/infra/CourseScheduleFormatter.java` — 시간표 목록을 응답 표시 문자열로 조립하는 유틸리티
- `src/test/java/uss/code/course/infra/CourseScheduleFormatterTest.java` — `CourseTest`에서 옮겨 온 조립 테스트

### 수정 파일

- `src/main/java/uss/code/course/domain/Course.java` — 표현 상수 5개(`NO_SCHEDULE`, `GROUP_PREFIX`, `GROUP_SUFFIX`, `CLASSROOM_DELIMITER`, `PERIOD_DELIMITER`)와 메서드 3개(`getFormattedCourseSchedules()`, `groupByClassroom()`, `formatClassroomSchedules()`) 제거. 이에 따라 `Comparator`, `LinkedHashMap`, `Map`, `Collectors` import도 제거
- `src/main/java/uss/code/course/domain/CourseSchedule.java` — 표현 상수 2개(`PERIOD_PREFIX`, `PERIOD_SUFFIX`)와 `getScheduleText()` 제거
- `src/main/java/uss/code/course/dto/response/SearchedCourseResponse.java` — `schedule` 조립 호출부 교체
- `src/main/java/uss/code/course/dto/response/MajorCourseResponse.java` — 동일
- `src/main/java/uss/code/course/dto/response/GeneralEducationCourseResponse.java` — 동일
- `src/main/java/uss/code/course/dto/response/InterdisciplinaryMajorCourseResponse.java` — 동일
- `src/main/java/uss/code/cart/dto/response/CartedCourseResponse.java` — 동일
- `src/main/java/uss/code/registration/dto/response/RegistrationCourseResponse.java` — 동일
- `src/test/java/uss/code/course/domain/CourseTest.java` — `시간표_문자열_조립_테스트` 중첩 클래스(6개) 제거, `수강_가능_판정_테스트`만 남김

### 손대지 않는 것

- `.claude/spec/service-policy/course.md` — 시간표 표시 형식 정책 자체는 그대로다. 정책 변경이 아니다
- Flyway 마이그레이션 — DB 변경 없음
- `CourseValidator` — 시간표 충돌 판정은 표시 형식을 쓰지 않으므로 무관
- `CourseScheduleFixture` — 필드 구성 변화 없음

## 구현 계획

### 1. infra: `CourseScheduleFormatter` 신설

`src/main/java/uss/code/course/infra/CourseScheduleFormatter.java`.
`CourseValidator`와 같은 스타일을 따른다 (`@UtilityClass` + 메서드에 `static` 명시).

```java
@UtilityClass
public class CourseScheduleFormatter {

    private static final String NO_SCHEDULE = "-";
    private static final String GROUP_PREFIX = "[";
    private static final String GROUP_SUFFIX = "]";
    private static final String CLASSROOM_DELIMITER = ":";
    private static final String PERIOD_DELIMITER = ",";
    private static final String PERIOD_PREFIX = "(";
    private static final String PERIOD_SUFFIX = ")";

    public static String format(final List<CourseSchedule> schedules)
    private static Map<String, List<CourseSchedule>> groupByClassroom(final List<CourseSchedule> schedules)
    private static String formatClassroomSchedules(final String classroom, final List<CourseSchedule> classroomSchedules)
    private static String formatPeriod(final CourseSchedule schedule)
}
```

- `format`: `schedules`가 비면 `NO_SCHEDULE` 반환. 아니면 `groupByClassroom()` 결과를 `formatClassroomSchedules()`로 매핑해 구분자 없이 이어 붙인다 (`Collectors.joining()`)
- `groupByClassroom`: 요일 순, 같은 요일이면 시작 시각 순으로 정렬한 뒤 `classroom` 기준 `LinkedHashMap`으로 그룹핑한다. 기존 `Course.groupByClassroom()`을 `schedules` 파라미터를 받도록만 바꿔 그대로 옮긴다
- `formatClassroomSchedules`: `GROUP_PREFIX + classroom + CLASSROOM_DELIMITER + 교시들 + GROUP_SUFFIX`. 교시들은 `formatPeriod()` 결과를 `PERIOD_DELIMITER`로 잇는다
- `formatPeriod`: `schedule.getDayOfWeek().getName() + PERIOD_PREFIX + schedule.getPeriodName() + PERIOD_SUFFIX`. 기존 `CourseSchedule.getScheduleText()` 본문을 그대로 옮긴 것이다

파라미터를 `Course`가 아니라 `List<CourseSchedule>`로 받는다. 포맷터가 필요한 것은 시간표 목록뿐이고, `Course` 전체를 받으면 불필요한 의존이 생긴다.

### 2. Entity: `Course`, `CourseSchedule` 정리

- `Course`: 상수 5개와 메서드 3개를 지운다. `schedules` 필드, `addCourseSchedule()`, `isRegisterable()`, `incrementEnrollment()`, `decrementEnrollment()`는 그대로 둔다. `@Getter`가 만드는 `getSchedules()`가 포맷터의 입력이 된다
- `CourseSchedule`: 상수 2개와 `getScheduleText()`를 지운다. `getScheduleText()` 호출부는 `Course.formatClassroomSchedules()` 한 곳뿐이므로(전수 확인 완료) 다른 파급이 없다

### 3. DTO: 호출부 6곳 교체

각 응답 DTO에서 아래로 바꾼다. import는 `CourseValidator` 호출 방식과 같이 일반 import + 클래스명 한정 호출을 쓴다 (static import 아님).

```java
// as-is
.schedule(course.getFormattedCourseSchedules())

// to-be
.schedule(CourseScheduleFormatter.format(course.getSchedules()))
```

대상: `SearchedCourseResponse`, `MajorCourseResponse`, `GeneralEducationCourseResponse`, `InterdisciplinaryMajorCourseResponse` (course), `CartedCourseResponse` (cart), `RegistrationCourseResponse` (registration).

지연 로딩 시점은 변하지 않는다. `getSchedules()` 접근이 DTO 조립 시점에 일어나는 것은 지금과 같고, `@BatchSize(1000)` 설정도 그대로다.

### 4. 테스트 이동

`CourseTest`의 `시간표_문자열_조립_테스트` 중첩 클래스 6개를 `CourseScheduleFormatterTest`로 옮긴다. `@IntegrationTest`를 유지한다 (레포의 모든 테스트가 이 어노테이션을 쓴다).
검증 대상만 `course.getFormattedCourseSchedules()` → `CourseScheduleFormatter.format(course.getSchedules())`로 바꾸고, given과 기대 문자열은 손대지 않는다.

| 옮길 테스트 | 기대값 |
|---|---|
| `시간표가_없으면_하이픈을_반환한다` | `-` |
| `강의실이_하나면_교시를_쉼표로_잇는다` | `[07-407:화(1-2A),목(2B-3)]` |
| `강의실이_여러_개면_강의실별로_묶는다` | `[05-506:월(5B-6)][05-507:수(5B-6)]` |
| `떨어져_있는_같은_강의실은_한_묶음으로_합친다` | `[15-113:월(1-2A),수(7-8A)][가상건물-200:화(4-5A)]` |
| `등록_순서와_무관하게_요일_순으로_정렬한다` | `[07-407:월(1),수(2),금(3)]` |
| `같은_요일이면_시작_시각_순으로_정렬한다` | `[08-201:월(5),월(6)]` |

`CourseTest`에는 `수강_가능_판정_테스트`만 남는다. `LocalTime`, `CourseDay`, `CourseScheduleFixture` import도 함께 정리한다.

## 결정 필요 (Decisions needed)

없음. 포맷터 위치(`course/infra/`), 시그니처(`List<CourseSchedule>` 수신), 테스트 어노테이션(`@IntegrationTest` 유지)은 기존 코드베이스 선례로 결정된다.

## 검증

- **신규**: `CourseScheduleFormatterTest` — 위 표의 6개 시나리오. 옮기기 전과 기대 문자열이 동일해야 한다
- **회귀**: `CourseResponseTest.시간표_문자열_매핑_테스트` — 호출부 교체 후에도 응답 `schedule`이 `[07-407:월(1-2A)]`, 시간표가 없으면 `-`로 나오는지 확인한다. 이 테스트가 DTO 6종 중 대표 경로를 덮는다
- **회귀**: `CourseTest.수강_가능_판정_테스트`, `CourseServiceTest`, `CartServiceTest`, `RegistrationServiceTest` — 엔티티에서 메서드를 지운 여파가 없는지 확인
- **전체**: `./gradlew test`

## Deviation Log
