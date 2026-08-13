# [PLAN-87] 실제 학사구조에 맞춘 학과 체계 개편

> 이슈: #87
> 브랜치: refactor/87-department-structure

## 목표

학생 소속(`MemberDepartment`)을 실제 학사구조에 맞추고, 소속에서 강의 학과로 가는 매핑을 1:N으로 명시해
학부 소속 학생이 하위 전공 강의까지 조회하게 만든다. 이름 기반 `valueOf` 매칭을 걷어내 상수명 변경에
조용히 깨지지 않는 구조로 바꾼다.

## 영향 범위

### 신규 파일
- `src/main/resources/database/migration/V1_8__realign_department_structure.sql` — `members.department`,
  `courses.department`의 기존 enum 이름을 새 이름으로 이관

### 수정 파일
- `src/main/java/uss/code/member/domain/MemberCollege.java` — 동북아국제통상물류학부 추가 (14개 -> 15개)
- `src/main/java/uss/code/member/domain/MemberDepartment.java` — 학사구조 기준 재구성 (55개 -> 62개, `DEFAULT` 제외)
- `src/main/java/uss/code/course/domain/CourseDepartment.java` — `owner` 필드 추가, 상수명 정리,
  `from(MemberDepartment)` 제거, `ownedBy` / `hasOwner` 추가
- `src/main/java/uss/code/course/repository/CourseRepository.java` — `findByDepartmentIn` 추가
- `src/main/java/uss/code/course/service/CourseService.java` — 전공, 타학과 조회를 목록 조건으로 변경
- `src/main/java/uss/code/member/domain/Member.java` — `updateDepartment`가 단과대학도 함께 갱신
- `src/main/java/uss/code/member/service/MemberService.java` — 학과 수정 시 `DEFAULT` 차단
- `src/main/java/uss/code/global/exception/domain/ExceptionCode.java` — `INVALID_DEPARTMENT` 추가
- `src/main/java/uss/code/member/dto/request/DepartmentUpdateRequest.java` — `@Schema` example 갱신
- `src/main/java/uss/code/course/controller/CourseControllerDocs.java` — 타학과 조회 파라미터 설명 갱신
- `.claude/spec/service-policy/course.md` — 전공, 타학과 조회 규칙과 알려진 제약 갱신 (**정책 변경**)
- `.claude/spec/service-policy/member.md` — 학과 목록과 소속 단위 설명 갱신 (**정책 변경**)
- `src/test/java/uss/code/course/domain/CourseDepartmentTest.java` — 상수명, 매핑 테스트 갱신
- `src/test/java/uss/code/course/service/CourseServiceTest.java` — 전공, 타학과 조회 시나리오 갱신
- `src/test/java/uss/code/member/service/MemberServiceTest.java` — 학과 수정 시나리오 갱신
- `src/test/java/uss/code/member/fixture/MemberFixture.java`, `src/test/java/uss/code/course/fixture/CourseFixture.java` — 상수명 반영
- `src/test/java/uss/code/admin/infra/CourseSyncApplierTest.java`, `src/test/java/uss/code/admin/service/CourseSyncServiceTest.java`,
  `src/test/java/uss/code/auth/service/AuthServiceTest.java`, `src/test/java/uss/code/registration/service/RegistrationServiceTest.java`,
  `src/test/java/uss/code/course/domain/CourseTest.java` — 상수명 반영

**시드 데이터(`database/seed/`)는 건드리지 않는다.** 적용된 Flyway 파일이며, 값 이관은 신규 마이그레이션에서 한다.

## 구현 계획

### 1. Entity / Flyway

**`MemberCollege`** — 상수 추가.
```java
NORTHEAST_ASIA_TRADE_LOGISTICS("동북아국제통상물류학부"),
```
`DEFAULT` 앞에 둔다. 강의 축의 `CourseCollege.NONE`("단과대구분없음")과 이름이 다른 것은 의도다.
법학부가 이미 같은 구조다(`MemberCollege.LAW`="법학부" / `CourseCollege.LAW`="단과대구분없음(법학)").

**`MemberDepartment`** — 학사구조 기준 재구성. 62개 + `DEFAULT`.

