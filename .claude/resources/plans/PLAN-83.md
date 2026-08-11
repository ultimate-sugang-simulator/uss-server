# [PLAN-83] 백오피스 API와 강의 동기화 구현

> 이슈: #83
> 브랜치: feat/83-backoffice-api

## 목표

관리자 인증, 표시 학기 설정, 학교 연계 API 기반 강의 동기화를 서버 기능으로 만든다. 지금은 관리자 계정이라는 개념이 없고 강의 데이터가 Flyway 시드로만 들어가서, 학기가 바뀌면 사람이 로컬에서 SQL을 다시 만들어 배포해야 한다.

## 사전 확인으로 드러난 사항

**1. 연계 API는 개발 로컬에서 호출할 수 없다.** 2026-08-11 실호출 결과 `{"respMsg":"Unauthorized IP","respCode":"401"}`를 받았다. IP 검사가 키 검사보다 앞서므로 키 유효성도 로컬에서는 확인이 안 된다. 따라서 **호출부를 인터페이스로 분리하고 테스트는 가짜 구현으로 돌린다.** 실호출 검증은 운영 배포 후에만 가능하다. 메서드는 POST다(명세 §3의 GET 예시는 표기 오류, 사용자 확인).

**2. 응답이 래핑돼 있다.** `{result, resultMsg, totalRecordCount, totalpageSize, pageRecordCount, page, data[]}` 형태다. `.local`의 기존 덤프는 `data`만 뽑아둔 것이라 배열로 보였다. 다만 2026-08-04 실측에서 `totalpageSize`가 `null`로 왔고 전체가 1페이지에 담겨 왔으며 `PAGE=2`는 JSON이 아닌 HTML 리다이렉트를 반환했다. **값이 있으면 그만큼 반복하고 없으면 1페이지로 끝내는 방어가 필요하다.**

**3. `MOD_DATE`는 공통 필수다.** 이름은 수정일자지만 없으면 호출이 실패한다. 증분 커서로 쓰라는 의도로 보이나 백오피스 명세가 전량 수집을 요구하므로 고정 하한값(`20260101`)을 프로퍼티로 둔다.

**4. `Course`에 정적 팩토리가 없다.** 지금까지 강의는 시드 SQL로만 들어오고 테스트는 `ReflectionTestUtils`로 필드를 채워서, 코드로 `Course`를 만드는 경로가 아예 없다. 동기화가 첫 사례라 생성과 갱신 메서드를 새로 만든다.

**5. enum의 `fromCode`가 전부 예외를 던진다.** `CourseCollege`, `CourseDepartment`, `CourseArea`, `CourseDay`, `CourseTerm` 다섯 개가 미등록 코드에 `INVALID_ENUM_TYPE`을 던진다. 동기화 경로는 예외 대신 경고로 넘겨야 하므로 `Optional`을 반환하는 짝을 만든다.

**6. `CourseSchedule` 전용 Repository가 없다.** 시간표 건수 조회와 일괄 삭제가 필요해 신설한다.

**7. 페이지네이션 전례가 없다.** 레포 전체에 `Pageable`, `Page` 사용처가 하나도 없다. 공통 래퍼를 새로 만든다.

## 영향 범위

### 신규 파일

**admin 도메인**

- `admin/domain/Admin.java` - 관리자 계정 Entity
- `admin/domain/AdminRole.java` - `ADMIN`
- `admin/domain/SemesterSetting.java` - 표시 학기 Entity (항상 1행)
- `admin/domain/CourseSyncJob.java` - 동기화 작업 Entity
- `admin/domain/CourseSyncDetail.java` - 변경 항목 Entity
- `admin/domain/CourseSyncChangedField.java` - 변경 필드 Entity
- `admin/domain/SyncStrategy.java` - `INITIAL`, `UPSERT`, `REPLACE`
- `admin/domain/SyncJobStatus.java` - `RUNNING`, `SUCCESS`, `FAILED`
- `admin/domain/SyncPhase.java` - `COURSE_FETCH`, `TIMETABLE_FETCH`, `PERSIST`
- `admin/domain/SyncChangeType.java` - `CREATED`, `UPDATED`, `CLOSED`, `WARNING`
- `admin/repository/AdminRepository.java`
- `admin/repository/SemesterSettingRepository.java`
- `admin/repository/CourseSyncJobRepository.java`
- `admin/repository/CourseSyncDetailRepository.java`
- `admin/service/AdminAuthService.java`
- `admin/service/SemesterSettingService.java`
- `admin/service/AdminCourseService.java`
- `admin/service/CourseSyncService.java` - preflight, Job 생성, 이력 조회
- `admin/service/CourseSyncJobService.java` - Job 상태 전이 전용 (짧은 트랜잭션)
- `admin/infra/AdminPasswordEncoder.java` - jbcrypt 래퍼
- `admin/infra/CourseSyncExecutor.java` - `@Async` 실행 오케스트레이션
- `admin/infra/CourseSyncApplier.java` - 전략별 적재
- `admin/infra/InuCourseApiClient.java` - 연계 API 호출 인터페이스
- `admin/infra/InuCourseApiRestClient.java` - `RestClient` 구현체
- `admin/infra/InuCourseApiProperties.java` - `@ConfigurationProperties`
- `admin/controller/AdminAuthController.java` + `AdminAuthControllerDocs.java`
- `admin/controller/AdminSemesterController.java` + `AdminSemesterControllerDocs.java`
- `admin/controller/AdminCourseController.java` + `AdminCourseControllerDocs.java`
- `admin/controller/AdminSyncController.java` + `AdminSyncControllerDocs.java`
- `admin/event/CourseSyncJobCreatedEvent.java` - Job 생성 커밋 후 실행 트리거

**admin DTO**

- request: `AdminLoginRequest`, `SemesterDisplayRequest`, `SyncPreflightRequest`, `SyncJobCreateRequest`
- response: `AdminTokenResponse`, `SemesterDisplayResponse`, `CourseSummaryResponse`, `SyncPreflightResponse`, `SyncJobCreatedResponse`, `SyncJobResponse`, `SyncJobDetailResponse`, `SyncChangeResponse`
- common: `SemesterRef`, `SyncDeleteCounts`, `LastJobInfo`, `ChangedFieldInfo`, `InuCourseResponse`, `InuTimetableResponse`, `InuApiResponse<T>`, `SyncResult`

