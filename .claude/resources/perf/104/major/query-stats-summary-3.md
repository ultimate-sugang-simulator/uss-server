# query-stats-summary-3 — major

상태: 3 = 사이클 3 적용 후: `CourseRepository`의 `findByDepartment`, `findByDepartmentIn`, `findByArea`, `findHussCourses`에서 `LEFT JOIN FETCH c.schedules` 제거. `Course.schedules`의 `@BatchSize(size = 1000)` 지연 로딩에 맡겨, 대상 경로가 `courses` 조회와 `course_schedules` 배치 조회 두 쿼리로 분리됐다.
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 2567건
직전 상태 대비:
- 요청당 쿼리 수: SELECT 2건(트랜잭션 제어문 포함 6.9939건) → SELECT 3건(트랜잭션 제어문 포함 8.7544건). `courses`+`course_schedules` 분리로 대상 SELECT가 1종에서 2종으로 늘었고, 상태 2에는 없던 세션 초기화 쿼리 2종(`SET character_set_results`, `SELECT @@SESSION.transaction_read_only`, 각 calls 6)이 새로 나타났다. 사라진 쿼리는 없다.
- 대상 쿼리는 상태 2까지 `CourseRepository.findByDepartmentIn`(`LEFT JOIN FETCH`) 단일 쿼리였으나, 상태 3부터 `courses`(`CourseRepository.findByDepartmentIn`)와 `course_schedules`(`Course.schedules` 배치 로딩)로 나뉘었다.
- `courses` 단독: calls 2300 → 2566, mean_ms 63.185455 → 22.192974, total_ms 145326.547837 → 56947.172354, 비중 96.9232% → 42.0369%, 읽은행/반환행 1.5351 → 1.6338, 행/호출 4570.2709 → 1498.5078
- `courses` + `course_schedules` 합: calls 2300 → 7057(=2566+4491), total_ms 145326.547837 → 130438.803724(=56947.172354+73491.63137), 비중 96.9232% → 96.2865%(=42.0369+54.2496)
- 신규 쿼리 `course_schedules` 배치: calls 4491, 요청당 1.7495, mean_ms 16.364202, total_ms 73491.63137, 비중 54.2496%, 행/호출 2609.2532, 읽은행/반환행 1.0000

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1.7495 | 4491 | 16.364202 | 73491.63137 | 54.2496% | 2609.2532 | 1.0000 | `Course.schedules`의 `@BatchSize(size = 1000)` 지연 로딩 배치 쿼리 (`Course.java:55`, `MajorCourseResponse.from` → `CourseScheduleFormatter.format` 첫 접근 시 발행, 리포지토리 메서드 아님) | course_schedules IN 배치 조회 (`course_id IN (...)`) |
| 2 | 0.9996 | 2566 | 22.192974 | 56947.172354 | 42.0369% | 1498.5078 | 1.6338 | `CourseRepository.findByDepartmentIn` | courses 조회 (`department IN` 조건, `status = ACTIVE`, `grade_code`·`classification_code`·`haksu_code` 정렬) |
| 3 | 0.9988 | 2564 | 0.563607 | 1445.090682 | 1.0667% | 1.0008 | 1.0000 | `MemberRepository.findById` | 회원 단건 조회 (PK) |
| 4 | 2.0019 | 5139 | 0.221772 | 1139.69045 | 0.8413% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 1.0000 | 2567 | 0.37875 | 972.253323 | 0.7177% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 1.0000 | 2567 | 0.335992 | 862.493586 | 0.6367% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.9996 | 2566 | 0.236854 | 607.769445 | 0.4486% | 0.0000 | - | - | 트랜잭션 제어 |
| 8 | 0.0023 | 6 | 0.288312 | 1.729875 | 0.0013% | 0.0000 | - | 미상 | 세션 문자셋 설정 (`character_set_results`) |
| 9 | 0.0023 | 6 | 0.232146 | 1.392876 | 0.0010% | 1.0000 | 1.0000 | 미상 | 세션 읽기전용 여부 조회 (`@@SESSION.transaction_read_only`) |
| 10 | 0.0004 | 1 | 0.120833 | 0.120833 | 0.0001% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` FROM `course_schedules` `s1_0` WHERE `s1_0` . `course_id` IN (...)

[2] SELECT `c1_0` . `id` , `c1_0` . `academic_year` , `c1_0` . `area` , `c1_0` . `area_code` , `c1_0` . `area_name` , `c1_0` . `classification_code` , `c1_0` . `classification_name` , `c1_0` . `college` , `c1_0` . `concentration_code` , `c1_0` . `concentration_name` , `c1_0` . `course_code` , `c1_0` . `credits` , `c1_0` . `current_enrollment` , `c1_0` . `department` , `c1_0` . `english_code` , `c1_0` . `english_name` , `c1_0` . `grade_code` , `c1_0` . `grade_name` , `c1_0` . `haksu_code` , `c1_0` . `is_english_course` , `c1_0` . `is_huss_course` , `c1_0` . `max_capacity` , `c1_0` . `status` , `c1_0` . `term` , `c1_0` . `title_en` , `c1_0` . `title_kr` , `c1_0` . `type_code` , `c1_0` . `type_name` FROM `courses` `c1_0` WHERE `c1_0` . `department` IN (...) AND `c1_0` . `status` = ? ORDER BY `c1_0` . `grade_code` , `c1_0` . `classification_code` , `c1_0` . `haksu_code`

[3] SELECT `m1_0` . `id` , `m1_0` . `academic_status` , `m1_0` . `college` , `m1_0` . `created_at` , `m1_0` . `department` , `m1_0` . `email` , `m1_0` . `grade` , `m1_0` . `last_semester_gpa` , `m1_0` . `name` , `m1_0` . `password` , `m1_0` . `student_id` , `m1_0` . `updated_at` FROM `members` `m1_0` WHERE `m1_0` . `id` = ?

[4] SET `autocommit` = ?

[5] SET SESSION TRANSACTION READ ONLY

[6] COMMIT

[7] SET SESSION TRANSACTION READ WRITE

[8] SET `character_set_results` = ?

[9] SELECT @@SESSION . `transaction_read_only`

[10] SELECT @@`version_comment` LIMIT ?
