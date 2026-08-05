# [PLAN-62] 학교 연계 API 기준 강의 도메인 재정의

> 이슈: #62
> 브랜치: refactor/62-real-course-domain

## 목표

학교가 강좌정보, 시간표 연계 API로 실제 강의 데이터를 제공하게 되면서, 임의로 구성해온 강의 도메인을
실데이터가 그대로 들어올 수 있는 형태로 바꾼다. 학년도와 학기 축을 도입해 학수번호 단독 UNIQUE를 걷어내고,
실데이터에 없는 교수명을 제거하며, 강의 단위에 묶여 있던 강의실을 시간표 단위로 내린다.

## 선행 조건

1단계(실데이터 조사)는 **완료됐다.** 산출물은 `.claude/resources/course-api-code-mapping.md`.
원본 응답은 `.local/inu-api/`에 있다 (gitignore 대상).

조사로 확인된 것과 계획이 바뀐 지점은 아래 "조사 반영"에 정리했다.

## 조사 반영

### enum은 거의 손댈 게 없다

`CourseCollege`(19), `CourseClassification`(9), `CourseArea`(19), `CourseType`(20), `CourseGrade`(5)는
실데이터와 **완전히 일치**한다. `CourseDay`는 일요일만 미사용이고 나머지 6개가 일치한다.
`CourseDepartment`만 87개 중 2개 추가, 2개 정리가 필요하다.

- 추가: `Global Trade & Service학부`(`0000913`), `지능형로봇시스템연계전공`(`0000912`)
  - 후자는 연계전공이므로 `isInterdisciplinary()`에도 넣는다
- 이번 학기 개설 없음: `무역학부`, `국제개발협력연계전공`
  - `무역학부`는 `Global Trade & Service학부`로 개편된 것으로 보인다. 상수는 남겨둔다

### 페이지 반복 수집은 필요 없다

`totalpageSize`가 응답에 아예 없고(`null`), 전체가 1페이지에 온다. `PAGE=2`는 JSON이 아니라
"site move" HTML 리다이렉트를 반환한다. 명세서의 페이지네이션 설명은 이 두 API에는 해당하지 않는다.
5단계와 후속 배치 이슈에서 이 전제를 뺀다.

### 적재 시 학기를 하나로 필터링해야 한다

받은 데이터에 2학기(2,396건)와 **여름계절학기(142건)가 함께** 들어 있다.
그대로 적재하면 학수번호가 68건 중복된다. 단일 학기 안에서는 중복이 0이므로
`TERM_CODE=20`만 적재한다.

운영 방식은 백오피스에서 학기가 바뀔 때마다 전체를 갈아끼우는 구조로 간다.
DB에는 항상 한 학기만 존재하므로 조회에 학기 조건을 붙일 필요가 없다 (결정 1 유지).

### 조인되지 않는 시간표 1,286행을 버린다

시간표 9,490행 중 1,286행(13.6%)이 강좌정보에 없는 학수번호를 참조한다.
강좌정보의 학수번호는 전부 10자리인데 이쪽에는 9자리가 섞여 있어, 학부가 아닌 다른 과정으로 보인다.
과목 정보가 없으면 화면에 띄울 수 없으므로 적재 대상에서 제외한다.

### 강의실을 시간표로 내리는 결정이 데이터로 확인됐다

**220개 강의(8.7%)가 두 개 이상의 강의실을 쓴다.** `ROOM_NAME` 빈값은 0건이므로
`CourseSchedule.classroom`은 `NOT NULL`로 잡는다.

### 교시는 현재 모델과 맞는다

`LECTM`은 31개 코드가 3계열(50분 단일 / 75분 블록 / 야간 B타임)로 나뉘고,
각 행이 `LECTM_START`, `LECTM_END`를 가진 연속 구간이다. `startTime`, `endTime`에 그대로 들어간다.

## 필드명 정리 (범위 확대)

`.claude/rules/code-convention/`에 "필드명에 클래스명을 반복하지 마라" 규칙을 세우고,
이번 이슈에서 손대는 도메인(`course`, `member`)과 그 응답 DTO에 일괄 적용한다.

근거: 접두사를 쓰는 쪽이 이미 소수파다. `Cart`, `Registration`, `ChallengeCode` 엔티티와
`MemberProfileResponse`는 접두사가 없고, `Course`, `Member` 엔티티와 Course 응답 DTO만 쓴다.
게다가 같은 값이 DTO마다 이름이 갈린다 - `MajorCourseResponse`는 `department`인데
`SearchedCourseResponse`, `CartedCourseResponse`, `RegistrationCourseResponse`는 `courseDepartment`다.

### `Course`

