# query-stats-summary-0 — carts

상태: 0 = 원본
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 47812건
직전 상태 대비: - (이 이슈의 첫 측정. 같은 조건의 #106 기준선은 `.claude/resources/perf/106/carts/query-stats-summary-0.md`)

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9977 | 47700 | 0.945745 | 45112.076913 | 42.6024% | 6.0857 | 204.0763 | `CartRepository.countCartedCoursesByCourseId` | carts를 course_id 목록(IN)으로 필터해 course_id별 COUNT를 센다 |
| 2 | 0.9956 | 47601 | 0.712025 | 33893.110021 | 32.0076% | 18.2776 | 1.9998 | `CartRepository.findByMemberId` | carts를 member_id로 필터해 courses를 조인, course_schedules를 LEFT JOIN으로 함께 읽는다 |
| 3 | 1.9815 | 94741 | 0.113743 | 10776.178211 | 10.1767% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9945 | 47547 | 0.117246 | 5574.707927 | 5.2646% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9945 | 47551 | 0.115821 | 5507.445021 | 5.2011% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9941 | 47531 | 0.105761 | 5026.968429 | 4.7473% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0000 | 1 | 0.36 | 0.36 | 0.0003% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT `c1_0` . `course_id` , COUNT ( `c1_0` . `id` ) FROM `carts` `c1_0` WHERE `c1_0` . `course_id` IN (...) GROUP BY `c1_0` . `course_id`

[2] SELECT `c1_0` . `id` , `c1_0` . `course_id` , `c2_0` . `id` , `c2_0` . `academic_year` , `c2_0` . `area` , `c2_0` . `area_code` , `c2_0` . `area_name` , `c2_0` . `classification_code` , `c2_0` . `classification_name` , `c2_0` . `college` , `c2_0` . `concentration_code` , `c2_0` . `concentration_name` , `c2_0` . `course_code` , `c2_0` . `credits` , `c2_0` . `current_enrollment` , `c2_0` . `department` , `c2_0` . `english_code` , `c2_0` . `english_name` , `c2_0` . `grade_code` , `c2_0` . `grade_name` , `c2_0` . `haksu_code` , `c2_0` . `is_english_course` , `c2_0` . `is_huss_course` , `c2_0` . `max_capacity` , `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` , `c2_0` . `status` , `c2_0` . `term` , `c2_0` . `title_en` , `c2_0` . `title_kr` , `c2_0` . `type_code` , `c2_0` . `type_name` , `c1_0` . `created_at` ,

> 원문이 `c1_0` . `created_at` 뒤 쉼표에서 끝난다(949자). `FROM`, `JOIN`, `WHERE`, `ORDER BY` 절이 원문에 남아 있지 않다. 말줄임표(`...`)로 끝나지는 않았다. #106에서도 같은 자리에서 끝났다. `performance_schema_max_digest_length`는 4096이므로 그 상한에 걸린 것이 아니다.

[3] SET `autocommit` = ?

[4] SET SESSION TRANSACTION READ ONLY

[5] SET SESSION TRANSACTION READ WRITE

[6] COMMIT

[7] SELECT @@`version_comment` LIMIT ?
