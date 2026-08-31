# query-stats-summary-0 — general-education

상태: 0 = 원본
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s / 캐시 warm / 요청 2006건
직전 상태 대비: 해당 없음 (원본)

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1.0000 | 2006 | 125.865056 | 252485.302684 | 80.3422% | 1204.9133 | 22.9427 | CourseRepository.findByArea | `courses`에서 area, status로 필터해 전 컬럼을 읽고 grade_code, classification_code, haksu_code로 정렬 |
| 2 | 2.1545 | 4322 | 13.456219 | 58157.781673 | 18.5061% | 1660.0731 | 1.0000 | Course.schedules (@BatchSize 지연 로딩) | `course_schedules`에서 강의 id 목록(IN)으로 시간표 행을 읽음 |
| 3 | 1.9850 | 3982 | 0.38644 | 1538.804401 | 0.4897% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9960 | 1998 | 0.374225 | 747.702349 | 0.2379% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9940 | 1994 | 0.370457 | 738.691349 | 0.2351% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9945 | 1995 | 0.297634 | 593.780344 | 0.1889% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0005 | 1 | 0.299417 | 0.299417 | 0.0001% | 1.0000 | 1.0000 | - | mysql 클라이언트 접속 시 자체 조회 (통계 수집 세션, 측정 대상 아님) |

## 쿼리 원문

[1] SELECT `c1_0` . `id` , `c1_0` . `academic_year` , `c1_0` . `area` , `c1_0` . `area_code` , `c1_0` . `area_name` , `c1_0` . `classification_code` , `c1_0` . `classification_name` , `c1_0` . `college` , `c1_0` . `concentration_code` , `c1_0` . `concentration_name` , `c1_0` . `course_code` , `c1_0` . `credits` , `c1_0` . `current_enrollment` , `c1_0` . `department` , `c1_0` . `english_code` , `c1_0` . `english_name` , `c1_0` . `grade_code` , `c1_0` . `grade_name` , `c1_0` . `haksu_code` , `c1_0` . `is_english_course` , `c1_0` . `is_huss_course` , `c1_0` . `max_capacity` , `c1_0` . `status` , `c1_0` . `term` , `c1_0` . `title_en` , `c1_0` . `title_kr` , `c1_0` . `type_code` , `c1_0` . `type_name` FROM `courses` `c1_0` WHERE `c1_0` . `area` = ? AND `c1_0` . `status` = ? ORDER BY `c1_0` . `grade_code` , `c1_0` . `classification_code` , `c1_0` . `haksu_code`

[2] SELECT `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` FROM `course_schedules` `s1_0` WHERE `s1_0` . `course_id` IN (...)

[3] SET `autocommit` = ?

[4] SET SESSION TRANSACTION READ WRITE

[5] COMMIT

[6] SET SESSION TRANSACTION READ ONLY

[7] SELECT @@`version_comment` LIMIT ?