| 현재 | 변경 |
|---|---|
| `courseSchedules` | `schedules` |
| `courseCollege` | `college` |
| `courseDepartment` | `department` |
| `courseClassification` | `classification` |
| `courseArea` | `area` |
| `courseType` | `type` |
| `courseGrade` | `grade` |
| `courseTerm` (신규) | `term` |
| `courseCode` | **유지** (아래 설명) |
| `titleKr`, `titleEn`, `credits`, `isEnglishCourse`, `maxCapacity`, `currentEnrollment`, `academicYear`, `haksuCode` | 유지 |

> `courseCode`(과목코드)와 `haksuCode`(학수번호)가 함께 있으므로 `courseCode`를 `code`로 줄이지 않는다.
> `course.getCode()`만으로는 둘 중 무엇인지 알 수 없다. 컨벤션의 예외 2번에 해당한다.

### `CourseSchedule`

| 현재 | 변경 |
|---|---|
| `courseDay` | `day` |
| `scheduleText` | `periodName`으로 대체 (앞서 결정) |
| `course`, `startTime`, `endTime`, `classroom` | 유지 |

### `Member`

| 현재 | 변경 |
|---|---|
| `memberCollege` | `college` |
| `memberDepartment` | `department` |
| `memberGrade` | `grade` |
| `lastSemesterGPA` | `lastSemesterGpa` (연속 대문자 제거) |
| 나머지 | 유지 |

### 응답 DTO 6종

`MajorCourseResponse`, `GeneralEducationCourseResponse`, `InterdisciplinaryMajorCourseResponse`,
`SearchedCourseResponse`, `CartedCourseResponse`, `RegistrationCourseResponse`

| 현재 | 변경 |
|---|---|
| `courseGrade` | `grade` |
| `courseClassification` | `classification` |
| `courseArea` | `area` |
| `courseTitleKr` | `titleKr` |
| `courseTitleEn` | `titleEn` |
| `courseDepartment` / `department` | `department` (통일) |
| `courseCode` | 유지 |
| `professor`, `classroom` | 제거 (앞서 결정) |
| `cartCount` | 유지 (장바구니에서 온 값이므로 출처를 밝힌다) |
| `MajorCoursesResponse.majorCourses` | `majorCourseResponses` (다른 5종과 통일) |

### 이에 따른 추가 수정

- `CourseRepository`의 JPQL 3곳 - `c.courseDepartment` → `c.department`, `c.courseArea` → `c.area`, `c.courseGrade` → `c.grade`, `c.courseSchedules` → `c.schedules`
- `CourseRepository`의 메서드명 - `findByCourseDepartment` → `findByDepartment`, `findByCourseArea` → `findByArea`
- `CourseValidator` - `getCourseType()`, `getCourseDay()`, `getCourseSchedules()` 호출부
- `CourseService`, `CartService`, `RegistrationService`, `MemberService` - getter 호출부
- `CourseFixture`, `MemberFixture`, `CourseScheduleFixture` - `ReflectionTestUtils.setField`의 필드명 문자열
- DB 컬럼명 - `course_college` → `college` 등. 4단계 마이그레이션에 `RENAME COLUMN` 추가

> `CartRepository`, `RegistrationRepository`는 `c.course.id`만 참조하므로 영향이 없다.
> 테스트 178개 호출부도 팩토리 메서드를 거치지 필드명 문자열을 직접 쓰지 않아 영향이 없다.

> **프론트 영향**: 응답 필드명이 바뀐다. 교수명과 강의실 제거로 이미 깨지는 변경이므로 한 번에 간다.
> 배포 전에 프론트에 공지가 필요하다.

## 네이밍 원칙

API 필드명이 더 명확한 것만 따오고, 나머지는 현재 이름을 유지한다.
API 쪽은 대부분 한글 발음 축약(`ISU`, `SUUP`, `HY`, `LECTM`)이라 채택할 만한 게 많지 않다.

| 대상 | 결정 | 이유 |
|---|---|---|
| `HAKSU_CODE` | **`haksuCode`로 채택** | 학수번호는 학교 시스템 고유명사 |
| `COURSE_CODE` | **`courseCode`로 채택** | API와 같은 이름, 같은 의미(과목코드) |
| `LECTM_NAME` | **`periodName`** (제3안) | 값이 `1-2A`, `야1-2A` 같은 교시 표기다. `scheduleText`는 내용이 안 드러나고 `lectm`은 축약이다 |
| `YEAR` | **`academicYear`** (제3안) | MySQL의 `YEAR`는 데이터 타입이자 함수명이고 H2도 `YEAR()`를 갖는다. 컬럼명 충돌을 피한다 |
| `TERM_CODE` | `courseTerm` | 다른 필드의 `course` 접두사와 일관성 |
| 그 외 전부 | 현재 이름 유지 | `titleKr`, `courseClassification`, `courseArea`, `courseType`, `courseDepartment`, `courseCollege`, `courseGrade`, `credits`, `isEnglishCourse`, `startTime`, `endTime`, `classroom` |

