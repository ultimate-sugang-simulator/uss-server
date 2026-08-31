# query-stats-summary-1 — general-education

상태: 1 = 사이클 1 적용 후: `courses` 복합 인덱스 `idx_area_sort`
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s / 캐시 warm / 요청 3097건
측정 이력: 상태 1의 1차 측정은 GC 이상으로 폐기(`*-1-discarded.*`), 이 파일은 앱 재기동 후 재측정본이다
직전 상태 대비: 요청당 쿼리 수 3.15 → 3.15 (변화 없음), findByArea mean_ms 125.865056 → 9.902062, total_ms 252485.302684 → 30567.667058 (비중 80.3422% → 30.5407%), 읽은행/반환행 22.9427 → 1.0000. schedules 배치가 비중 1위로 올라섬 (18.5061% → 66.1500%, mean_ms 13.456219 → 9.929267)

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 2.1531 | 6668 | 9.929267 | 66208.354871 | 66.1500% | 1658.5586 | 1.0000 | Course.schedules (@BatchSize 지연 로딩) | `course_schedules`에서 강의 id 목록(IN)으로 시간표 행을 읽음 |
| 2 | 0.9968 | 3087 | 9.902062 | 30567.667058 | 30.5407% | 1206.8899 | 1.0000 | CourseRepository.findByArea | `courses`에서 area, status로 필터해 전 컬럼을 읽고 grade_code, classification_code, haksu_code로 정렬 |
| 3 | 1.9793 | 6130 | 0.20086 | 1231.272698 | 1.2302% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9923 | 3073 | 0.233057 | 716.184511 | 0.7156% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9948 | 3081 | 0.229925 | 708.400883 | 0.7078% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9932 | 3076 | 0.213311 | 656.146674 | 0.6556% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0003 | 1 | 0.130416 | 0.130416 | 0.0001% | 1.0000 | 1.0000 | - | mysql 클라이언트 접속 시 자체 조회 (통계 수집 세션, 측정 대상 아님) |

## 쿼리 원문

[1] SELECT `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` FROM `course_schedules` `s1_0` WHERE `s1_0` . `course_id` IN (...)

[2] SELECT `c1_0` . `id` , `c1_0` . `academic_year` , `c1_0` . `area` , `c1_0` . `area_code` , `c1_0` . `area_name` , `c1_0` . `classification_code` , `c1_0` . `classification_name` , `c1_0` . `college` , `c1_0` . `concentration_code` , `c1_0` . `concentration_name` , `c1_0` . `course_code` , `c1_0` . `credits` , `c1_0` . `current_enrollment` , `c1_0` . `department` , `c1_0` . `english_code` , `c1_0` . `english_name` , `c1_0` . `grade_code` , `c1_0` . `grade_name` , `c1_0` . `haksu_code` , `c1_0` . `is_english_course` , `c1_0` . `is_huss_course` , `c1_0` . `max_capacity` , `c1_0` . `status` , `c1_0` . `term` , `c1_0` . `title_en` , `c1_0` . `title_kr` , `c1_0` . `type_code` , `c1_0` . `type_name` FROM `courses` `c1_0` WHERE `c1_0` . `area` = ? AND `c1_0` . `status` = ? ORDER BY `c1_0` . `grade_code` , `c1_0` . `classification_code` , `c1_0` . `haksu_code`

[3] SET `autocommit` = ?

[4] SET SESSION TRANSACTION READ ONLY

[5] COMMIT

[6] SET SESSION TRANSACTION READ WRITE

[7] SELECT @@`version_comment` LIMIT ?
