# [PLAN-79] 강의 스키마 확장과 필터 조회 API 추가

> 이슈: #79
> 브랜치: feat/79-course-schema-extension

## 목표

프론트엔드가 요구한 강의 정보(75분 수업 여부, 수업유형, 집중이수제, 원어강의 구분명, HUSS 여부)를 제공하고, 카테고리·년도학기·연계전공 목록 조회와 HUSS 강의 조회 API를 추가한다. 코드와 명칭은 학교 연계 API 원문 그대로 저장해 내려주고, enum은 비즈니스 규칙이 필요한 곳에만 남긴다. 시드는 2026-2 기준으로 전량 재생성한다.

> **HUSS 조회 추가 (구현 중 확정).** 최초 계획은 `isHussCourse` 필드만 두고 조회 API를 범위 밖으로 뒀다.
> 그 결과 컬럼에 적재만 되고 어떤 경로로도 읽을 수 없는 상태가 됐다.
> 간접 조회를 실측해보니 연계전공 조회(32건)와 수업유형 `26`,`27`,`28` 필터(34건)를 합쳐도
> 실제 HUSS 35건 중 34건까지만 잡히고, 나머지 1건(`Global Trade & Service학부`의 `강의(이론)` 유형)은 어느 쪽으로도 잡히지 않는다.
> 게다가 수업유형 필터는 프론트엔드가 코드 목록을 하드코딩해야 해서, 이 이슈의 설계 방향과 어긋난다.
> 따라서 전용 조회 API와 응답 필드 노출을 이번 범위에 포함한다.

## 사전 확인으로 드러난 기존 코드 문제

계획에 반영해야 할 두 가지다.

**1. 학년 정렬이 깨져 있다.** `CourseRepository.findByDepartment`와 `findByArea`는 `ORDER BY c.grade`를 쓰는데, `Course.grade`가 `@Enumerated(EnumType.STRING)`이라 컬럼에 enum 이름이 문자열로 들어간다. 따라서 정렬이 사전순이 되어 `ALL, FRESHMAN, JUNIOR, SENIOR, SOPHOMORE`, 즉 **전학년, 1학년, 3학년, 4학년, 2학년** 순으로 나온다. 이번 정렬 요구사항 적용으로 함께 해소된다.

**2. 이슈 본문의 "75분 판별 원천 불가" 서술은 과장이었다.** 기존 시드의 `period_name`에는 교시구분명이 그대로 들어 있고(`1-2A`, `2B-3`, `7-8A`, `8B-9`, `야1-2A`, `야2B-3`), B계열만 하이픈을 포함하므로 추론 자체는 가능하다. 다만 명칭 표기에 의존하는 암묵적 규칙이라 깨지기 쉬우므로, 원천 교시 코드(`LECTM_CODE`)를 저장하는 방향은 그대로 간다.

## 영향 범위

### 신규 파일
- `src/main/java/uss/code/course/dto/common/CourseCategory.java` — 카테고리 조회 projection record
- `src/main/java/uss/code/course/dto/common/CourseTermInfo.java` — 년도, 학기 조회 projection record
- `src/main/java/uss/code/course/dto/response/CourseAreaResponse.java` — 이수영역 1건 (카테고리 2단계)
- `src/main/java/uss/code/course/dto/response/CourseCategoryResponse.java` — 이수구분 1건 + 하위 영역 목록
- `src/main/java/uss/code/course/dto/response/CourseCategoriesResponse.java` — 카테고리 목록 래퍼
- `src/main/java/uss/code/course/dto/response/CourseTermResponse.java` — 년도, 학기 1건
- `src/main/java/uss/code/course/dto/response/CourseTermsResponse.java` — 년도, 학기 목록 래퍼
- `src/main/java/uss/code/course/dto/response/InterdisciplinaryMajorResponse.java` — 연계전공 1건
- `src/main/java/uss/code/course/dto/response/InterdisciplinaryMajorsResponse.java` — 연계전공 목록 래퍼
- `src/main/resources/database/migration/V1_0__init_table.sql` — 재작성 (결정 1에 따름)
- `src/main/resources/database/seed/V1_1__insert_course.sql` 이하 — 재생성
- `.local/generate-seed.sh` — 연계 API JSON을 시드 SQL로 변환하는 생성 스크립트 (커밋 대상 아님)