> `courseCode`는 이름은 그대로지만 **의미가 바뀐다.** 지금까지 학수번호가 들어 있었고 앞으로는 과목코드가 들어간다.
> 과목코드는 분반을 공유하므로(1,499개 과목코드 → 2,470개 학수번호) 유일하지 않다.
> 기존 코드에서 `courseCode`를 식별자로 쓰는 곳이 없는지 확인해야 한다.

## 영향 범위

### 신규 파일

- `src/main/resources/database/migration/V0_8__redefine_course_for_real_data.sql` - 학년도, 학기 컬럼 추가와 UNIQUE 전환, 교수명 제거, 강의실 이동
- `src/main/resources/database/seed/V0_9__insert_course.sql` (분할 시 V0_9~V0_11) - 실데이터 기준 강의 시드
- `src/main/resources/database/seed/V0_12__insert_course_schedule.sql` (분할 시 V0_12~V0_14) - 실데이터 기준 시간표 시드
- `src/main/java/uss/code/course/domain/CourseTerm.java` - 학기 구분 enum (10, 20, 30, 40)
- `.claude/resources/course-api-code-mapping.md` - 1단계 산출물. API 코드값과 enum의 매핑표

### 수정 파일

도메인

- `src/main/java/uss/code/course/domain/Course.java` - `academicYear`, `term`, `haksuCode` 추가, `professorName`과 `classroom` 제거, 필드 7개 접두사 제거
- `src/main/java/uss/code/course/domain/CourseSchedule.java` - `classroom`, `periodName` 추가, `scheduleText` 제거, `courseDay` → `day`, `getScheduleText()`가 요일, 교시, 강의실을 조립하도록 변경
- `src/main/java/uss/code/member/domain/Member.java` - `memberCollege`, `memberDepartment`, `memberGrade` 접두사 제거, `lastSemesterGPA` → `lastSemesterGpa`
- `src/main/java/uss/code/course/repository/CourseRepository.java` - `findByKeyword`의 `MATCH` 컬럼 목록에 `haksu_code` 추가, JPQL 필드 참조와 메서드명 변경
- `src/main/java/uss/code/course/infra/CourseValidator.java` - getter 호출부
- `src/main/java/uss/code/course/domain/CourseCollege.java` - `code` 필드와 `fromCode` 추가
- `src/main/java/uss/code/course/domain/CourseDepartment.java` - `code` 필드와 `fromCode` 추가, 상수 2개 추가
- `src/main/java/uss/code/course/domain/CourseClassification.java` - `code` 필드와 `fromCode` 추가
- `src/main/java/uss/code/course/domain/CourseArea.java` - `code` 필드와 `fromCode` 추가
- `src/main/java/uss/code/course/domain/CourseType.java` - `code` 필드와 `fromCode` 추가
- `src/main/java/uss/code/course/domain/CourseGrade.java` - `code` 필드와 `fromCode` 추가
- `src/main/java/uss/code/course/domain/CourseDay.java` - `code` 필드와 `fromCode` 추가

서비스

- `src/main/java/uss/code/course/service/CourseService.java` - getter, 리포지토리 메서드명 변경
- `src/main/java/uss/code/cart/service/CartService.java` - getter 호출부
- `src/main/java/uss/code/registration/service/RegistrationService.java` - getter 호출부
- `src/main/java/uss/code/member/service/MemberService.java` - getter 호출부

DTO (교수명, 강의실 제거 + 컴포넌트명 접두사 제거)

- `src/main/java/uss/code/course/dto/response/MajorCourseResponse.java`
- `src/main/java/uss/code/course/dto/response/MajorCoursesResponse.java` - `majorCourses` → `majorCourseResponses`
- `src/main/java/uss/code/course/dto/response/GeneralEducationCourseResponse.java`
- `src/main/java/uss/code/course/dto/response/InterdisciplinaryMajorCourseResponse.java`
- `src/main/java/uss/code/course/dto/response/SearchedCourseResponse.java`
- `src/main/java/uss/code/cart/dto/response/CartedCourseResponse.java`
- `src/main/java/uss/code/registration/dto/response/RegistrationCourseResponse.java`

테스트

- `src/test/java/uss/code/course/fixture/CourseFixture.java` - 팩토리 시그니처와 `setField` 필드명
- `src/test/java/uss/code/course/fixture/CourseScheduleFixture.java` - `classroom`, `periodName` 추가, `setField` 필드명
- `src/test/java/uss/code/member/fixture/MemberFixture.java` - `setField` 필드명
- `src/test/java/uss/code/course/service/CourseServiceTest.java` - 픽스처 호출 34곳
- `src/test/java/uss/code/cart/service/CartServiceTest.java` - 픽스처 호출 48곳
- `src/test/java/uss/code/registration/service/RegistrationServiceTest.java` - 픽스처 호출 96곳
- `src/test/java/uss/code/member/service/MemberServiceTest.java` - getter 검증부