**공통**

- `global/dto/response/PageResponse.java` - 페이지네이션 래퍼
- `global/http/AdminEndpoint.java` - 관리자 경로 판별과 관리자 인증 예외 경로
- `auth/annotation/AdminAuth.java`
- `auth/resolver/AdminAuthArgumentResolver.java`
- `auth/filter/AdminAuthenticationFilter.java`

**course 도메인**

- `course/domain/CourseStatus.java` - `ACTIVE`, `CLOSED`
- `course/domain/CourseSnapshot.java` - 연계 API가 소유하는 값 묶음 (생성, 갱신 입력)
- `course/domain/CourseFieldChange.java` - 변경 필드 (field, before, after)
- `course/repository/CourseScheduleRepository.java`

**Flyway**

- `database/migration/V1_5__add_admin_and_course_sync.sql`
- `database/seed/V1_6__insert_semester_setting.sql`
- `database/seed/V1_7__insert_local_admin.sql`

**문서**

- `BACKOFFICE-API-DEVIATIONS.md` (레포 루트) - 내부 규약과 어긋나는 프론트엔드 요구사항
- `.claude/spec/service-policy/admin.md` - 관리자 인증, 표시 학기, 동기화 정책

### 수정 파일

- `src/main/java/uss/code/course/domain/Course.java` - `status` 필드, `create`, `applyUpdate`, `close`, `reopen`, `isActive`
- `src/main/java/uss/code/course/domain/CourseSchedule.java` - `create` 정적 팩토리
- `src/main/java/uss/code/course/domain/CourseCollege.java` - `tryFromCode` 추가, `fromCode`가 이를 위임
- `src/main/java/uss/code/course/domain/CourseDepartment.java` - 동일
- `src/main/java/uss/code/course/domain/CourseArea.java` - 동일
- `src/main/java/uss/code/course/domain/CourseDay.java` - 동일
- `src/main/java/uss/code/course/domain/CourseTerm.java` - 동일
- `src/main/java/uss/code/course/repository/CourseRepository.java` - 조회 4종에 `status = ACTIVE`, 동기화용 조회 추가
- `src/main/java/uss/code/cart/service/CartService.java` - 폐강 검증
- `src/main/java/uss/code/registration/service/RegistrationService.java` - 폐강 검증
- `src/main/java/uss/code/cart/repository/CartRepository.java` - 학기별 건수
- `src/main/java/uss/code/registration/repository/RegistrationRepository.java` - 학기별 건수
- `src/main/java/uss/code/auth/dto/request/LoginRequest.java` - 학번 정규식 완화
- `src/main/java/uss/code/auth/infra/JwtProvider.java` - 관리자 토큰 발급과 검증
- `src/main/java/uss/code/auth/filter/JwtAuthenticationFilter.java` - 관리자 경로 제외, 관리자 토큰 거부
- `src/main/java/uss/code/auth/filter/JwtExceptionFilter.java` - `RestApiException` 상태코드 반영
- `src/main/java/uss/code/global/config/FilterChainConfig.java` - 관리자 필터 등록
- `src/main/java/uss/code/global/config/ArgumentResolverConfig.java` - 관리자 리졸버 등록
- `src/main/java/uss/code/global/exception/domain/ExceptionCode.java` - 5000번대와 `COURSE_CLOSED`
- `src/main/java/uss/code/global/exception/handler/GlobalExceptionHandler.java` - 본문 enum 오류 처리
- `src/main/resources/application-prod.yml` - 연계 API, 관리자 토큰 만료 설정
- `src/test/resources/application.yml` - 동일 키의 테스트값
- `.github/workflows/cd-prod.yml` - 시크릿 2개 주입
- `.claude/spec/secret-convention.md` - 시크릿 표 갱신
- `.claude/spec/service-policy/README.md` - `admin.md` 등록
- `.claude/spec/service-policy/course.md` - 폐강 정책, 데이터 적재 경로
- `.claude/spec/service-policy/auth.md` - 관리자 토큰, 인증 예외 경로
- `.claude/spec/service-policy/cart.md` - 폐강 검증
- `.claude/spec/service-policy/registration.md` - 폐강 검증

## 구현 계획

### 1. Entity / Flyway

**`V1_5__add_admin_and_course_sync.sql`**

```sql
-- 관리자 계정. API로 만들지 않는다. 시드 또는 DB 직접 등록.
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_login_id (login_id)
) ENGINE=InnoDB;

-- 표시 학기. 프론트 노출용 라벨이며 courses 데이터와 무관하다. 항상 1행이다.
CREATE TABLE IF NOT EXISTS semester_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year INT NOT NULL,
    term VARCHAR(50) NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB;

-- 동기화 작업 이력. 진행 중 작업은 전체에서 하나만 존재한다.
-- 건수 컬럼은 SUCCESS가 아니면 NULL이다. 화면이 '-'로 표시한다.
CREATE TABLE IF NOT EXISTS course_sync_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    academic_year INT NOT NULL,
    term VARCHAR(50) NOT NULL,
    strategy VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    phase VARCHAR(50) NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    fetched_course_count INT NULL,
    fetched_schedule_count INT NULL,
    created_count INT NULL,
    updated_count INT NULL,
    closed_count INT NULL,
    warning_count INT NULL,
    partially_applied BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason TEXT NULL,
    FOREIGN KEY (admin_id) REFERENCES admins(id),
    INDEX idx_started_at (started_at),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- 작업별 변경 항목. 완료 후 불변이라 페이지 오프셋이 밀리지 않는다.
CREATE TABLE IF NOT EXISTS course_sync_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    haksu_code VARCHAR(15) NOT NULL,
    course_name VARCHAR(255) NULL,
    reason VARCHAR(255) NULL,
    FOREIGN KEY (job_id) REFERENCES course_sync_jobs(id) ON DELETE CASCADE,
    INDEX idx_job_change_haksu (job_id, change_type, haksu_code)
) ENGINE=InnoDB;

-- 수정 항목의 필드별 변경 전후. before, after는 MySQL 키워드라 접미사를 붙인다.
CREATE TABLE IF NOT EXISTS course_sync_changed_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    detail_id BIGINT NOT NULL,
    field VARCHAR(50) NOT NULL,
    before_value VARCHAR(500) NOT NULL,
    after_value VARCHAR(500) NOT NULL,
    FOREIGN KEY (detail_id) REFERENCES course_sync_details(id) ON DELETE CASCADE,
    INDEX idx_detail_id (detail_id)
) ENGINE=InnoDB;

-- 폐강은 물리 삭제하지 않는다. carts, registrations가 course_id를 참조한다.
ALTER TABLE courses ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- 학생 조회 4종이 status로 먼저 걸러지므로 기존 정렬 인덱스 앞에 status를 둔다.
ALTER TABLE courses DROP INDEX idx_department_sort;
ALTER TABLE courses DROP INDEX idx_area_sort;
ALTER TABLE courses DROP INDEX idx_huss_sort;
ALTER TABLE courses ADD INDEX idx_department_sort (status, department, grade_code, classification_code, haksu_code);
ALTER TABLE courses ADD INDEX idx_area_sort (status, area, grade_code, classification_code, haksu_code);
ALTER TABLE courses ADD INDEX idx_huss_sort (status, is_huss_course, grade_code, classification_code, haksu_code);
```