### 수정 파일
- `src/main/java/uss/code/course/domain/Course.java` — 원문 코드·명칭 필드로 교체 및 신규 필드 추가
- `src/main/java/uss/code/course/domain/CourseSchedule.java` — 교시 코드 필드 추가, 75분 판별 메서드
- `src/main/java/uss/code/course/domain/CourseClassification.java` — 엔티티 매핑에서 제거, 코드 상수 용도로 축소하거나 삭제
- `src/main/java/uss/code/course/domain/CourseGrade.java` — 엔티티 매핑에서 제거
- `src/main/java/uss/code/course/domain/CourseType.java` — 엔티티 매핑에서 제거, `CourseValidator`용 코드 상수로 유지
- `src/main/java/uss/code/course/domain/CourseArea.java` — 유지. `fromCode`의 예외 던지기만 완화
- `src/main/java/uss/code/course/domain/CourseDepartment.java` — 유지. `fromCode`의 예외 던지기만 완화
- `src/main/java/uss/code/course/infra/CourseValidator.java` — `getType()` enum 비교를 코드 문자열 비교로 전환
- `src/main/java/uss/code/course/repository/CourseRepository.java` — 정렬 변경, 조회 메서드 3개 추가
- `src/main/java/uss/code/course/service/CourseService.java` — 조회 메서드 3개 추가
- `src/main/java/uss/code/course/controller/CourseController.java` — 엔드포인트 3개 추가
- `src/main/java/uss/code/course/controller/CourseControllerDocs.java` — 신규 3개 문서화, 기존 응답 필드 변경 반영
- `src/main/java/uss/code/course/dto/response/MajorCourseResponse.java` — 필드 추가
- `src/main/java/uss/code/course/dto/response/GeneralEducationCourseResponse.java` — 필드 추가
- `src/main/java/uss/code/course/dto/response/InterdisciplinaryMajorCourseResponse.java` — 필드 추가
- `src/main/java/uss/code/course/dto/response/SearchedCourseResponse.java` — 필드 추가
- `src/main/java/uss/code/global/config/CorsConfig.java` — 허용 오리진 2개 추가
- `.claude/spec/service-policy/course.md` — 정렬 규칙 변경, 75분 판별 규칙 추가 (정책 변경)

## 구현 계획

### 1. Entity / Flyway

**`Course`** — enum 매핑 4개를 원문 코드·명칭 쌍으로 교체하고 신규 필드를 추가한다.

| 개념 | 현재 | 변경 후 | 이유 |
|---|---|---|---|
| 이수구분 | `CourseClassification classification` | `String classificationCode`, `String classificationName` | 정렬 기준, 카테고리 API 원천 |
| 학년 | `CourseGrade grade` | `String gradeCode`, `String gradeName` | 정렬 기준. 코드가 `0`~`4`라 문자열 정렬이 곧 의미 순서 |
| 수업유형 | `CourseType type` | `String typeCode`, `String typeName` | 원문 명칭 보존(코드 `11`은 원문이 `담장너머~,사회봉사(1)`) |
| 이수영역 | `CourseArea area` | `CourseArea area` 유지 + `String areaCode`, `String areaName` | 교양 영역 판별에 enum이 필요 |
| 집중이수제 | 없음 | `String concentrationCode`, `String concentrationName` | 신규 |
| 원어강의 | `boolean isEnglishCourse` | 유지 + `String englishCode`, `String englishName` | 신규 |
| HUSS | 없음 | `boolean isHussCourse` | 신규 |

`college`, `department`는 조회 조건과 연계전공 판별에 쓰이므로 enum을 유지한다.