문서

- `.claude/spec/service-policy/course.md` - 강의 식별, 정원, 강의실 표기 정책 갱신
- `.claude/rules/code-convention/common.md` - 필드명 접두사 규칙, enum 타입명 규칙 신설 (완료)
- `.claude/rules/code-convention/domain.md` - 컬럼명과 필드명 일치, DB 예약어 회피 (완료)
- `.claude/rules/code-convention/dto.md` - 컴포넌트명 규칙, 목록 응답 통일 (완료)

## 구현 계획

### 1. 실데이터 조사 — 완료

산출물: `.claude/resources/course-api-code-mapping.md`. 결과 요약은 위 "조사 반영"을 본다.

### 2. Entity

**`Course`** - 필드를 아래와 같이 바꾼다.

추가

```java
@Column(nullable = false, name = "academic_year")
private int academicYear;

@Enumerated(EnumType.STRING)
@Column(nullable = false, name = "course_term")
private CourseTerm courseTerm;

@Column(nullable = false, name = "haksu_code")
private String haksuCode;
```

변경

- `courseCode`의 `unique = true`를 뗀다. 유일성은 `(academic_year, course_term, haksu_code)` 복합 제약이 보장한다.
  기존 `courseCode`에는 학수번호가 들어 있었으므로, 실데이터의 `COURSE_CODE`(과목코드)를 `courseCode`에,
  `HAKSU_CODE`(학수번호)를 `haksuCode`에 넣는다.
  과목코드는 분반이 공유하므로(1,499개 과목코드 → 2,470개 학수번호) 유일하지 않다

제거

- `private String professorName` 과 getter 사용처
- `private String classroom`

`getFormattedCourseSchedules()`, `isRegisterable()`, `incrementEnrollment()`, `decrementEnrollment()`,
`addCourseSchedule()`은 그대로 둔다.

**`CourseSchedule`** - 강의실과 교시명을 받고 `scheduleText`를 걷어낸다.

추가

```java
@Column(nullable = false, name = "classroom")
private String classroom;

@Column(nullable = false, name = "period_name")
private String periodName;
```

제거

- `private String scheduleText` - `periodName`과 `classroom` 조합으로 대체한다 (6단계)

`classroom`을 `NOT NULL`로 잡는 근거: 실데이터 9,490행 중 `ROOM_NAME` 빈값이 0건이다.
온라인 강좌도 `가상건물-200 온라인강의실` 같은 값을 받는다.

`periodName`에는 `LECTM_NAME`이 들어간다. 값은 `1-2A`, `야1-2A`, `5B-6` 형태의 교시 표기다.

**`CourseTerm`** (신규)

```java
@Getter
@RequiredArgsConstructor
public enum CourseTerm {
    FIRST("10", "1학기"),
    SECOND("20", "2학기"),
    SUMMER("30", "여름계절학기"),
    WINTER("40", "겨울계절학기");

    private final String code;
    private final String name;

    public static CourseTerm fromCode(final String code) { ... }
}
```

### 3. enum에 코드값 부여

7개 enum 모두 같은 형태로 바꾼다. 코드값은 `course-api-code-mapping.md`에 전부 정리돼 있다.

```java
COMPUTER_ENGINEERING("0000077", CourseCollege.INFORMATION_TECHNOLOGY, "컴퓨터공학부"),

private final String code;
private final CourseCollege courseCollege;
private final String name;

public static CourseDepartment fromCode(final String code) {
    return Arrays.stream(values())
            .filter(department -> department.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
}
```

`CourseDepartment`에만 상수 2개를 추가한다.

```java
GLOBAL_TRADE_SERVICE("0000913", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "Global Trade & Service학부"),
INTELLIGENT_ROBOT_SYSTEM("0000912", CourseCollege.ETC, "지능형로봇시스템연계전공"),
```

후자는 연계전공이므로 `isInterdisciplinary()`의 목록에도 넣는다.
`무역학부`, `국제개발협력연계전공`은 이번 학기 개설이 없어 코드값이 없다. 상수는 남기되 `code`를 빈 문자열로 둔다.

기존 `from(String)`, `from(MemberDepartment)`, `fromInterdisciplinary(String)`은 **그대로 둔다.**
조회 API가 `?department=COMPUTER_ENGINEERING` 형태로 enum 이름을 받고 있어 제거하면 API가 깨진다.
`fromCode`는 시드 생성 단계에서만 쓴다.

> **반드시 코드로 매칭한다. 이름으로 매칭하면 두 군데가 깨진다.**
> `CourseArea.BASIC_SCIENCE_ENGINEERING`의 API 이름은 `기초과학ㆍ공학`(U+318D)인데
> 현재 enum은 `기초과학·공학`(U+00B7)으로 문자가 다르다.
> `CourseType.SOCIAL_SERVICE_1`의 API 이름은 `담장너머~,사회봉사(1)`로 현재 enum(`사회봉사(1)`)과 다르고 콤마까지 들어 있다.