| 구분 | 상수 | 표시명 |
|---|---|---|
| 제거 | `ELECTRONICS_ENGINEERING` | 전자공학과 |
| 제거 | `TRADE` | 무역학부 |
| 추가 | `ELECTRONICS_ENGINEERING_SCHOOL` | 전자공학부 |
| 추가 | `GLOBAL_TRADE_SERVICE` | Global Trade & Service학부 |
| 추가 | `ECONOMICS_NIGHT` | 경제학과(야) |
| 추가 | `TRADE_NIGHT` | 무역학부(야) |
| 추가 | `NORTHEAST_ASIAN_TRADE_MAJOR` | 동북아국제통상전공 |
| 추가 | `SMART_LOGISTICS_ENGINEERING_MAJOR` | 스마트물류공학전공 |
| 추가 | `IBE_MAJOR` | IBE전공 |
| 추가 | `INTERNATIONAL_LIBERAL_ARTS` | 국제자유전공학부 |
| 추가 | `CONVERGENCE` | 융합학부 |
| 이름 변경 | `CIVIL_ENVIRONMENT_ENGINEERING` -> `URBAN_ENVIRONMENT_ENGINEERING_SCHOOL` | 도시환경공학부 |
| 이름 변경 | `URBAN_ARCHITECTURE` -> `URBAN_ARCHITECTURE_SCHOOL` | 도시건축학부 |
| 이름 변경 | `LIFE_SCIENCE` -> `LIFE_SCIENCE_SCHOOL` | 생명과학부 |
| 이름 변경 | `BIOENGINEERING` -> `BIOENGINEERING_SCHOOL` | 생명공학부 |
| 이름 변경 | `FINE_ARTS` -> `FINE_ARTS_SCHOOL` | 조형예술학부 |

동북아 3전공의 `MemberCollege`는 `NORTHEAST_ASIA_TRADE_LOGISTICS`, 야간 2개는 `COMMERCE_PUBLIC_AFFAIRS`,
국제자유전공학부와 융합학부는 `LIBERAL_ARTS_COLLEGE`다.

`from(String)`은 유지하되 `DEFAULT`를 선택 불가로 막는 메서드를 추가한다.
```java
public static MemberDepartment fromSelectable(final String value)
```
`DEFAULT`면 `INVALID_DEPARTMENT`를 던진다. `DEFAULT`는 포털 신규 가입자의 미설정 상태이지 선택지가 아니다.

**`CourseDepartment`** — 상수 89개를 유지하고 필드와 상수명을 정리한다.

명명 규칙: 학부는 `_SCHOOL`, 전공은 `_MAJOR`, 학과는 접미사 없음. 학부와 하위 전공이 함께 있는 그룹은
그룹 전체에 규칙을 적용해 한 눈에 층위가 보이게 한다.

| 그룹 | 변경 전 -> 변경 후 |
|---|---|
| 전자공학부 | `ELECTRONICS_ENGINEERING_DEPARTMENT` -> `ELECTRONICS_ENGINEERING_SCHOOL`, `SEMICONDUCTOR_CONVERGENCE` -> `SEMICONDUCTOR_CONVERGENCE_MAJOR` (`ELECTRONICS_ENGINEERING_MAJOR` 유지, 폐지 학과 `ELECTRONICS_ENGINEERING` 유지) |
| 도시환경공학부 | `URBAN_ENVIRONMENT_ENGINEERING_DEPARTMENT` -> `URBAN_ENVIRONMENT_ENGINEERING_SCHOOL`, `CIVIL_ENVIRONMENT_ENGINEERING` -> `CIVIL_ENVIRONMENT_ENGINEERING_MAJOR`, `ENVIRONMENT_ENGINEERING` -> `ENVIRONMENT_ENGINEERING_MAJOR` |
| 도시건축학부 | `URBAN_ARCHITECTURE_DEPARTMENT` -> `URBAN_ARCHITECTURE_SCHOOL`, `ARCHITECTURE_ENGINEERING` -> `ARCHITECTURE_ENGINEERING_MAJOR`, `URBAN_ARCHITECTURE` -> `URBAN_ARCHITECTURE_MAJOR` |
| 생명과학부 | `LIFE_SCIENCE_DEPARTMENT` -> `LIFE_SCIENCE_SCHOOL`, `LIFE_SCIENCE` -> `LIFE_SCIENCE_MAJOR`, `MOLECULAR_LIFE_SCIENCE` -> `MOLECULAR_LIFE_SCIENCE_MAJOR` |
| 생명공학부 | `BIOENGINEERING_DEPARTMENT` -> `BIOENGINEERING_SCHOOL`, `BIOENGINEERING` -> `BIOENGINEERING_MAJOR`, `NANO_BIOENGINEERING` -> `NANO_BIOENGINEERING_MAJOR` |
| 조형예술학부 | `FINE_ARTS` -> `FINE_ARTS_SCHOOL`, `KOREAN_PAINTING` -> `KOREAN_PAINTING_MAJOR`, `WESTERN_PAINTING` -> `WESTERN_PAINTING_MAJOR` |
| 동북아 | `NORTHEAST_ASIAN_TRADE` -> `NORTHEAST_ASIAN_TRADE_MAJOR`, `SMART_LOGISTICS_ENGINEERING` -> `SMART_LOGISTICS_ENGINEERING_MAJOR`, `IBE` -> `IBE_MAJOR` |