**`CourseSchedule`** — `String periodCode` 추가(`LECTM_CODE`). `periodName`은 유지한다.

```java
private static final String LONG_LESSON_CODE_PREFIX = "B";

public boolean is75MinLesson() {
    return periodCode.startsWith(LONG_LESSON_CODE_PREFIX);
}
```

**`Course`** — 강의 단위 판별 메서드를 추가한다.

```java
public boolean is75MinLesson() {
    return schedules.stream().anyMatch(CourseSchedule::is75MinLesson);
}
```

> 판별을 접두사 `B`로 두는 근거: 실데이터의 교시 코드는 `A00~A15`(50분), `B00~B06`,`B10`,`B11`(75분), `C01~C06`(50분) 세 계열이며 B계열만 종료-시작 차이가 75분이다. 프론트엔드가 제시한 열거 목록(`B00~B06`,`B10`,`B11`,`B12`)과 결과가 일치하고, 목록에 있는 `B12`는 실데이터에 존재하지 않는다.

**Flyway** — `V1_0__init_table.sql`의 `courses`, `course_schedules` 정의를 위 필드에 맞춰 재작성한다. 정렬용 인덱스를 추가한다.

```sql
INDEX idx_department_sort (department, grade_code, classification_code, haksu_code),
INDEX idx_area_sort (area, grade_code, classification_code, haksu_code),
INDEX idx_huss_sort (is_huss_course, grade_code, classification_code, haksu_code)
```

`period_code VARCHAR(8) NOT NULL`을 `course_schedules`에 추가한다.

### 2. Repository

`CourseRepository`의 기존 두 쿼리에 정렬을 적용한다.

```java
@Query("""
    SELECT DISTINCT c
    FROM Course c
    LEFT JOIN FETCH c.schedules
    WHERE c.department = :department
    ORDER BY c.gradeCode, c.classificationCode, c.haksuCode
""")
List<Course> findByDepartment(@Param("department") final CourseDepartment department);
```

`findByArea`도 동일한 `ORDER BY`를 적용한다.

신규 쿼리 3개를 추가한다.

```java
@Query("""
    SELECT DISTINCT new uss.code.course.dto.common.CourseCategory(
        c.classificationCode, c.classificationName, c.areaCode, c.areaName)
    FROM Course c
    ORDER BY c.classificationCode, c.areaCode
""")
List<CourseCategory> findCategories();

@Query("""
    SELECT DISTINCT new uss.code.course.dto.common.CourseTermInfo(c.academicYear, c.term)
    FROM Course c
    ORDER BY c.academicYear DESC, c.term
""")
List<CourseTermInfo> findTerms();

@Query("""
    SELECT DISTINCT c.department
    FROM Course c
    WHERE c.department IN :departments
""")
List<CourseDepartment> findDepartmentsIn(@Param("departments") final List<CourseDepartment> departments);

@Query("""
    SELECT DISTINCT c
    FROM Course c
    LEFT JOIN FETCH c.schedules
    WHERE c.isHussCourse = true
    ORDER BY c.gradeCode, c.classificationCode, c.haksuCode
""")
List<Course> findHussCourses();
```

### 3. Service

`CourseService`에 4개를 추가한다.

```java
@Transactional(readOnly = true)
public CourseCategoriesResponse getCategories()
```
`findCategories()` 결과를 `classificationCode` 기준으로 묶어 1단계(이수구분) 아래 2단계(이수영역) 목록을 만든다. `LinkedHashMap`으로 쿼리 정렬 순서를 보존한다.

```java
@Transactional(readOnly = true)
public CourseTermsResponse getTerms()
```
`findTerms()` 결과를 그대로 매핑한다.

```java
@Transactional(readOnly = true)
public InterdisciplinaryMajorsResponse getInterdisciplinaryMajors()
```
`CourseDepartment`에서 연계전공 목록을 얻어 `findDepartmentsIn(...)`으로 실제 데이터가 있는 것만 남긴다. 이를 위해 `CourseDepartment.isInterdisciplinary()`를 `public static List<CourseDepartment> interdisciplinaryValues()`로 노출한다.