### 4. Flyway - 스키마

`src/main/resources/database/migration/V0_8__redefine_course_for_real_data.sql`

버전은 V0_8이다. `application-prod.yml`이 `database/migration`과 `database/seed`를 함께 읽으므로
두 디렉토리가 버전 번호를 공유한다. 현재 최대는 `V0_7__remove_email_verification.sql`이다.

```sql
-- 기존 강의 데이터를 비운다. 학년도와 학기가 NOT NULL이 되므로 기본값을 채울 근거가 없다.
-- registrations, carts는 ON DELETE CASCADE로 함께 지워진다.
DELETE FROM courses;

-- 학년도, 학기 축 도입
ALTER TABLE courses ADD COLUMN academic_year INT NOT NULL;
ALTER TABLE courses ADD COLUMN course_term VARCHAR(50) NOT NULL;
ALTER TABLE courses ADD COLUMN haksu_code VARCHAR(15) NOT NULL;

-- 학수번호 단독 UNIQUE를 학년도, 학기, 학수번호 복합 UNIQUE로 교체
ALTER TABLE courses DROP INDEX course_code;
ALTER TABLE courses ADD UNIQUE KEY uk_year_term_haksu (academic_year, course_term, haksu_code);

-- 연계 API가 제공하지 않는 교수명 제거
ALTER TABLE courses DROP COLUMN professor_name;

-- 강의실을 강의 단위에서 시간표 단위로 이동
ALTER TABLE courses DROP COLUMN classroom;
ALTER TABLE course_schedules ADD COLUMN classroom VARCHAR(255) NOT NULL;
ALTER TABLE course_schedules ADD COLUMN period_name VARCHAR(255) NOT NULL;

-- schedule_text는 period_name과 classroom 조합으로 대체
ALTER TABLE course_schedules DROP COLUMN schedule_text;

-- course_code가 과목코드로 바뀌므로 학수번호를 검색 대상에 추가
ALTER TABLE courses DROP INDEX ft_idx_course_search;
ALTER TABLE courses ADD FULLTEXT INDEX ft_idx_course_search (course_code, haksu_code, title_kr, title_en) WITH PARSER ngram;

-- 필드명 접두사 제거에 맞춰 컬럼명 정리
ALTER TABLE courses RENAME COLUMN course_college TO college;
ALTER TABLE courses RENAME COLUMN course_department TO department;
ALTER TABLE courses RENAME COLUMN course_classification TO classification;
ALTER TABLE courses RENAME COLUMN course_area TO area;
ALTER TABLE courses RENAME COLUMN course_type TO type;
ALTER TABLE courses RENAME COLUMN course_grade TO grade;
ALTER TABLE course_schedules RENAME COLUMN course_day TO day;

ALTER TABLE members RENAME COLUMN member_college TO college;
ALTER TABLE members RENAME COLUMN member_department TO department;
ALTER TABLE members RENAME COLUMN member_grade TO grade;

-- 인덱스명도 컬럼명에 맞춘다
ALTER TABLE courses RENAME INDEX idx_course_department TO idx_department;
ALTER TABLE courses RENAME INDEX idx_course_area TO idx_area;
```

> `RENAME COLUMN`은 MySQL 8.0부터 지원한다. 8.0 미만이면 `CHANGE COLUMN {옛이름} {새이름} {타입} NOT NULL`로 바꿔야 한다.
>
> `day`와 `type`은 MySQL 예약어가 아니지만 흔한 키워드다. `course_schedules.day`, `courses.type`으로
> 테이블명과 함께 쓰면 문제없고, 네이티브 쿼리에서만 주의한다.
> `courses`에는 네이티브 쿼리가 `findByKeyword` 하나뿐이고 두 컬럼을 참조하지 않는다.

`CourseRepository.findByKeyword`의 네이티브 쿼리에도 `MATCH(...)` 컬럼 목록을 두 군데(SELECT 조건과 ORDER BY) 모두
`c.course_code, c.haksu_code, c.title_kr, c.title_en`으로 맞춰야 한다. 컬럼 목록이 인덱스와 정확히 일치하지 않으면
MySQL이 FULLTEXT 인덱스를 쓰지 못하고 에러를 낸다.

### 5. Flyway - 시드

`.local/inu-api/`의 JSON을 INSERT 문으로 변환한다. 변환은 일회성 스크립트로 하고 스크립트는 커밋하지 않는다.

**적재 대상을 두 번 거른다.**

1. `TERM_CODE == "20"`(2학기)만 남긴다. 여름계절학기 142건을 넣으면 학수번호가 68건 중복된다
2. 시간표는 강좌정보에 있는 `(YEAR, TERM_CODE, HAKSU_CODE)`에 매칭되는 행만 남긴다.
   전체 9,490행 중 1,286행이 매칭되지 않는다