`code` 값은 하나도 바꾸지 않는다. 동기화는 코드로 매칭하므로 상수명 변경에 영향받지 않는다.

세 번째 필드로 소속을 추가한다.
```java
private final String code;
private final CourseCollege courseCollege;
private final String name;
private final MemberDepartment owner;
```
`owner`가 `null`인 값은 학생 소속이 없는 것이다: 교양, 교직, 일선, 군사학, 연계전공 11종, HUSS 2종.
폐지 학과는 후신 학부를 `owner`로 갖는다 (`ELECTRONICS_ENGINEERING` -> `ELECTRONICS_ENGINEERING_SCHOOL`).

`code`가 비어 있고 개설 강의도 0건인 `TRADE`(무역학부)와 `INTERNATIONAL_DEVELOPMENT_COOPERATION`
(국제개발협력연계전공)은 재개설 가능성을 고려해 **남긴다**. `TRADE`의 `owner`는 후신인
`GLOBAL_TRADE_SERVICE`로 두고, `INTERNATIONAL_DEVELOPMENT_COOPERATION`은 연계전공이므로 `null`이다.
둘 다 `code`가 빈 문자열이라 `tryFromCode`에 걸리지 않으므로 동기화에는 영향이 없다.

**`V1_8__realign_department_structure.sql`** — `members`와 `courses`의 enum 문자열을 이관한다.
```sql
-- 폐지 학과를 후신 학부로 이관한다
UPDATE members SET department = 'ELECTRONICS_ENGINEERING_SCHOOL' WHERE department = 'ELECTRONICS_ENGINEERING';
UPDATE members SET department = 'GLOBAL_TRADE_SERVICE'           WHERE department = 'TRADE';

-- 학부, 전공 층위가 드러나도록 바뀐 이름을 반영한다
UPDATE members SET department = 'URBAN_ENVIRONMENT_ENGINEERING_SCHOOL' WHERE department = 'CIVIL_ENVIRONMENT_ENGINEERING';
UPDATE members SET department = 'URBAN_ARCHITECTURE_SCHOOL'            WHERE department = 'URBAN_ARCHITECTURE';
UPDATE members SET department = 'LIFE_SCIENCE_SCHOOL'                  WHERE department = 'LIFE_SCIENCE';
UPDATE members SET department = 'BIOENGINEERING_SCHOOL'                WHERE department = 'BIOENGINEERING';
UPDATE members SET department = 'FINE_ARTS_SCHOOL'                     WHERE department = 'FINE_ARTS';

-- 강의의 학과 상수명을 반영한다 (21건, 위 표와 동일)
UPDATE courses SET department = 'ELECTRONICS_ENGINEERING_SCHOOL' WHERE department = 'ELECTRONICS_ENGINEERING_DEPARTMENT';
-- ... 나머지 상수도 같은 형태로 이어 쓴다
```
`members`와 `courses`의 갱신 순서를 섞지 말고 테이블별로 묶는다. `department` 컬럼은 `VARCHAR(50)`이고
새 이름 중 가장 긴 `URBAN_ENVIRONMENT_ENGINEERING_SCHOOL`이 36자라 길이 여유가 있다.

### 2. Repository

`CourseRepository.findByDepartmentIn` 추가. 기존 `findByDepartment`는 연계전공 조회가 계속 쓰므로 남긴다.
```java
@Query("""
    SELECT DISTINCT c
    FROM Course c
    LEFT JOIN FETCH c.schedules
    WHERE c.department IN :departments
      AND c.status = uss.code.course.domain.CourseStatus.ACTIVE
    ORDER BY c.gradeCode, c.classificationCode, c.haksuCode
""")
List<Course> findByDepartmentIn(@Param("departments") final List<CourseDepartment> departments);
```
기존 `idx_department_sort (department, grade_code, classification_code, haksu_code)`를 그대로 탄다.

### 3. Domain 정적 메서드

`CourseDepartment`에서 `from(MemberDepartment)`를 **제거**하고 아래를 추가한다.
```java
public static List<CourseDepartment> ownedBy(final MemberDepartment memberDepartment) {
    return Arrays.stream(values())
            .filter(department -> department.owner == memberDepartment)
            .toList();
}

public boolean hasOwner() {
    return owner != null;
}
```
`ownedBy`는 소속에 대응하는 강의 학과가 없으면 빈 목록을 돌려준다. 예외를 던지지 않는다.
국제자유전공학부, 융합학부, `DEFAULT`가 여기 해당한다.