시드 두 개를 나눠 넣는다. 표시 학기 1행은 운영에도 필요하고, 관리자 계정은 로컬 개발용 하나만 넣는다.

```sql
-- V1_6__insert_semester_setting.sql
INSERT INTO semester_settings (academic_year, term, updated_at) VALUES (2026, 'SECOND', NOW());

-- V1_7__insert_local_admin.sql
-- 로컬 개발용 계정이다. 운영 계정은 이 시드로 만들지 않고 DB에 직접 등록한다.
-- 레포가 public이므로 운영 비밀번호 해시를 여기에 넣지 마라.
INSERT INTO admins (login_id, password, name, role, created_at) VALUES ('local-admin', '{BCrypt 해시}', '로컬관리자', 'ADMIN', NOW());
```

**`Admin`**

```java
@Getter @Entity @NoArgsConstructor @Table(name = "admins")
public class Admin {
    Long id; String loginId; String password; String name;
    @Enumerated(STRING) AdminRole role; LocalDateTime createdAt;

    public static Admin create(final String loginId, final String encodedPassword, final String name);
    public boolean isAdmin();
}
```

비밀번호 대조는 Entity가 아니라 `AdminPasswordEncoder`가 한다. BCrypt는 도메인 규칙이 아니라 구현 수단이다.

**`SemesterSetting`**

```java
Long id; int academicYear; @Enumerated(STRING) CourseTerm term; LocalDateTime updatedAt;

public static SemesterSetting create(final int academicYear, final CourseTerm term);
public void change(final int academicYear, final CourseTerm term);
```

**`CourseSyncJob`**

```java
Long id;
@ManyToOne(fetch = LAZY) @JoinColumn(name = "admin_id") Admin executedBy;
int academicYear; @Enumerated(STRING) CourseTerm term;
@Enumerated(STRING) SyncStrategy strategy;
@Enumerated(STRING) SyncJobStatus status;
@Enumerated(STRING) SyncPhase phase;
LocalDateTime startedAt, finishedAt;
Integer fetchedCourseCount, fetchedScheduleCount;
Integer createdCount, updatedCount, closedCount, warningCount;
boolean partiallyApplied; String failureReason;

public static CourseSyncJob start(Admin executedBy, int academicYear, CourseTerm term, SyncStrategy strategy);
public void changePhase(final SyncPhase phase);
public void markFetched(final int courseCount, final int scheduleCount);
public void succeed(final SyncResult result);
public void fail(final String reason, final boolean partiallyApplied);
public boolean isRunning();
public Long durationSeconds();   // finishedAt == null이면 null
```

`start`는 `status = RUNNING`, `phase = COURSE_FETCH`, `startedAt = now`로 시작한다. `succeed`는 `phase`를 `null`로 되돌리고 건수 4종과 `finishedAt`을 채운다. `fail`은 건수를 `null`로 둔 채 사유만 남긴다.

**`CourseSyncDetail`**

```java
Long id;
@ManyToOne(fetch = LAZY) CourseSyncJob job;
@Enumerated(STRING) SyncChangeType changeType;
String haksuCode; String courseName; String reason;
@OneToMany(mappedBy = "detail", cascade = PERSIST, orphanRemoval = true) @BatchSize(size = 100)
List<CourseSyncChangedField> changedFields = new ArrayList<>();

public static CourseSyncDetail created(job, haksuCode, courseName);
public static CourseSyncDetail updated(job, haksuCode, courseName, List<CourseFieldChange> changes);
public static CourseSyncDetail closed(job, haksuCode, courseName);
public static CourseSyncDetail warning(job, haksuCode, courseName, reason);
```

목록 조회가 페이지 단위라 `changedFields`는 fetch join 대신 `@BatchSize`로 끌어온다. 페이지네이션과 컬렉션 fetch join을 같이 쓰면 Hibernate가 전량을 메모리로 올린다.

**`Course` (수정)**

```java
@Enumerated(STRING) @Column(nullable = false, name = "status")
private CourseStatus status;

public static Course create(final CourseSnapshot snapshot, final int maxCapacity);
public List<CourseFieldChange> applyUpdate(final CourseSnapshot snapshot);
public void replaceSchedules(final List<CourseSchedule> schedules);
public void close();
public void reopen();
public boolean isActive();
```

`applyUpdate`는 연계 API가 소유하는 값만 비교해 바뀐 것만 반영하고 변경 목록을 돌려준다. **`maxCapacity`와 `currentEnrollment`는 비교 대상이 아니다.** 학교 API에 없는 서비스 소유 값이다.

`replaceSchedules`는 기존 컬렉션을 비우고 새로 채운다. 시간표는 필드 단위로 비교하지 않고 과목 단위로 통째 교체한다.

**`CourseSnapshot`** (record, `course/domain/`)