거른 결과는 강의 2,396건, 시간표 약 6,300행이다.

- `V0_9__insert_course.sql` 이후 - `courses` 적재. 기존 시드가 1000행 단위로 나뉘어 있으므로 같은 크기로 분할한다
- 강의 시드 다음 버전부터 - `course_schedules` 적재. 기존과 같이
  `(SELECT id FROM courses WHERE ...)` 서브쿼리로 FK를 잡되, 이제 학수번호만으로는 유일하지 않으므로
  `WHERE academic_year = ? AND course_term = ? AND haksu_code = ?` 3개 조건을 모두 건다
- `max_capacity`는 API가 주지 않는다. 전체를 `100`으로 넣고 이벤트 때 수동으로 조정한다
- `current_enrollment`는 0으로 넣는다

변환 스크립트는 코드값을 enum 상수명으로 바꾸는 과정에서 매핑되지 않는 값을 만나면
해당 코드값과 이름을 목록으로 출력하고 **중단한다.** 흡수용 상수로 넘기지 않는다.
enum을 보강한 뒤 다시 돌린다.

> 매핑표대로라면 `CourseDepartment` 2개를 추가한 뒤에는 미매핑이 나오지 않아야 한다.
> 나온다면 매핑표가 틀렸다는 신호이므로 그대로 멈추고 확인한다.

### 6. DTO

응답 6종에서 **`professor`와 `classroom` 두 컴포넌트를 모두 제거**하고, 대응하는
`.professor(...)`, `.classroom(...)` 빌더 호출도 지운다. 강의실은 `schedule` 문자열에 합쳐 나간다.

문자열 조립은 `CourseSchedule`이 맡는다. `getScheduleText()`가 저장된 원문을 그대로 돌려주던 것을
요일, 교시, 강의실을 조립하도록 바꾼다.

```java
public String getScheduleText() {
    return courseDay.getName() + "(" + periodName + ") " + classroom;
}
```

`classroom`이 `NOT NULL`이라 분기가 필요 없다.

`Course.getFormattedCourseSchedules()`는 그대로 둔다. 요일 순 정렬 후 공백으로 잇는 동작이 바뀌지 않는다.

> **`ROOM_NAME`이 길다.** 실측 최대 48자이고 `제15호관 인문대학-503 전용어학실습실-3` 같은 형태로
> 건물, 호실, 용도가 한 문자열에 붙어 온다. 시간표가 3개면 `schedule` 한 필드가 150자를 넘는다.
> 적재 시 앞의 건물, 호실 부분만 잘라 쓸지는 시드 변환 단계에서 실제 값을 보고 판단한다.

### 7. Service, Repository, Controller

**`CourseRepository.findByKeyword`만 바꾼다.** 4번에서 FULLTEXT 인덱스에 `haksu_code`를 추가하므로
네이티브 쿼리의 `MATCH(...)` 컬럼 목록을 두 군데 모두 인덱스와 똑같이 맞춘다.

```java
@Query(value = """
    SELECT DISTINCT c.*
    FROM courses c
    WHERE MATCH(c.course_code, c.haksu_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
    ORDER BY MATCH(c.course_code, c.haksu_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
""", nativeQuery = true)
List<Course> findByKeyword(@Param("keyword") final String keyword);
```

나머지는 그대로다.

- `findByCourseDepartment`, `findByCourseArea`, `findByIdWithSchedules` - 학기 조건을 붙이지 않기로 했으므로 변경 없음
- `CourseService`의 조회 5개 메서드, `CourseController`, `CourseControllerDocs` - 변경 없음
- `CourseValidator` - 변경 없음. 시간표 충돌 판정은 `CourseSchedule`의 요일과 시각만 보므로 강의실 추가의 영향을 받지 않는다

### 8. 테스트

`CourseFixture`의 팩토리 4개에서 `professorName`, `classroom` 파라미터를 빼고 `academicYear`, `courseTerm`, `haksuCode`를 넣는다.

```java
public static Course createCourseWithDetails(
        final String titleKr,
        final String titleEn,
        final String courseCode,
        final String haksuCode,
        final CourseGrade grade
) { ... }
```

`CourseScheduleFixture.createCourseSchedule`에는 `classroom`, `periodName`을 추가하고 `scheduleText`를 뺀다.

호출부는 세 테스트에서 총 178곳이다 (`RegistrationServiceTest` 96, `CartServiceTest` 48, `CourseServiceTest` 34).
시그니처를 바꾸면 전부 컴파일 에러가 나므로, 기본값 오버로드(`createCourse()`)를 최대한 살려
인자를 명시하는 호출만 고친다.

테스트는 H2 + `ddl-auto: create-drop`이라 Flyway를 타지 않는다.
즉 **4단계 마이그레이션 SQL의 정합성은 테스트로 검증되지 않는다.** 로컬 MySQL에서 직접 확인해야 한다.

