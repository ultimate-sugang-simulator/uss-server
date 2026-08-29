# query-stats-summary-2 — major

상태: 2 = 사이클 2 적용 후: JPQL `SELECT DISTINCT c` → `SELECT c`로 변경 (SQL `DISTINCTROW` 제거). `LEFT JOIN FETCH c.schedules`를 쓰는 CourseRepository 5개 메서드(19, 29, 39, 90, 111행)에 적용
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 2302건
직전 상태 대비: 요청당 쿼리 수 SELECT 2건(트랜잭션 제어문 포함 6.9960건) → SELECT 2건(트랜잭션 제어문 포함 6.9939건), 대상 쿼리(`CourseRepository.findByDepartmentIn`) total_ms 370836.376959 → 145326.547837, mean_ms 188.720802 → 63.185455, 비중 98.36059% → 96.9232%, 읽은행/반환행 1.9560 → 1.5351, 행/호출 4563.1405 → 4570.2709

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9991 | 2300 | 63.185455 | 145326.547837 | 96.9232% | 4570.2709 | 1.5351 | `CourseRepository.findByDepartmentIn` | courses·course_schedules LEFT JOIN 조회 (학과 목록 IN 조건) |
| 2 | 0.9991 | 2300 | 0.601701 | 1383.912398 | 0.9230% | 1.0004 | 1.0000 | `MemberRepository.findById` | 회원 단건 조회 (PK) |
| 3 | 1.9970 | 4597 | 0.289935 | 1332.835783 | 0.8889% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9996 | 2301 | 0.313599 | 721.592672 | 0.4813% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9991 | 2300 | 0.265438 | 610.507871 | 0.4072% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9996 | 2301 | 0.244882 | 563.473554 | 0.3758% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0004 | 1 | 0.981875 | 0.981875 | 0.0007% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT `c1_0` . `id` , `c1_0` . `academic_year` , `c1_0` . `area` , `c1_0` . `area_code` , `c1_0` . `area_name` , `c1_0` . `classification_code` , `c1_0` . `classification_name` , `c1_0` . `college` , `c1_0` . `concentration_code` , `c1_0` . `concentration_name` , `c1_0` . `course_code` , `c1_0` . `credits` , `c1_0` . `current_enrollment` , `c1_0` . `department` , `c1_0` . `english_code` , `c1_0` . `english_name` , `c1_0` . `grade_code` , `c1_0` . `grade_name` , `c1_0` . `haksu_code` , `c1_0` . `is_english_course` , `c1_0` . `is_huss_course` , `c1_0` . `max_capacity` , `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` , `c1_0` . `status` , `c1_0` . `term` , `c1_0` . `title_en` , `c1_0` . `title_kr` , `c1_0` . `type_code` , `c1_0` . `type_name` FROM `courses` `c1_0` LEFT JOIN `course_schedules` `s1_0` ON `c1_0` . `id`

> 원문이 잘렸다 (`performance_schema_max_digest_length`, record.md Phase 2 기록 근거 현재 1024). `WHERE` 절, `ORDER BY` 절이 원문에 남아 있지 않다. 상태 1의 원문(`SELECT DISTINCTROW c1_0 ...`, 966자에서 잘림)은 `ON `c1_0` .` 뒤 마침표에서 끊겼는데, 이번 원문은 `DISTINCTROW` 제거분(12자)만큼 뒤로 밀려 `` `id` `` 까지 담겼다. 그래도 조인 조건(`= s1_0.course_id`) 이후는 여전히 없다.

[2] SELECT `m1_0` . `id` , `m1_0` . `academic_status` , `m1_0` . `college` , `m1_0` . `created_at` , `m1_0` . `department` , `m1_0` . `email` , `m1_0` . `grade` , `m1_0` . `last_semester_gpa` , `m1_0` . `name` , `m1_0` . `password` , `m1_0` . `student_id` , `m1_0` . `updated_at` FROM `members` `m1_0` WHERE `m1_0` . `id` = ?

[3] SET `autocommit` = ?

[4] COMMIT

[5] SET SESSION TRANSACTION READ WRITE

[6] SET SESSION TRANSACTION READ ONLY

[7] SELECT @@`version_comment` LIMIT ?