```java
@Transactional(readOnly = true)
public MajorCoursesResponse getHussCourses()
```
`findByHussCourse()` 결과를 매핑한다. HUSS 강의는 학과가 여러 개에 걸쳐 있어 응답에 학과명이 필요하므로, 타학과 조회와 같이 `MajorCoursesResponse`를 재사용한다.

### 4. DTO

**projection record** (`dto/common/`)

```java
public record CourseCategory(String classificationCode, String classificationName,
                             String areaCode, String areaName) {}
public record CourseTermInfo(int academicYear, CourseTerm term) {}
```

**응답 record** (`dto/response/`)

```java
public record CourseAreaResponse(String code, String name) {}
public record CourseCategoryResponse(String code, String name, List<CourseAreaResponse> areaResponses) {}
public record CourseCategoriesResponse(List<CourseCategoryResponse> categoryResponses) {}
public record CourseTermResponse(int academicYear, String termCode, String termName) {}
public record CourseTermsResponse(List<CourseTermResponse> termResponses) {}
public record InterdisciplinaryMajorResponse(String code, String name) {}
public record InterdisciplinaryMajorsResponse(List<InterdisciplinaryMajorResponse> interdisciplinaryMajorResponses) {}
```

**기존 응답 4종에 공통 추가할 필드** (`MajorCourseResponse`, `GeneralEducationCourseResponse`, `InterdisciplinaryMajorCourseResponse`, `SearchedCourseResponse`)

```text
boolean is75MinLesson
String  suupTypeCode
String  suupTypeName
String  cnctrIsuCode
String  cnctrIsuName
String  englishCourseName
boolean isHussCourse
```

> `englishCourseName`은 `isEnglishCourse`가 `false`면 `null`로 둔다. 원천이 `N`일 때도 `비대상`을 채워 보내지만, 프론트엔드 요구가 "`ENGLISH_YN='Y'`인 경우 연동"이므로 서버에서 걸러 내려준다.

`classification`, `grade`, `area` 컴포넌트는 enum의 `getName()` 대신 저장된 원문 명칭을 쓴다. 컴포넌트명과 타입은 그대로 유지해 프론트엔드 파싱이 깨지지 않게 한다.

### 5. Controller

```text
GET /api/v1/courses/categories               → CourseController.getCategories()
GET /api/v1/courses/terms                    → CourseController.getTerms()
GET /api/v1/courses/interdisciplinary-majors → CourseController.getInterdisciplinaryMajors()
GET /api/v1/courses/huss                     → CourseController.getHussCourses()
```

`CourseControllerDocs`에 4개의 `@Operation`을 추가한다. 네 API 모두 파라미터가 없고 인증만 요구한다.

### 6. CORS

`CorsConfig.ALLOWED_ORIGINS`에 2개를 추가한다. `http://localhost:5173`은 이미 있다.

```
https://ultimate-sugang-web.inuappcenter.kr
https://ultimate-sugang-web.pages.dev
```

### 7. 시드 생성

`.local/generate-seed.sh`가 연계 API JSON 2개를 읽어 시드 SQL을 출력한다. 스크립트만 유지보수하고 생성된 SQL은 읽지 않는다.

처리 규칙:
- `TERM_CODE = '20'`만 통과시킨다 (여름계절학기 142건 제외)
- 강좌 2,439건, 시간표 9,109건을 생성한다
- `max_capacity`는 100, `current_enrollment`는 0으로 채운다 (`course.md` 정책)
- 학년구분명은 원천이 숫자면 `학년`을 붙여 저장한다 (`2` → `2학년`, `전학년`은 그대로)
- 강의실은 `ROOM_NAME` 원문에서 건물과 호실만 남긴다. 원문이 `제12호관 컨벤션센터-101 용정강의실(계단식(대))` 형태이므로 호관번호를 두 자리로 줄여 `12-101`로 만들고, `제N호관`이 아닌 22종(`가상건물-200 온라인강의실` 등)은 건물명을 그대로 써 `가상건물-200`으로 만든다
- 시간표 INSERT의 `course_id`는 현재 행마다 서브쿼리를 도는 방식이다. 9,109행으로 늘어나므로 임시 테이블에 학수번호를 적재한 뒤 조인하는 방식으로 바꾼다

