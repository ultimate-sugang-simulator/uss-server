# query-stats-summary-2 — general-education

상태: 2 = 사이클 2 적용 후: 영역별 교양 목록 캐싱 (Redis, 실시간 정원 병합)
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s / 캐시 warm / 요청 7268건
직전 상태 대비: 요청당 쿼리 수 3.15 → 0.99. `findByArea`(28컬럼)와 `course_schedules IN` 배치가 요청 경로에서 사라지고 정원 쿼리(`findCapacitiesByArea`)로 대체됨. 대상 쿼리 total_ms 96,776 (66,208 + 30,568) → 28,155.722298. 캐시 적중 7,268/7,268 (100%), Redis GET 요청당 1.0 (mean 93.55 ms)

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9927 | 7215 | 3.902387 | 28155.722298 | 84.7828% | 1210.2304 | 1.0000 | CourseRepository.findCapacitiesByArea | `courses`에서 area, status로 필터해 id, 현재원, 정원만 읽음 (실시간 정원 병합용) |
| 2 | 1.9565 | 14220 | 0.141236 | 2008.380193 | 6.0477% | 0.0000 | - | - | 트랜잭션 제어 |
| 3 | 0.9917 | 7208 | 0.143395 | 1033.592705 | 3.1124% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9862 | 7168 | 0.142888 | 1024.228276 | 3.0842% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9856 | 7163 | 0.137799 | 987.060254 | 2.9722% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.0001 | 1 | 0.25675 | 0.25675 | 0.0008% | 1.0000 | 1.0000 | - | mysql 클라이언트 접속 시 자체 조회 (통계 수집 세션, 측정 대상 아님) |

## 쿼리 원문

[1] SELECT `c1_0` . `id` , `c1_0` . `current_enrollment` , `c1_0` . `max_capacity` FROM `courses` `c1_0` WHERE `c1_0` . `area` = ? AND `c1_0` . `status` = ?

[2] SET `autocommit` = ?

[3] SET SESSION TRANSACTION READ ONLY

[4] COMMIT

[5] SET SESSION TRANSACTION READ WRITE

[6] SELECT @@`version_comment` LIMIT ?
