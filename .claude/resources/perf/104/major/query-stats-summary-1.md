# query-stats-summary-1 — major

상태: 1 = 사이클 1 적용 후: `courses` 복합 인덱스 추가 (`idx_department_sort`: department, grade_code, classification_code, haksu_code)
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 1965건
직전 상태 대비: 요청당 쿼리 수 SELECT 2건(트랜잭션 제어문 포함 6.9948건) → SELECT 2건(트랜잭션 제어문 포함 6.9960건), 대상 쿼리(`CourseRepository.findByDepartmentIn`) total_ms 537123.163886 → 370836.376959, mean_ms 393.78531 → 188.720802, 비중 98.4120% → 98.36059%, 읽은행/반환행 7.7942 → 1.9560, 행/호출 4562.5169 → 4563.1405

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1.0000 | 1965 | 188.720802 | 370836.376959 | 98.36059% | 4563.1405 | 1.9560 | `CourseRepository.findByDepartmentIn` | courses·course_schedules LEFT JOIN 조회 (학과 목록 IN 조건, DISTINCT) |
| 2 | 0.9995 | 1964 | 0.934903 | 1836.150946 | 0.48702% | 1.0005 | 1.0000 | `MemberRepository.findById` | 회원 단건 조회 (PK) |
| 3 | 1.9975 | 3925 | 0.391294 | 1535.830771 | 0.40736% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9990 | 1963 | 0.511611 | 1004.293074 | 0.26638% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9995 | 1964 | 0.494504 | 971.207381 | 0.25760% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 1.0000 | 1965 | 0.423945 | 833.053023 | 0.22096% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0005 | 1 | 0.313042 | 0.313042 | 0.00008% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT DISTINCTROW `c1_0` . `id` , `c1_0` . `academic_year` , `c1_0` . `area` , `c1_0` . `area_code` , `c1_0` . `area_name` , `c1_0` . `classification_code` , `c1_0` . `classification_name` , `c1_0` . `college` , `c1_0` . `concentration_code` , `c1_0` . `concentration_name` , `c1_0` . `course_code` , `c1_0` . `credits` , `c1_0` . `current_enrollment` , `c1_0` . `department` , `c1_0` . `english_code` , `c1_0` . `english_name` , `c1_0` . `grade_code` , `c1_0` . `grade_name` , `c1_0` . `haksu_code` , `c1_0` . `is_english_course` , `c1_0` . `is_huss_course` , `c1_0` . `max_capacity` , `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` , `c1_0` . `status` , `c1_0` . `term` , `c1_0` . `title_en` , `c1_0` . `title_kr` , `c1_0` . `type_code` , `c1_0` . `type_name` FROM `courses` `c1_0` LEFT JOIN `course_schedules` `s1_0` ON `c1_0` .

> 원문이 966자에서 잘렸다 (`performance_schema_max_digest_length` = 1024, `record.md` Phase 2 기록 근거). WHERE 절, ORDER BY 절이 원문에 남아 있지 않다. 상태 0과 잘리는 지점이 동일하다.

[2] SELECT `m1_0` . `id` , `m1_0` . `academic_status` , `m1_0` . `college` , `m1_0` . `created_at` , `m1_0` . `department` , `m1_0` . `email` , `m1_0` . `grade` , `m1_0` . `last_semester_gpa` , `m1_0` . `name` , `m1_0` . `password` , `m1_0` . `student_id` , `m1_0` . `updated_at` FROM `members` `m1_0` WHERE `m1_0` . `id` = ?

[3] SET `autocommit` = ?

[4] COMMIT

[5] SET SESSION TRANSACTION READ ONLY

[6] SET SESSION TRANSACTION READ WRITE

[7] SELECT @@`version_comment` LIMIT ?