## 결정 필요 (Decisions needed)

- [x] **마이그레이션 파일 전략** — **전체 재작성.** `V1_0`과 기존 시드 파일을 지우고 처음부터 다시 쓴다. Flyway 이력 초기화가 전제다. 증분 `ALTER` 없이 최종 스키마를 바로 읽을 수 있다
- [x] **시드 형식** — **INSERT문 + 생성 스크립트.** 현행 형식을 유지하되 손으로 쓰지 않고 `.local/generate-seed.sh`로 뽑는다. Flyway와 마찰이 없고, 결과 SQL은 사람도 도구도 열어볼 일이 없다
- [x] **검색 API 정렬** — **현행 관련도순 유지.** `course.md`가 검색을 "관련도가 높은 순"으로 규정하고 있고, 새 정렬 요구는 목록 조회에 대한 것이다. `findByKeyword`는 건드리지 않는다
- [x] **학년 정렬에서 전학년의 위치** — **맨 앞.** 학년 코드 `0`(전학년), `1`~`4`의 문자열 오름차순 그대로 둔다. `ORDER BY` 한 줄로 끝나고 정렬 인덱스를 그대로 탄다
- [x] **학년구분명 가공 여부** — **서버가 가공한다.** 원천이 `1`~`4`로만 오므로 시드 생성 시점에 `1학년` 형태로 만들어 저장한다(`0`은 `전학년`). 기존 `CourseGrade` enum의 명칭과 같아져 응답 표기가 바뀌지 않는다. 정렬은 `gradeCode`로 하므로 영향이 없다
- [x] **이수구분 정렬 기준** — **코드 오름차순으로 진행한다.** `11 기초교양, 21 핵심교양, 23 심화교양, 25 전공기초, 31 전공핵심, 41 전공심화, 50 교직, 70 군사학, 80 일반선택` 순이 된다. 전산원 자료 순서와 다르면 `ORDER BY`만 바꾸면 되므로 프론트엔드 확인은 구현과 병행한다

### 프론트엔드에 확인 요청할 사항

- 위 이수구분 정렬 순서가 전산원 자료 기준과 일치하는지
- 년도, 학기 조회 API의 응답이 `2026년 2학기` 한 건뿐이다(정책상 항상 한 학기만 적재). 다학기 UI를 전제하고 있다면 어긋난다
- 프론트엔드가 전달한 코드 범위 두 건이 실데이터와 달랐다(수업유형 `9`, `10` 부재 / 집중이수제 `2` 부재, `0`이 99.7%). 서버는 원문을 그대로 내리므로 렌더링 분기 기준을 재확인해야 한다

## 검증

- **대상 테스트**: `src/test/java/uss/code/course/` 하위
  - `CourseServiceTest` — 카테고리 2단계 묶음이 이수구분별로 정확히 그룹핑되는지, 연계전공 목록이 데이터에 있는 것만 반환하는지
  - `CourseRepositoryTest` — 정렬이 학년, 이수구분, 학수번호 순으로 나오는지. 전학년(`0`)과 1~4학년이 섞인 픽스처로 검증한다
  - `CourseScheduleTest` — `B01`은 `true`, `A03`과 `C01`은 `false`를 반환하는지
  - `CourseValidatorTest` — 수업유형 비교를 코드 문자열로 바꾼 뒤에도 OCU 2개, K-MOOC 1개 제한이 유지되는지
- **적재 검증**: 마이그레이션 적용 후 `courses` 2,439행, `course_schedules` 9,109행, `period_code`가 `B`로 시작하는 행이 존재하는지 확인한다