### 9. 문서

`.claude/spec/service-policy/course.md`

- `## 강의 식별` 신설 - 강의는 학년도, 학기, 학수번호로 유일하다. 과목코드는 여러 분반이 공유한다
- `## 정원` - 정원은 연계 API가 제공하지 않으며 운영자가 직접 채운다는 문장 추가
- `## 강의 검색` - 검색 대상을 "학수번호, 과목코드, 국문 강의명, 영문 강의명"으로 갱신
- `## 시간표` 신설 - 강의실은 강의가 아니라 시간표에 속한다. 같은 강의도 요일별로 강의실이 다를 수 있고,
  응답의 `schedule` 문자열에 교시와 강의실이 함께 담긴다

## 결정 필요 (Decisions needed)

- [x] **1. 조회에 학기 조건을 붙일지** - **붙이지 않는다.**
  스키마에만 학년도, 학기를 넣고 `CourseRepository`의 4개 쿼리와 `CourseService`의 5개 메서드는 그대로 둔다.
  한 학기만 적재하는 동안은 동작이 같다. 두 학기가 섞이는 문제는 다음 학기 데이터를 넣는 시점에 다룬다.
  7단계가 "변경 없음"으로 확정된다.

- [x] **2. 강의실을 응답에 어떻게 노출할지** - **`schedule` 문자열에 합친다.**
  `월(1-2) 4호관 209 수(3-4) 4호관 209` 형태로 내보내고 응답 6종에서 `classroom` 컴포넌트를 제거한다.
  강의실 문자열 조립은 `CourseSchedule`이 맡고, `Course.getFormattedCourseSchedules()`는 그대로 조합만 한다.

- [x] **3. 매핑되지 않는 코드값 처리** - **중단한다.**
  시드 변환 스크립트가 미매핑 값을 만나면 코드값과 이름을 목록으로 출력하고 멈춘다.
  사람이 enum 상수를 보강한 뒤 다시 돌린다. 흡수용 상수로 넘기지 않는다.
  적재가 오프라인 일회성 작업이라, 조용히 넘어가서 조회가 안 되는 강의를 만드는 것보다 낫다.

- [x] **4. FULLTEXT 검색 대상** - **둘 다 넣는다.**
  `(course_code, haksu_code, title_kr, title_en)`으로 인덱스를 다시 만든다.
  학생이 넣는 건 주로 학수번호지만 과목코드 검색을 막을 이유가 없다.

- [x] **5. `max_capacity` 기본값** - **100으로 채운다.** 기존 시드와 같은 값이라 기존 테스트와 체감이 유지된다.
  이벤트 때 실제 정원으로 수동 조정한다.

- [x] **6. 기존 데이터 삭제 범위** - **그대로 진행한다.** `DELETE FROM courses`가 CASCADE로
  `registrations`, `carts`를 함께 비우는 것을 허용한다. 별도 백업 절차를 두지 않는다.

## 검증

- `./gradlew build` - 제거, 개명한 것들의 잔여 참조가 없는지 컴파일로 확인한다.
  `getProfessorName()`, `Course.getClassroom()`, `scheduleText`, 그리고 접두사를 뗀 getter 11개
  (`getCourseCollege`, `getCourseDepartment`, `getCourseClassification`, `getCourseArea`, `getCourseType`,
  `getCourseGrade`, `getCourseSchedules`, `getCourseDay`, `getMemberCollege`, `getMemberDepartment`, `getMemberGrade`)
- **JPQL은 컴파일로 안 잡힌다.** `CourseRepository`의 `c.department`, `c.area`, `c.grade`, `c.schedules`는
  런타임에 컨텍스트가 뜰 때 검증되므로 `./gradlew test`로 확인해야 한다. 오타가 나면 애플리케이션 기동 자체가 실패한다
- **`ReflectionTestUtils.setField`도 컴파일로 안 잡힌다.** 픽스처의 필드명 문자열이 틀리면
  컴파일은 통과하고 테스트 실행 시 예외가 난다. `CourseFixture`, `CourseScheduleFixture`, `MemberFixture` 세 곳을 확인한다
- `./gradlew test` - `CourseServiceTest`, `CartServiceTest`, `RegistrationServiceTest`가 통과하는지.
  H2는 Flyway를 타지 않으므로 엔티티 매핑만 검증된다
- 로컬 MySQL에서 `./gradlew bootRun` - V0_8 마이그레이션과 신규 시드가 순서대로 적용되는지.
  특히 `ALTER TABLE courses DROP INDEX course_code`가 실제 인덱스명과 맞는지 (MySQL은 UNIQUE 제약에 컬럼명을 인덱스명으로 붙인다)