연계 API가 주는 값만 담는다. 필드는 `academicYear`, `term`, `titleKr`, `titleEn`, `courseCode`, `haksuCode`, `college`, `department`, `classificationCode`, `classificationName`, `area`, `areaCode`, `areaName`, `typeCode`, `typeName`, `gradeCode`, `gradeName`, `concentrationCode`, `concentrationName`, `credits`, `isEnglishCourse`, `englishCode`, `englishName`, `isHussCourse` 24개다.

DTO가 아니라 도메인 값 객체로 둔다. `Course.create`와 `applyUpdate`의 입력이고 admin 패키지가 이걸 조립해 넘긴다. `course/dto/`에 두면 도메인이 DTO에 의존하게 된다.

**`CourseFieldChange`** (record, `course/domain/`) - `field`, `before`, `after` 세 문자열.

**enum 5종에 `tryFromCode` 추가**

```java
public static Optional<CourseCollege> tryFromCode(final String code) {
    return Arrays.stream(values())
            .filter(college -> !college.getCode().isBlank())
            .filter(college -> college.getCode().equals(code))
            .findFirst();
}

public static CourseCollege fromCode(final String code) {
    return tryFromCode(code).orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
}
```

`CourseCollege`, `CourseDepartment`, `CourseArea`, `CourseDay`, `CourseTerm` 다섯 개에 같은 형태로 넣는다. 기존 호출부 동작은 그대로다.

### 2. Repository

**`CourseRepository`** - 학생 조회 4종에 상태 조건을 넣는다.

```java
// findByDepartment, findByArea, findHussCourses: WHERE 절에 AND c.status = 'ACTIVE' 추가
// findByKeyword(native): WHERE MATCH(...) AND c.status = 'ACTIVE'
```

`findByKeyword`의 `MATCH` 컬럼 목록은 건드리지 않는다. 목록이 FULLTEXT 인덱스와 어긋나면 MySQL이 인덱스를 쓰지 못한다.

동기화, 요약용으로 추가한다.

```java
@Query("SELECT COUNT(c) FROM Course c WHERE c.academicYear = :academicYear AND c.term = :term")
long countBySemester(@Param("academicYear") final int academicYear, @Param("term") final CourseTerm term);

@Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.schedules WHERE c.academicYear = :academicYear AND c.term = :term")
List<Course> findAllBySemesterWithSchedules(...);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("DELETE FROM Course c WHERE c.academicYear = :academicYear AND c.term = :term")
void deleteBySemester(...);
```

벌크 삭제는 DB의 `ON DELETE CASCADE`가 `course_schedules`, `carts`, `registrations`를 함께 지운다. JPA 캐스케이드에 기대지 않으므로 영속성 컨텍스트를 비워야 한다.

**`CourseScheduleRepository`** (신규)

```java
@Query("SELECT COUNT(s) FROM CourseSchedule s WHERE s.course.academicYear = :academicYear AND s.course.term = :term")
long countBySemester(...);

long count();   // JpaRepository 기본. 요약 조회용
```

**`CartRepository`, `RegistrationRepository`**

```java
@Query("SELECT COUNT(x) FROM Cart x WHERE x.course.academicYear = :academicYear AND x.course.term = :term")
long countBySemester(...);
```

**`AdminRepository`**

```java
Optional<Admin> findByLoginId(final String loginId);
```

**`SemesterSettingRepository`**

```java
@Query("SELECT s FROM SemesterSetting s ORDER BY s.id")
List<SemesterSetting> findAllOrdered();   // 서비스가 첫 행을 쓴다. 없으면 5100
```

**`CourseSyncJobRepository`**

```java
Optional<CourseSyncJob> findFirstByStatus(final SyncJobStatus status);
Optional<CourseSyncJob> findFirstByOrderByStartedAtDesc();
Page<CourseSyncJob> findAllByOrderByStartedAtDesc(final Pageable pageable);

@Query("SELECT j FROM CourseSyncJob j JOIN FETCH j.executedBy WHERE j.id = :id")
Optional<CourseSyncJob> findByIdWithAdmin(@Param("id") final long id);
```

**`CourseSyncDetailRepository`**

```java
Page<CourseSyncDetail> findByJobIdAndChangeTypeOrderByHaksuCodeAsc(
        final long jobId, final SyncChangeType changeType, final Pageable pageable);
```

### 3. 인증

**`JwtProvider`** - 관리자용 발급과 검증을 추가한다.

```java
private static final String ROLE_CLAIM = "role";

private final long adminAccessTokenExpirationTime;   // @Value("${security.jwt.admin-access-token-expiration-time}")

public String generateAdminToken(final long adminId);          // role = ADMIN, 만료 2시간
public void validateAdminToken(final String accessToken);      // 누락, 만료, 형식, 서명 검증 후 role 확인
public Long getAdminId(final String accessToken);
public Long getAdminIdAllowingExpiration(final String accessToken);   // 재발급용
public boolean isAdminToken(final String accessToken);         // 학생 필터가 교차 사용을 거르는 데 쓴다
```

`validateAdminToken`은 서명, 만료를 먼저 보고(`1000`~`1004`) role이 `ADMIN`이 아니면 `RestApiException(ADMIN_ACCESS_DENIED)`를 던진다. 403이므로 401 전용인 `JwtAuthenticationException`을 쓰지 않는다.

`isAdminToken`은 만료 토큰도 claim을 읽어야 하므로 `ExpiredJwtException`에서 `e.getClaims()`를 쓴다.

**`AdminEndpoint`** (`global/http/`)

```java
public final class AdminEndpoint {
    private static final String ADMIN_BASE_PATH = "/api/v1/admin";
    private static final List<EndPoint> ADMIN_WHITELIST = List.of(
            new EndPoint("/api/v1/admin/auth/login", POST),
            new EndPoint("/api/v1/admin/auth/refresh", POST)
    );

    public static boolean isAdminPath(final String uri);
    public static boolean isWhitelisted(final String uri, final String method);
}
```

관리자 경로를 `WhitelistEndpoint`에 넣지 않는다. 그쪽 목록은 정책상 "인증 없이 열린 경로"라서, 관리자 토큰이 필요한 경로를 섞으면 문서와 코드가 어긋난다.

**`JwtAuthenticationFilter`** (수정)