## Deviation Log

- **HUSS 조회 API와 응답 필드를 범위에 추가** (계획서 본문도 함께 갱신) — 이유: 최초 계획은 `isHussCourse`를 적재만 하고 노출하지 않아 컬럼이 읽기 불가 상태였다. 간접 조회 실측 결과 연계전공(32건) + 수업유형 `26`,`27`,`28`(34건)을 합쳐도 35건 중 34건까지만 잡힌다. 사용자 확인 후 `GET /api/v1/courses/huss`와 응답 필드 `isHussCourse`를 추가했다
- `V1_0__init_table.sql`: `idx_huss_sort` 인덱스 추가 — 이유: HUSS 조회가 다른 목록 조회와 같은 정렬을 쓰므로 같은 형태의 인덱스를 둔다. 실제로 인덱스를 타고 filesort가 없는 것을 확인했다
- `database/seed/`: 시간표를 9,109행이 아니라 **7,819행** 생성 — 이유: `TERM_CODE='20'` 시간표 9,109행 중 1,290행(488개 학수번호)이 강좌 목록에 없는 학수번호를 가리킨다. 이 학수번호들은 `A_MAP_COURSE_INFO.json` 어디에도 없어(여름계절학기 강좌도 아니다) `course_id`를 붙일 수 없다. 계획서의 9,109는 조인 전 원본 건수를 적은 것이다
- `.local/generate-seed.sh`: enum 매핑표를 스크립트에 적지 않고 `domain/`의 enum 정의를 파싱해 생성 — 이유: 매핑표를 복사해두면 enum이 바뀔 때 시드가 조용히 어긋난다
- `.local/generate-seed.sh`: 모든 명칭 값의 공백을 정규화 — 이유: 원천 `COURSE_NM_ENG`에 줄바꿈 2건과 앞뒤 공백 다수가 섞여 있다. 그대로 두면 SQL 문자열에 개행이 들어가고 FULLTEXT 검색과 표기가 흔들린다
- `database/seed/V1_4__insert_course_schedule.sql`: 시간표를 6개 파일이 아닌 1개 파일로 생성 — 이유: 임시 테이블은 세션 단위라 CREATE·INSERT·JOIN·DROP이 한 마이그레이션 안에 있어야 한다
- `CourseClassification`, `CourseGrade`: 삭제하지 않고 코드·명칭 상수표로 유지 — 이유: 엔티티 매핑에서는 빠졌지만 테스트 픽스처가 코드와 명칭을 함께 얻는 데 쓴다. `CourseType`을 남긴 것과 같은 이유다
- `CourseArea.fromCode`, `CourseDepartment.fromCode`: 예외 던지기를 완화하지 않고 그대로 둠 — 이유: 두 메서드는 메인 코드에서 호출되지 않는다(시드 생성이 SQL로 옮겨간 뒤 호출부가 사라졌다). 완화해도 동작이 바뀌는 곳이 없고 기존 테스트만 깨진다
- `CourseService.getInterdisciplinaryMajors`: 쿼리 결과를 그대로 매핑하지 않고 `interdisciplinaryValues()` 순서로 필터링 — 이유: `SELECT DISTINCT`는 순서를 보장하지 않아 응답 순서가 요청마다 달라질 수 있다
- `CartedCourseResponse`, `RegistrationCourseResponse`: 계획서 "수정 파일"에 없지만 수정 — 이유: `course.getClassification()`을 호출하고 있어 컴파일이 깨진다
- `src/test/`: `CourseFixture`, `CourseScheduleFixture`를 새 필드에 맞춰 갱신하고 `CartServiceTest`, `RegistrationServiceTest`의 `getClassification()` 호출을 상수로 교체 — 이유: 테스트 작성은 이 스킬 범위 밖이지만, 두지 않으면 테스트가 컴파일되지 않는다. 계획서 "검증"에 적힌 신규 테스트는 작성하지 않았다
