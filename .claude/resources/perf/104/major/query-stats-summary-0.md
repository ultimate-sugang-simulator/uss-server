# query-stats-summary-0 — major

상태: 0 = 원본
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 1364건

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1.0000 | 1364 | 393.78531 | 537123.163886 | 98.4120% | 4562.5169 | 7.7942 | `CourseRepository.findByDepartmentIn` | courses·course_schedules LEFT JOIN 조회 (학과 목록 IN 조건, DISTINCT) |
| 2 | 1.9963 | 2723 | 1.132855 | 3084.766738 | 0.5652% | 0.0000 | - | - | 트랜잭션 제어 |
| 3 | 1.0000 | 1364 | 1.500286 | 2046.391447 | 0.3749% | 1.0000 | 1.0000 | `MemberRepository.findById` | 회원 단건 조회 (PK) |
| 4 | 1.0000 | 1364 | 1.159158 | 1581.091705 | 0.2897% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9993 | 1363 | 0.882921 | 1203.421502 | 0.2205% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9985 | 1362 | 0.551373 | 750.971342 | 0.1376% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0007 | 1 | 0.34575 | 0.34575 | 0.0001% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT DISTINCTROW `c1_0` . `id` , `c1_0` . `academic_year` , `c1_0` . `area` , `c1_0` . `area_code` , `c1_0` . `area_name` , `c1_0` . `classification_code` , `c1_0` . `classification_name` , `c1_0` . `college` , `c1_0` . `concentration_code` , `c1_0` . `concentration_name` , `c1_0` . `course_code` , `c1_0` . `credits` , `c1_0` . `current_enrollment` , `c1_0` . `department` , `c1_0` . `english_code` , `c1_0` . `english_name` , `c1_0` . `grade_code` , `c1_0` . `grade_name` , `c1_0` . `haksu_code` , `c1_0` . `is_english_course` , `c1_0` . `is_huss_course` , `c1_0` . `max_capacity` , `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` , `c1_0` . `status` , `c1_0` . `term` , `c1_0` . `title_en` , `c1_0` . `title_kr` , `c1_0` . `type_code` , `c1_0` . `type_name` FROM `courses` `c1_0` LEFT JOIN `course_schedules` `s1_0` ON `c1_0` .

> 원문이 966자에서 잘렸다 (`performance_schema_max_digest_length` = 1024, `record.md` Phase 2 기록 근거). WHERE 절, ORDER BY 절이 원문에 남아 있지 않다.

[2] SET `autocommit` = ?

[3] SELECT `m1_0` . `id` , `m1_0` . `academic_status` , `m1_0` . `college` , `m1_0` . `created_at` , `m1_0` . `department` , `m1_0` . `email` , `m1_0` . `grade` , `m1_0` . `last_semester_gpa` , `m1_0` . `name` , `m1_0` . `password` , `m1_0` . `student_id` , `m1_0` . `updated_at` FROM `members` `m1_0` WHERE `m1_0` . `id` = ?

[4] SET SESSION TRANSACTION READ WRITE

[5] SET SESSION TRANSACTION READ ONLY

[6] COMMIT

[7] SELECT @@`version_comment` LIMIT ?