```java
// shouldNotFilter: WhitelistEndpoint.isWhitelisted(...) || AdminEndpoint.isAdminPath(uri)
// doFilterInternal: validateToken 후
if (jwtProvider.isAdminToken(accessToken))
    throw new JwtTokenInvalidException(INVALID_ACCESS_TOKEN);
```

관리자 토큰의 subject는 `admins.id`다. 거르지 않으면 학생 API가 그 값을 회원 식별자로 읽어 남의 데이터를 내준다.

**`AdminAuthenticationFilter`** (신규, order 4)

```java
// shouldNotFilter: !AdminEndpoint.isAdminPath(uri) || AdminEndpoint.isWhitelisted(uri, method)
// doFilterInternal: validateAdminToken 후 request.setAttribute("admin-id", adminId)
```

**`JwtExceptionFilter`** (수정) - `RestApiException`도 잡아 `exceptionCode.getStatus()`로 응답한다. 필터에서 던진 403이 상태코드를 잃지 않게 한다. 컨트롤러가 던진 예외는 `@RestControllerAdvice`가 먼저 응답으로 바꾸므로 여기까지 오지 않는다.

**`@AdminAuth` + `AdminAuthArgumentResolver`** - `admin-id` 속성을 읽어 주입한다. `ArgumentResolverConfig`에 등록한다.

### 4. Service

**`AdminAuthService`**

```java
@Transactional(readOnly = true)
public AdminTokenResponse login(final AdminLoginRequest request) {
    final Admin admin = adminRepository.findByLoginId(request.loginId())
            .orElseThrow(() -> new RestApiException(ADMIN_LOGIN_FAILED));

    if (!passwordEncoder.matches(request.password(), admin.getPassword()))
        throw new RestApiException(ADMIN_LOGIN_FAILED);

    return AdminTokenResponse.of(jwtProvider.generateAdminToken(admin.getId()), admin.getName());
}

@Transactional(readOnly = true)
public AdminTokenResponse reIssue(final String accessToken) {
    final Long adminId = jwtProvider.getAdminIdAllowingExpiration(accessToken);   // 서명만 검증

    if (!jwtProvider.isAdminToken(accessToken))
        throw new RestApiException(ADMIN_ACCESS_DENIED);

    final Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new RestApiException(ADMIN_NOT_FOUND));

    return AdminTokenResponse.of(jwtProvider.generateAdminToken(admin.getId()), admin.getName());
}
```

아이디 없음과 비밀번호 불일치를 같은 코드, 같은 메시지로 응답한다. 계정 존재 여부를 드러내지 않는다.

**`SemesterSettingService`**

```java
@Transactional(readOnly = true) public SemesterDisplayResponse getDisplaySemester();
@Transactional              public SemesterDisplayResponse changeDisplaySemester(final SemesterDisplayRequest request);
```

행이 없으면 `SEMESTER_SETTING_NOT_FOUND`. 새로 만들지 않는다. 시드가 1행을 보장한다.

**`AdminCourseService`**

```java
@Transactional(readOnly = true)
public CourseSummaryResponse getSummary() {
    // semester: courseRepository.findTerms() 첫 건, 없으면 null
    // courseCount: courseRepository.count()   (CLOSED 포함)
    // scheduleCount: courseScheduleRepository.count()
    // lastJob: courseSyncJobRepository.findFirstByOrderByStartedAtDesc()
    // runningJobId: courseSyncJobRepository.findFirstByStatus(RUNNING)
}
```

**`CourseSyncService`**

```java
@Transactional(readOnly = true)
public SyncPreflightResponse preflight(final SyncPreflightRequest request) {
    final Optional<CourseTermInfo> loaded = findLoadedSemester();
    final SyncStrategy strategy = judgeStrategy(loaded, request.academicYear(), request.term());

    // REPLACE일 때만 삭제 예정 건수 4종을 센다. 나머지는 전부 0
    return SyncPreflightResponse.of(strategy, loaded, request, deleteCounts);
}

@Transactional
public SyncJobCreatedResponse createJob(final long adminId, final SyncJobCreateRequest request) {
    if (courseSyncJobRepository.findFirstByStatus(RUNNING).isPresent())
        throw new RestApiException(SYNC_JOB_ALREADY_RUNNING);

    final SyncStrategy strategy = judgeStrategy(findLoadedSemester(), request.academicYear(), request.term());

    if (strategy != request.expectedStrategy())
        throw new RestApiException(SYNC_STRATEGY_MISMATCH);

    final Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new RestApiException(ADMIN_NOT_FOUND));

    final CourseSyncJob job = courseSyncJobRepository.save(
            CourseSyncJob.start(admin, request.academicYear(), request.term(), strategy));

    eventPublisher.publishEvent(new CourseSyncJobCreatedEvent(job.getId()));

    return SyncJobCreatedResponse.of(job.getId());
}

@Transactional(readOnly = true) public PageResponse<SyncJobResponse> getJobs(final int page);
@Transactional(readOnly = true) public SyncJobDetailResponse getJob(final long jobId);
@Transactional(readOnly = true) public PageResponse<SyncChangeResponse> getJobDetails(final long jobId, final SyncChangeType changeType, final int page);
```

전략 판정은 한 곳에 둔다.

```java
private SyncStrategy judgeStrategy(final Optional<CourseTermInfo> loaded, final int academicYear, final CourseTerm term) {
    if (loaded.isEmpty()) return INITIAL;
    if (loaded.get().matches(academicYear, term)) return UPSERT;
    return REPLACE;
}
```

`preflight`는 캐시하지 않는다. 매 호출마다 센다.

**`CourseSyncJobService`** - 상태 전이만 담당한다. 각 메서드가 짧은 트랜잭션이라 폴링이 진행 상황을 바로 본다.

```java
@Transactional public void changePhase(final long jobId, final SyncPhase phase);
@Transactional public void markFetched(final long jobId, final int courseCount, final int scheduleCount);
@Transactional public void succeed(final long jobId, final SyncResult result);
@Transactional public void fail(final long jobId, final String reason, final boolean partiallyApplied);
```