- 시간표 충돌 판정이 여전히 동작하는지 - 같은 학수번호의 여러 시간표 행이 강의실만 다를 때
  `CourseValidator.validateCourseScheduleNotConflict`가 자기 자신과 충돌 판정하지 않는지
- Swagger UI에서 조회 4종과 검색 응답에 `professor`가 사라지고 강의실 표기가 2번 결정대로 나오는지

## Deviation Log

- `Course.java`, `CourseSchedule.java`: `schedule` 문자열을 강의실별로 묶어 `[강의실:요일(교시),요일(교시)]` 형태로 조립하도록 바꿨다.
  6단계는 `getScheduleText()`가 `월(1-2) 4호관 209`를 만들고 `Course`는 공백으로 잇기만 하는 구조였다 —
  이유: 사용자가 `[07-407:화(1-2A),목(2B-3)]` 형식을 요구했고, 강의실로 묶으려면 조립 책임이 `Course`로 올라가야 한다.
  `getScheduleText()`는 `화(1-2A)`까지만 만들고, `getFormattedCourseSchedules()`가 그룹핑과 조립을 맡는다
- 마이그레이션 구조 전면 교체 — 이유: 사용자가 운영 서버의 Flyway 히스토리를 초기화하기로 해서,
  `V0_0`(init) + `V0_7`(이메일 인증 제거) + `V0_8`(강의 재정의) 패치 체인 대신 최종 스키마를 담은
  `V1_0__init_table.sql` 하나로 다시 짰다. 기존 시드 `V0_1`~`V0_6`도 지우고 `V1_1`~`V1_9`로 재생성했다.
  4단계의 `ALTER TABLE` 나열과 5단계의 시드 버전 번호는 이 구조로 대체된다.
  아직 상용 전이라 사용자가 파일 삭제를 허가했다
- `Course.term`의 컬럼명 `course_term` → `term` — 이유: 필드는 `term`인데 컬럼만 접두사가 남아
  이번에 `domain.md`에 세운 "컬럼명은 필드명을 snake_case로 옮긴 것이어야 한다"를 어기고 있었다.
  스키마를 새로 짜는 김에 맞췄다. `term`은 MySQL, H2 모두 예약어가 아니다
- `CourseSchedule.day` → `dayOfWeek`, 컬럼 `day` → `day_of_week` — 이유: `day`가 H2 예약어라
  `create table course_schedules (... day enum(...) ...)`가 파싱에 실패해 테스트 86개가 전부 깨졌다.
  4단계는 MySQL 예약어만 확인했다. `YEAR` → `academicYear`와 같은 이유로 같은 해법을 적용했다
- 응답 DTO 6종: `haksuCode` 컴포넌트를 추가했다 — 이유: `courseCode`의 의미가 학수번호에서 과목코드로 바뀌면서
  응답에서 분반 식별자가 사라진다. 과목코드는 분반 1,499개가 공유해 분반을 구분하지 못한다.
  검색은 이미 `haksu_code`를 FULLTEXT 대상에 넣었는데 정작 결과에 그 값이 없는 불일치도 생긴다
- `CourseDay.SUNDAY`: `code`를 빈 문자열로 뒀다 — 이유: 실데이터에 일요일이 없어 코드값을 확인할 수 없다.
  3단계가 `무역학부`, `국제개발협력연계전공`에 적용한 처리와 같다
- enum 8종의 `fromCode()`: 코드가 빈 상수를 매칭 대상에서 제외했다 — 이유: 3단계대로 빈 문자열을 두면
  `fromCode("")`가 `TRADE`와 `SUNDAY`를 반환한다. 배치가 Java로 옮겨온 뒤 API가 빈 코드를 보내면
  개설도 안 된 학과로 조용히 적재된다. 코드 없는 상수는 이름(`from`)으로만 찾게 하고 `fromCode`로는 못 찾게 했다
- `CartRepository.findByMemberId`: JPQL의 `course.courseSchedules` → `course.schedules`로 고쳤다 —
  이유: "`CartRepository`는 `c.course.id`만 참조하므로 영향이 없다"는 전제가 틀렸다. 이 쿼리는 `LEFT JOIN FETCH`로
  컬렉션을 가져오고 있었고, 고치기 전까지 컨텍스트 로딩이 실패했다
- 시드 규모: 5단계가 예상한 "시간표 약 6,300행"이 아니라 5,494행이다 —
  이유: 2학기 시간표 6,765행 중 1,271행이 2학기 강좌정보에 없는 학수번호를 참조한다.
  미조인 1,286행은 전체 학기 기준이라 2학기만 거르면 수치가 달라진다
- 테스트 4개 삭제 (`CourseServiceTest` 2, `CartServiceTest` 1, `RegistrationServiceTest` 1) —
  이유: `null값인_교수명과_강의실은_하이픈으로_반환된다` 계열로, 검증 대상 필드가 응답에서 사라져 남길 근거가 없다
