# query-stats-summary-4 — major

상태: 4 = 사이클 4 적용 후: 정적 강의 목록을 Redis에 캐싱(`major-courses` 캐시, 키 `MemberDepartment`, 기동 시 `CourseCacheWarmer`가 전 학과 적재)하고, `isRegisterable`은 매 요청 `CourseRepository.findCapacitiesByDepartmentIn`(projection `SELECT c.id, c.currentEnrollment, c.maxCapacity ... WHERE c.department IN :departments AND c.status = ACTIVE`, 정렬 없음)으로 라이브 조립. 캐시 적중 시 상태 3의 `courses` 28컬럼 쿼리(`CourseRepository.findByDepartmentIn`)와 `course_schedules` 배치 쿼리(`Course.schedules`의 `@BatchSize`)는 실행되지 않는다. 측정 구간 캐시 hit 6350 / miss 0.
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 6350건
직전 상태 대비:
- 요청당 쿼리 수: SELECT 3건(트랜잭션 제어문 포함 8.7544건) → SELECT 2건(트랜잭션 제어문 포함 6.9735건)
- 사라진 쿼리: `courses` 28컬럼 조회(`CourseRepository.findByDepartmentIn`) - calls 2566 → 0, mean_ms 22.192974 → -, total_ms 56947.172354 → -, 비중 42.0369% → -, 행/호출 1498.5078 → -, 읽은행/반환행 1.6338 → -
- 사라진 쿼리: `course_schedules` 배치 조회(`Course.schedules`의 `@BatchSize`) - calls 4491 → 0, mean_ms 16.364202 → -, total_ms 73491.63137 → -, 비중 54.2496% → -, 행/호출 2609.2532 → -, 읽은행/반환행 1.0000 → -
- 사라진 쿼리: 세션 초기화 쿼리 2종(`SET character_set_results`, `SELECT @@SESSION.transaction_read_only`, 상태 3에서 각 calls 6) - 상태 4 1차 출력에 없음
- 새로 나타난 쿼리: `CourseRepository.findCapacitiesByDepartmentIn` - calls 6349, mean_ms 9.38515, total_ms 59586.323607, 비중 85.3284%, 행/호출 1498.0433, 읽은행/반환행 1.0000
- `MemberRepository.findById`: calls 2564 → 6306, mean_ms 0.563607 → 0.405776, total_ms 1445.090682 → 2558.824969, 비중 1.0667% → 3.6643%, 행/호출 1.0008 → 1.0062, 읽은행/반환행 1.0000 → 0.9995

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9998 | 6349 | 9.38515 | 59586.323607 | 85.3284% | 1498.0433 | 1.0000 | `CourseRepository.findCapacitiesByDepartmentIn` | courses 조회 (id·current_enrollment·max_capacity만, department IN 조건, status = ACTIVE, 정렬 없음) |
| 2 | 1.9904 | 12639 | 0.230702 | 2915.842925 | 4.1755% | 0.0000 | - | - | 트랜잭션 제어 |
| 3 | 0.9931 | 6306 | 0.405776 | 2558.824969 | 3.6643% | 1.0062 | 0.9995 | `MemberRepository.findById` | 회원 단건 조회 (PK) |
| 4 | 0.9972 | 6332 | 0.277931 | 1759.864619 | 2.5201% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9964 | 6327 | 0.243474 | 1540.460784 | 2.2060% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9964 | 6327 | 0.232384 | 1470.295039 | 2.1055% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0002 | 1 | 0.1435 | 0.1435 | 0.0002% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

## 쿼리 원문

[1] SELECT `c1_0` . `id` , `c1_0` . `current_enrollment` , `c1_0` . `max_capacity` FROM `courses` `c1_0` WHERE `c1_0` . `department` IN (...) AND `c1_0` . `status` = ?

[2] SET `autocommit` = ?

[3] SELECT `m1_0` . `id` , `m1_0` . `academic_status` , `m1_0` . `college` , `m1_0` . `created_at` , `m1_0` . `department` , `m1_0` . `email` , `m1_0` . `grade` , `m1_0` . `last_semester_gpa` , `m1_0` . `name` , `m1_0` . `password` , `m1_0` . `student_id` , `m1_0` . `updated_at` FROM `members` `m1_0` WHERE `m1_0` . `id` = ?

[4] SET SESSION TRANSACTION READ ONLY

[5] COMMIT

[6] SET SESSION TRANSACTION READ WRITE

[7] SELECT @@`version_comment` LIMIT ?