실행자가 트랜잭션 안에 있으면 갱신이 끝까지 커밋되지 않아 폴링이 `COURSE_FETCH`에 머문다. 그래서 실행자를 트랜잭션 밖에 두고 이 서비스를 호출한다.

### 5. 동기화 실행

**`InuCourseApiClient`** (인터페이스)

```java
List<InuCourseResponse> fetchCourses(final int academicYear, final CourseTerm term);
List<InuTimetableResponse> fetchTimetables(final int academicYear, final CourseTerm term);
```

**`InuCourseApiRestClient`** (구현체)

`RestClient`로 POST 호출한다. `AUTH_KEY`는 헤더, 나머지는 쿼리 파라미터다.

```
POST {base-url}/A_MAP_COURSE_INFO?PAGE=1&MOD_DATE={mod-date}&YEAR={year}&TERM_CODE={termCode}
Header: AUTH_KEY: {auth-key}
```

- `totalpageSize`가 `null`이거나 1 이하면 1페이지로 끝낸다. 값이 있으면 그만큼 `PAGE`를 올려 이어 받는다
- `result`가 `success`가 아니면 `resultMsg`를 담아 예외를 던진다
- 응답 본문이 JSON이 아니면(HTML 리다이렉트) 그 페이지에서 수집을 멈춘다. `PAGE=2`가 HTML을 반환한 전례가 있다
- 타임아웃은 연결 5초, 읽기 60초. 재시도하지 않는다. 429가 있는 API다

**`InuCourseApiProperties`**

```yaml
inu:
  course-api:
    base-url: ${INU_API_BASE_URL}
    auth-key: ${INU_API_AUTH_KEY}
    mod-date: "20260101"
```

값은 GitHub Secrets로 주입한다. **레포가 public이므로 어떤 값도 커밋하지 않는다.** 테스트 설정에는 더미값을 넣는다.

**`CourseSyncExecutor`** (`@Component`, 트랜잭션 없음)

```java
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void execute(final CourseSyncJobCreatedEvent event) {
    final long jobId = event.jobId();
    try {
        final List<InuCourseResponse> courses = apiClient.fetchCourses(year, term);

        jobService.changePhase(jobId, TIMETABLE_FETCH);
        final List<InuTimetableResponse> timetables = apiClient.fetchTimetables(year, term);

        jobService.markFetched(jobId, courses.size(), timetables.size());
        jobService.changePhase(jobId, PERSIST);

        final SyncResult result = applier.apply(jobId, courses, timetables);

        jobService.succeed(jobId, result);
    } catch (final Exception e) {
        jobService.fail(jobId, e.getMessage(), applier.isPartiallyApplied());
    }
}
```

Job 생성 트랜잭션이 커밋된 뒤에 실행해야 한다. `@Async`만 붙이면 실행 스레드가 아직 커밋되지 않은 Job 행을 못 찾는다. `@TransactionalEventListener(AFTER_COMMIT)`이 그 순서를 보장한다.

**`CourseSyncApplier`** (`@Component`, `@Transactional`)

```java
public SyncResult apply(final long jobId, final List<InuCourseResponse> courses, final List<InuTimetableResponse> timetables);
```

1. 시간표를 학수번호로 묶는다. **강좌 목록에 없는 학수번호의 시간표는 버린다.** 붙일 강의가 없다(실측 13.6%).
2. 강좌를 하나씩 `CourseSnapshot`으로 옮긴다. enum `tryFromCode`가 비면 그 과목을 건너뛰고 `WARNING`을 남긴다.
   - 사유 문구는 `"미등록 {항목} 코드: {값}"` 형태다 (예: `"미등록 학과 코드: 0000999"`)
   - 경고가 있어도 Job은 `SUCCESS`로 끝난다
3. 전략별로 갈린다.

| 전략 | 처리 |
|---|---|
| `INITIAL` | 전량 생성. `CREATED` 기록. `closedCount = 0` |
| `REPLACE` | 기존 학기 데이터를 벌크 삭제한 뒤 전량 생성. `CREATED` 기록. `closedCount = 0` |
| `UPSERT` | 학수번호로 대조. 없으면 생성, 있으면 변경분만 반영, 수집되지 않은 `ACTIVE` 강의는 폐강 |

4. `UPSERT`의 변경 감지
   - `course.applyUpdate(snapshot)`이 돌려준 필드 목록
   - 시간표는 `CourseScheduleFormatter.format`으로 만든 문자열을 비교해 다르면 통째 교체하고 `schedule` 한 필드로 기록한다
   - 폐강됐던 강의가 다시 수집되면 `reopen()` 하고 `status` 필드 변경으로 기록한다
   - 변경이 하나도 없으면 아무것도 기록하지 않는다. 건수에도 넣지 않는다
5. `SyncResult(createdCount, updatedCount, closedCount, warningCount)`를 돌려준다.

`maxCapacity`는 신규 생성 시에만 정한다(결정 필요 2번). 기존 강의의 정원과 현재 수강인원은 어떤 전략에서도 건드리지 않는다.

### 6. DTO

**`PageResponse<T>`** (`global/dto/response/`)

```java
public record PageResponse<T>(int page, int totalPages, boolean hasNextPage, List<T> content) {
    public static <T> PageResponse<T> of(final Page<?> page, final List<T> content);
}
```

`page`는 1부터다. Spring Data의 0-based를 `+1` 해서 내보낸다. 목록 컴포넌트명이 `content`라 레포의 `{단수형}Responses` 규칙에서 벗어난다. 프론트엔드 명세에 고정된 이름이라 그대로 두고 `BACKOFFICE-API-DEVIATIONS.md`에 남긴다.

**주요 응답**

| DTO | 필드 |
|---|---|
| `AdminTokenResponse` | `accessToken`, `name` |
| `SemesterDisplayResponse` | `academicYear`, `term` |
| `CourseSummaryResponse` | `semester`(nullable), `courseCount`, `scheduleCount`, `lastJob`(nullable), `runningJobId`(nullable) |
| `SyncPreflightResponse` | `strategy`, `currentSemester`(nullable), `targetSemester`, `deleteCounts` |
| `SyncJobCreatedResponse` | `jobId` |
| `SyncJobResponse` | `jobId`, `academicYear`, `term`, `strategy`, `status`, `startedAt`, 건수 3종(nullable) |
| `SyncJobDetailResponse` | 위 + `executedBy`, `finishedAt`, `durationSeconds`, `fetchedCourseCount`, `fetchedScheduleCount`, `warningCount`, `progress`(nullable), `partiallyApplied`, `failureReason` |
| `SyncChangeResponse` | `haksuCode`, `courseName`, `changedFields`(nullable), `reason`(nullable) |