`interdisciplinaryValues()`가 선언 순서를 그대로 쓰므로(`course.md`의 연계전공 목록 정렬 규칙),
상수 정리 시 연계전공 블록의 순서를 바꾸지 않는다.

### 4. Service

**`CourseService.getMajorCourses(final long memberId)`**
```java
final Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

final List<CourseDepartment> departments = CourseDepartment.ownedBy(member.getDepartment());
if (departments.isEmpty()) {
    return MajorCoursesResponse.of(List.of());
}

final List<Course> courses = courseRepository.findByDepartmentIn(departments);
```
빈 목록을 그대로 `IN`에 넘기지 않는 이유는 두 가지다. 빈 `IN` 절의 동작이 구현에 따라 갈리고,
`member.md`가 "학과가 미정인 회원은 전공 강의 조회 결과가 비어 있다"로 이미 정하고 있어
예외가 아니라 빈 결과가 되어야 한다.

**`CourseService.getOtherDepartmentCourses(final String department)`**
```java
final MemberDepartment memberDepartment = MemberDepartment.fromSelectable(department);
final List<CourseDepartment> departments = CourseDepartment.ownedBy(memberDepartment);
if (departments.isEmpty()) {
    return MajorCoursesResponse.of(List.of());
}

final List<Course> courses = courseRepository.findByDepartmentIn(departments);
```
파라미터를 `CourseDepartment`가 아니라 `MemberDepartment`로 받는다. 그러면 교양, 일선처럼 학과가 아닌 값이
애초에 목록에 없어 통과할 수 없고, 타학과를 볼 때도 그 학과 학생이 보는 것과 같은 범위가 나온다.
클래스 상단의 `TODO` 주석과 `course.md`의 "알려진 제약"을 함께 지운다.

**`MemberService.updateDepartment`** — `MemberDepartment.from`을 `fromSelectable`로 바꿔 `DEFAULT` 선택을 막는다.

**`Member.updateDepartment(final MemberDepartment department)`** — 학과와 함께 단과대학을 갱신한다.
```java
public void updateDepartment(final MemberDepartment department) {
    this.department = department;
    this.college = department.getMemberCollege();
    this.updatedAt = LocalDateTime.now();
}
```
`MemberDepartment`가 이미 자기 `MemberCollege`를 갖고 있어 두 값이 어긋날 수 있는 구조였다.
동북아국제통상물류학부는 학과 수정으로만 도달하므로, college를 함께 갱신하지 않으면 새로 추가한
`MemberCollege` 값이 DB에 영원히 들어가지 않는다. `member.md`의 "학과를 수정해도 단과대학은
함께 바뀌지 않는다"를 뒤집는 **정책 변경**이다.

### 5. DTO / Controller / 예외

- `ExceptionCode`에 과목 그룹이 아니라 회원 그룹으로 추가한다.
  `INVALID_DEPARTMENT(BAD_REQUEST, 1014, "유효하지 않은 학과입니다.")`
  학과는 회원 소속 개념이고 학과 수정과 타학과 조회 양쪽에서 쓰이므로 회원 번호대(1010~)에 붙인다.
- `DepartmentUpdateRequest`의 `@Schema` example은 그대로 `COMPUTER_ENGINEERING`을 쓴다(변경 없는 상수).
- `CourseControllerDocs`의 타학과 조회 파라미터 설명에서 값 집합이 `MemberDepartment` 이름임을 명시한다.
- 응답 DTO는 바뀌지 않는다. `InterdisciplinaryMajorResponse`는 연계전공 전용이라 영향이 없다.

### 6. 정책 문서

**`course.md`**
- 강의 조회 표의 전공 기준을 "요청한 회원의 학과에 대응하는 강의 학과 전체"로 고친다
- 타학과 제약을 "학과가 아니면 실패"로 채운다
- "전공 조회는 회원의 학과를 강의의 학과로 대응시켜 찾는다. 대응되지 않는 학과면 실패한다"를
  "대응되는 강의 학과가 없으면 빈 결과를 돌려준다"로 바꾼다
- 하단 "알려진 제약" 블록을 삭제한다
- 학부 소속 학생이 하위 전공 강의를 함께 본다는 규칙을 새로 적는다

