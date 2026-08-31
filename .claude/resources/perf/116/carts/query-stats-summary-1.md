# query-stats-summary-1 — carts

상태: 1 = 사이클 1 적용 후: 담기 수 비정규화 카운터 (`courses.cart_count`) + 원자적 UPDATE
측정: VU 30 / ramp-up 30s + 유지 1m + ramp-down 30s (총 2m) / 캐시 warm / 요청 75103건
직전 상태 대비: 요청당 SQL 1.9933 → 0.9931 (집계 쿼리 소멸), 왕복 약 7 → 약 6, 대상 쿼리(`countCartedCoursesByCourseId`) total_ms 45112.076913 → 없음, 읽은행/반환행 204.0763 → 없음. 남은 조회 쿼리(`findByMemberId`)는 mean_ms 0.712025 → 0.623936, 읽은행/반환행 1.9998 → 1.9989로 사실상 동일하며 `cart_count` 컬럼이 SELECT 목록에 추가됐다

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9931 | 74585 | 0.623936 | 46536.295162 | 57.6393% | 18.5561 | 1.9989 | `CartRepository.findByMemberId` | carts를 member_id로 필터해 courses를 조인, course_schedules를 LEFT JOIN으로 함께 읽는다. 담기 수를 courses의 cart_count 컬럼에서 함께 가져온다 |
| 2 | 1.9781 | 148558 | 0.092833 | 13791.186616 | 17.0816% | 0.0000 | - | - | 트랜잭션 제어 |
| 3 | 0.9935 | 74613 | 0.093316 | 6962.623081 | 8.6238% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9931 | 74582 | 0.093019 | 6937.61381 | 8.5929% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9933 | 74602 | 0.087249 | 6508.973093 | 8.0619% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.0000 | 1 | 0.351042 | 0.351042 | 0.0004% | 1.0000 | 1.0000 | 미상 | 서버 변수(`@@version_comment`) 조회 |

사라진 쿼리: `CartRepository.countCartedCoursesByCourseId` (`-0`에서 요청당 0.9977, total_ms 45112.076913, 비중 42.60%, 읽은행/반환행 204.0763). 코드에서 제거되어 digest에 나타나지 않는다.
새로 생긴 쿼리: 없음. 쓰기 경로(`addCart`, `deleteCartedCourse`)에 추가한 카운터 UPDATE는 이번 측정이 읽기 전용이라 실행되지 않는다.

## 쿼리 원문

[1] SELECT `c1_0` . `id` , `c1_0` . `course_id` , `c2_0` . `id` , `c2_0` . `academic_year` , `c2_0` . `area` , `c2_0` . `area_code` , `c2_0` . `area_name` , `c2_0` . `cart_count` , `c2_0` . `classification_code` , `c2_0` . `classification_name` , `c2_0` . `college` , `c2_0` . `concentration_code` , `c2_0` . `concentration_name` , `c2_0` . `course_code` , `c2_0` . `credits` , `c2_0` . `current_enrollment` , `c2_0` . `department` , `c2_0` . `english_code` , `c2_0` . `english_name` , `c2_0` . `grade_code` , `c2_0` . `grade_name` , `c2_0` . `haksu_code` , `c2_0` . `is_english_course` , `c2_0` . `is_huss_course` , `c2_0` . `max_capacity` , `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` , `c2_0` . `status` , `c2_0` . `term` , `c2_0` . `title_en` , `c2_0` . `title_kr` , `c2_0` . `type_code` , `c2_0` . `type_name` ,

> 원문이 `c2_0` . `type_name` 뒤 쉼표에서 끝난다. `-0`에서는 `c1_0` . `created_at` 뒤에서 끝났다. `cart_count`가 컬럼 목록에 알파벳 순으로 끼어들면서 뒤가 그만큼 밀린 것이다. 말줄임표(`...`)로 끝나지는 않았고, `performance_schema_max_digest_length`는 4096이므로 그 상한에 걸린 것이 아니다.

[2] SET `autocommit` = ?

[3] SET SESSION TRANSACTION READ ONLY

[4] COMMIT

[5] SET SESSION TRANSACTION READ WRITE

[6] SELECT @@`version_comment` LIMIT ?