건수 4종은 `SUCCESS`가 아니면 `null`이다. `progress`는 `RUNNING`일 때만 `{phase}`를 담고 그 외에는 `null`이다. **백분율, 건수 같은 정량 수치를 넣지 않는다.**

`changedFields`는 `changeType = UPDATED`일 때만 채우고 나머지는 `null`, `reason`은 `WARNING`일 때만 채운다.

### 7. Controller

전부 `/api/v1/admin` 아래에 둔다.

| Controller | Method | Path | 상태 |
|---|---|---|---|
| `AdminAuthController` | POST | `/auth/login` | 200 |
| | POST | `/auth/refresh` | 200 |
| `AdminSemesterController` | GET | `/semesters/display` | 200 |
| | PUT | `/semesters/display` | 200 |
| `AdminCourseController` | GET | `/courses/summary` | 200 |
| `AdminSyncController` | POST | `/sync/preflight` | 200 |
| | POST | `/sync/jobs` | **202** |
| | GET | `/sync/jobs` | 200 |
| | GET | `/sync/jobs/{jobId}` | 200 |
| | GET | `/sync/jobs/{jobId}/details` | 200 |

- 관리자 식별자는 `@AdminAuth final long adminId`로 받는다. Job 생성만 쓴다
- `page`는 `@RequestParam(defaultValue = "1") @Min(1)`. 위반은 `7777`
- `changeType`은 필수 `@RequestParam` + `@EnumValidation(target = SyncChangeType.class)`. 누락과 오값 모두 `7777`
- 202는 `ResponseEntity.status(ACCEPTED).body(...)`

### 8. 예외

**`ExceptionCode` 추가**

```java
// 과목
COURSE_CLOSED(BAD_REQUEST, 2003, "폐강된 과목입니다."),

// 백오피스 관리자
ADMIN_LOGIN_FAILED(UNAUTHORIZED, 5000, "아이디나 비밀번호가 맞지 않아요."),
ADMIN_NOT_FOUND(NOT_FOUND, 5001, "관리자를 찾을 수 없어요."),
ADMIN_ACCESS_DENIED(FORBIDDEN, 5002, "관리자 권한이 없어요."),

// 표시 학기
SEMESTER_SETTING_NOT_FOUND(NOT_FOUND, 5100, "표시 학기 설정을 찾을 수 없어요."),

// 강의 동기화
SYNC_JOB_ALREADY_RUNNING(CONFLICT, 5200, "이미 업데이트가 진행 중이에요."),
SYNC_STRATEGY_MISMATCH(CONFLICT, 5201, "데이터가 변경됐어요. 다시 확인해주세요."),
SYNC_JOB_NOT_FOUND(NOT_FOUND, 5202, "업데이트 작업을 찾을 수 없어요.");
```

`2003`은 명세에 없는 신설 코드다. 폐강 강의를 담거나 신청할 때 쓴다. 프론트엔드에 알려야 한다.

**`GlobalExceptionHandler` 추가**

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(final HttpMessageNotReadableException e)
```

원인이 enum 변환 실패면 `INVALID_ENUM_TYPE`(8888), 아니면 `INVALID_REQUEST_PARAMETER`(7777)를 응답한다. 지금은 둘 다 `9999`로 떨어진다. Spring Boot 4는 Jackson 3을 쓰므로 예외 타입이 `tools.jackson.databind.exc.InvalidFormatException`이다. 구현 시 실제 타입을 확인한다.

### 9. 학생 API 반영

- `CourseRepository` 조회 4종에 `status = ACTIVE`
- `CartService.addCart` - 조회한 강의가 `isActive()`가 아니면 `COURSE_CLOSED`
- `RegistrationService.registerCourse` - 정원 검증 앞에 같은 검증을 넣는다. 검증 순서가 정책이라 `registration.md`에 반영한다
- 취소와 삭제는 막지 않는다. 폐강된 강의를 빼는 것까지 막을 이유가 없다
- `LoginRequest.studentId` - `@Pattern(regexp = "^[A-Za-z0-9]{1,20}$", message = "학번은 20자 이하의 영문자 또는 숫자여야 합니다.")`

### 10. 설정

```yaml
security:
  jwt:
    admin-access-token-expiration-time: 7200000   # 2시간

inu:
  course-api:
    base-url: ${INU_API_BASE_URL}
    auth-key: ${INU_API_AUTH_KEY}
    mod-date: "20260101"