**`member.md`**
- 회원 정보의 학과 설명에 소속 단위가 학부임을, 동북아국제통상물류학부만 전공이 소속 단위임을 적는다
- 학과 수정에서 `미정`을 선택할 수 없음을 적는다
- "학과를 수정해도 단과대학은 함께 바뀌지 않는다"를 "학과를 수정하면 단과대학도 함께 바뀐다"로 뒤집는다

## 결정 필요 (Decisions needed)

- [x] **상수명 정리 범위** — 학부, 전공 그룹 전체에 `_SCHOOL` / `_MAJOR` 접미사를 일괄 적용한다(21개 변경).
  다음 학기에 전공이 추가돼도 어느 층위인지 판단할 기준이 생긴다
- [x] **타학과 조회 파라미터** — `MemberDepartment`로 받아 하위 전공까지 함께 조회한다.
  교양, 일선처럼 학과가 아닌 값이 목록에 없어 자동으로 막히고, 알려진 제약이 함께 해소된다.
  프론트엔드가 넘기는 값 집합이 바뀌므로 공유가 필요하다
- [x] **죽은 상수 2개** — `TRADE`와 `INTERNATIONAL_DEVELOPMENT_COOPERATION`을 남긴다.
  학교가 코드를 다시 주면 `code`만 채우면 된다
- [x] **회원의 단과대학 정합성** — 이번 작업에서 함께 고친다. `updateDepartment`가 학과의 `MemberCollege`로
  단과대학을 갱신하고 `member.md`의 해당 규칙을 뒤집는다

## 검증

- **`CourseDepartmentTest`**: `ownedBy`가 학부 소속에 하위 전공을 모두 포함하는지(전자공학부 -> 4개, 도시환경공학부 -> 3개),
  소속 없는 값이 어느 목록에도 안 들어가는지, 폐지 학과가 후신 학부에 매핑되는지, `fromCode`가 상수명 변경 후에도 같은 코드로 동작하는지
- **`CourseServiceTest`**: 학부 소속 학생의 전공 조회가 학부와 하위 전공 강의를 모두 반환하는지,
  대응 강의 학과가 없는 소속(`DEFAULT`, 국제자유전공학부)의 전공 조회가 예외 없이 빈 목록인지,
  타학과 조회에 교양 값을 넘기면 `INVALID_DEPARTMENT`인지, 정렬 순서(학년, 이수구분, 학수번호)가 유지되는지
- **`MemberServiceTest`**: 학과 수정에 `DEFAULT`를 넘기면 실패하는지, 새로 추가된 학과로 수정되는지,
  학과를 바꾸면 단과대학도 함께 바뀌는지(동북아 3전공으로 바꿨을 때 college가 `NORTHEAST_ASIA_TRADE_LOGISTICS`인지)
- **`CourseSyncApplierTest` / `CourseSyncServiceTest`**: 상수명이 바뀌어도 코드 기반 매핑이 그대로 동작하는지
- **마이그레이션**: 테스트는 H2 `ddl-auto: create-drop`으로 돌고 Flyway는 prod 프로파일에서만 켜지므로
  `V1_8`은 자동 검증되지 않는다. 로컬 MySQL에 적용해 `members`, `courses`에 구 이름이 남지 않는지 직접 확인한다

## Deviation Log

- `CourseDepartment.java`: Lombok이 만드는 4인자 생성자에 더해 3인자 생성자를 명시적으로 추가해
  `this(code, courseCollege, name, null)`로 위임했다 — 이유: 소속이 없는 상수 15개(교양, 교직, 일선,
  군사학, 연계전공 11종, HUSS 2종)가 선언마다 `null`을 적지 않아도 되고, 기존 선언 형태가 그대로 유지된다
- `MemberControllerDocs.java`: 계획서 영향 범위에 없던 파일을 수정했다 — 이유: 학과 수정 API에
  `INVALID_DEPARTMENT`(1014) 400 응답이 새로 생겼는데 문서에 없으면 코드와 어긋난다
- `MemberServiceTest.java`: 기존 테스트 상수 `INVALID_DEPARTMENT`(문자열 "존재하지_않는_학과")를
  `UNKNOWN_DEPARTMENT`로 바꿨다 — 이유: 새로 추가한 `ExceptionCode.INVALID_DEPARTMENT` static import를
  같은 이름의 지역 상수가 가려 예외 코드 검증이 문자열과 비교되고 있었다
- `V1_8__realign_department_structure.sql`: 계획서는 강의 상수명 변경을 21건으로 적었으나 실제는 20건이다
  — 이유: 계획 작성 시 표의 항목 수를 잘못 셌다. 대상 상수 자체는 계획서 표와 동일하다
