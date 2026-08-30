# query-stats-summary-0 — carts

상태: 0 = 원본
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 84618건
직전 상태 대비: -

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9994 | 84568 | 0.819716 | 69321.75026 | 46.1285% | 6.1496 | 205.1944 | `CartRepository.countCartedCoursesByCourseId` | carts를 course_id 목록(IN)으로 필터해 course_id별 COUNT를 센다 |
| 2 | 0.9997 | 84595 | 0.603096 | 51018.91827 | 33.9493% | 18.4443 | 2.0000 | `CartRepository.findByMemberId` | carts를 member_id로 필터해 courses를 조인, course_schedules를 LEFT JOIN으로 함께 읽는다 |
| 3 | 1.9953 | 168837 | 0.071969 | 12151.067943 | 8.0856% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9987 | 84506 | 0.071582 | 6049.191312 | 4.0253% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9989 | 84521 | 0.069491 | 5873.489913 | 3.9084% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9989 | 84529 | 0.069383 | 5864.896197 | 3.9027% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0000 | 1 | 0.317375 | 0.317375 | 0.0002% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT `c1_0` . `course_id` , COUNT ( `c1_0` . `id` ) FROM `carts` `c1_0` WHERE `c1_0` . `course_id` IN (...) GROUP BY `c1_0` . `course_id`

[2] SELECT `c1_0` . `id` , `c1_0` . `course_id` , `c2_0` . `id` , `c2_0` . `academic_year` , `c2_0` . `area` , `c2_0` . `area_code` , `c2_0` . `area_name` , `c2_0` . `classification_code` , `c2_0` . `classification_name` , `c2_0` . `college` , `c2_0` . `concentration_code` , `c2_0` . `concentration_name` , `c2_0` . `course_code` , `c2_0` . `credits` , `c2_0` . `current_enrollment` , `c2_0` . `department` , `c2_0` . `english_code` , `c2_0` . `english_name` , `c2_0` . `grade_code` , `c2_0` . `grade_name` , `c2_0` . `haksu_code` , `c2_0` . `is_english_course` , `c2_0` . `is_huss_course` , `c2_0` . `max_capacity` , `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` , `c2_0` . `status` , `c2_0` . `term` , `c2_0` . `title_en` , `c2_0` . `title_kr` , `c2_0` . `type_code` , `c2_0` . `type_name` , `c1_0` . `created_at` ,

> 원문이 `c1_0` . `created_at` 뒤 쉼표에서 끝난다. `FROM`, `JOIN`, `WHERE`, `ORDER BY` 절이 원문에 남아 있지 않다. 말줄임표(`...`)로 끝나지는 않았다.

[3] SET `autocommit` = ?

[4] SET SESSION TRANSACTION READ ONLY

[5] COMMIT

[6] SET SESSION TRANSACTION READ WRITE

[7] SELECT @@`version_comment` LIMIT ?