```

`cd-prod.yml`에 `INU_API_BASE_URL`, `INU_API_AUTH_KEY` 주입 단계를 넣고 `secret-convention.md`의 시크릿 표에 두 줄을 더한다. 접두사는 붙이지 않는다(환경이 하나뿐이다).

## 결정 필요 (Decisions needed)

- [x] **신규 강의의 수강 정원 기본값** - **100을 유지한다.** 기존 시드 전체와 `course.md` 정책이 100이라 40으로 바꾸면 동기화로 생긴 강의만 정원이 달라져 한 화면에 두 기준이 섞인다. 명세의 40은 프론트엔드에 정정을 요청하고 `BACKOFFICE-API-DEVIATIONS.md`에 남긴다
- [x] **`admin` 단일 패키지** - **단일 패키지로 간다.** 인증, 학기, 동기화 셋 다 관리자 토큰으로만 접근하는 한 덩어리라 패키지 이름에 권한 경계를 드러낸다
- [x] **폐강 후 재개설 기록** - **`status` 식별자를 추가한다.** `UPDATED`로 잡고 `changedFields`에 `status`(`CLOSED` -> `ACTIVE`)를 넣는다. 명세 목록에 없는 식별자이므로 프론트엔드에 알리고 문서에 남긴다

## 검증

전부 `@IntegrationTest`(H2)로 작성한다. 연계 API는 로컬에서 호출할 수 없으므로 `InuCourseApiClient`의 가짜 구현을 테스트 설정에 등록해 주입한다.

| 테스트 클래스 | 시나리오 |
|---|---|
| `AdminAuthServiceTest` | 로그인 성공, 없는 아이디와 틀린 비밀번호가 같은 코드(`5000`)로 실패, 재발급 성공, 만료 토큰 재발급 성공, 관리자 아닌 토큰 재발급 실패(`5002`), 없는 관리자(`5001`) |
| `SemesterSettingServiceTest` | 조회 성공, 변경 후 값 반영, 설정 없음(`5100`) |
| `AdminCourseServiceTest` | 강의 없을 때 `semester`가 null, 폐강 포함 건수, 최근 Job과 진행 중 Job 식별 |
| `CourseSyncServiceTest` | 전략 판정 3종, `REPLACE`의 삭제 예정 건수 4종, 진행 중 Job 존재 시 생성 실패(`5200`), 전략 불일치(`5201`), 없는 Job 조회(`5202`), 이력 정렬과 페이지 경계 |
| `CourseSyncApplierTest` | `INITIAL` 전량 생성, `REPLACE` 삭제 후 재적재, `UPSERT`의 생성, 수정, 폐강 분류, 변경 없는 강의는 기록하지 않음, enum 미매핑이 `WARNING`으로 남고 Job은 성공, 강좌 없는 시간표 폐기, 정원과 현재 수강인원 불변 |
| `CourseServiceTest` | 폐강 강의가 조회 4종에서 빠진다 (기존 클래스에 추가) |
| `CartServiceTest`, `RegistrationServiceTest` | 폐강 강의 담기와 신청이 `2003`으로 실패, 취소와 삭제는 성공 (기존 클래스에 추가) |
| `AuthServiceTest` | 영문자 섞인 20자 학번 로그인, 21자 거부 (기존 클래스에 추가) |
| `JwtProviderTest` | 관리자 토큰의 role claim, 학생 토큰과의 교차 사용 거부 |
| `CourseTest` | `applyUpdate`가 바뀐 필드만 돌려준다, 정원 필드는 비교 대상이 아니다 (기존 클래스에 추가) |

기존 테스트 중 `CourseFixture`가 만드는 강의에 `status`가 없어 전부 손봐야 한다. 픽스처 기본값을 `ACTIVE`로 두면 기존 호출부는 그대로 통과한다.

## Deviation Log

- `admin/infra/InuCourseApiRestClient`: `RestClient.Builder` 주입을 걷어내고 `RestClient.builder()`로 직접 만든다 - 이유: Spring Boot 4의 테스트 컨텍스트에 `RestClient.Builder` 빈이 없어 통합 테스트 155개가 전부 컨텍스트 로드에 실패했다. 타임아웃(연결 5초, 읽기 60초)은 `SimpleClientHttpRequestFactory`로 직접 건다
- `course/repository/CourseRepository.deleteBySemester` 외 3개: DB의 `ON DELETE CASCADE`에 기대지 않고 수강신청, 장바구니, 시간표를 순서대로 먼저 지운다 - 이유: H2 테스트는 Hibernate가 스키마를 만드는데 생성된 FK에 `ON DELETE CASCADE`가 없다. 캐스케이드에 기대면 운영에서만 동작하고 테스트에서는 FK 위반이 난다
- `admin/infra/CourseSyncMapper` 신설: 계획은 매핑을 `CourseSyncApplier` 안에 두었으나 별도 클래스로 분리했다 - 이유: 코드값 변환, 강의실 축약, 학년 명칭 가공이 적재 로직과 성격이 다르고, 합치면 한 클래스가 300줄을 넘는다
- `course/domain/CourseSnapshot`: 정적 팩토리 대신 public `@Builder`를 쓴다 - 이유: 필드가 24개라 정적 팩토리 파라미터 목록이 읽을 수 없는 크기가 된다. Entity도 Response DTO도 아닌 입력 값 객체다
- `admin/infra/CourseSyncExecutor`: `partiallyApplied`를 applier에 묻지 않고 실행자의 지역 변수로 판단한다 - 이유: applier는 요청마다 공유되는 빈이라 상태를 들고 있으면 동시 실행에서 값이 섞인다. 적재 트랜잭션이 커밋된 뒤 실패했는지는 호출한 쪽이 안다
- `UssServerApplication`: `@ConfigurationPropertiesScan` 추가 - 이유: `InuCourseApiProperties`를 record로 두려면 스캔이 필요하다
- `admin/infra/InuCourseApiException`, `CourseMappingException` 신설 - 이유: 수집 실패는 Job의 `failureReason`으로, 매핑 실패는 경고로 갈라져야 하는데 둘 다 `RestApiException`으로 던지면 구분할 수 없다
- `changedFields`에 `concentration` 추가 - 이유: 집중이수제는 학교 API가 주는 값이라 바뀌면 기록돼야 하는데 명세 목록에 빠져 있었다. `BACKOFFICE-API-DEVIATIONS.md`에 남겼다
- 테스트 픽스처 수정: `CourseFixture`에 `status` 기본값(`ACTIVE`)을, `AuthServiceTest`에 관리자 토큰 만료 인자를 넣었다 - 이유: 필드와 생성자 시그니처가 바뀌어 기존 테스트가 컴파일되지 않는다
- `CourseSyncApplier.apply()`: REPLACE의 기존 학기 삭제를 Job 로드보다 앞으로 옮기고 Job을 다시 읽는다 - 이유: 벌크 삭제가 영속성 컨텍스트를 비워서 먼저 읽어둔 Job이 준영속이 된다
- `SemesterSetting` -> `SystemSemester`로 개명하고 표시 학기 경로에서 `/display`를 뺐다 (사용자 지시) - 리포지토리, 서비스, 테이블(`system_semesters`), 픽스처, 에러코드 상수(`SYSTEM_SEMESTER_NOT_FOUND`), DTO(`SystemSemesterRequest`, `SystemSemesterResponse`)까지 이름을 맞췄다. 클라이언트 호출 경로가 바뀌므로 `BACKOFFICE-API-DEVIATIONS.md` 2-0에 남겼다
