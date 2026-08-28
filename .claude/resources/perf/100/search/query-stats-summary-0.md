# query-stats-summary-0 — search

상태: 0 = 원본
측정: VU 30 / 2m / 캐시 warm / 요청 43545건

| # | 요청당 | calls | mean_ms | total_ms | 비중 | 행/호출 | 읽은행/반환행 | 출처 | 하는 일 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.9999 | 43542 | 2.439837 | 106235.424569 | 53.1024% | 26.2010 | 2.0000 | CourseRepository.findByKeyword | courses 테이블에서 FULLTEXT 인덱스(course_code, haksu_code, title_kr, title_en)로 MATCH ... AGAINST 검색, status = 'ACTIVE' 조건을 추가하고 같은 MATCH 식으로 정렬해 반환 |
| 2 | 0.9999 | 43539 | 1.645839 | 71658.193432 | 35.8188% | 77.2111 | 1.0000 | Course.schedules 지연 로딩 (SearchedCourseResponse.from에서 course.getSchedules() 호출로 트리거, @BatchSize(1000)) | course_schedules 테이블에서 course_id IN (...) 배치로 강의 시간표를 조회 |
| 3 | 1.9972 | 86966 | 0.103698 | 9018.201303 | 4.5078% | 0.0000 | - | - | 트랜잭션 제어 |
| 4 | 0.9996 | 43527 | 0.103635 | 4510.92689 | 2.2548% | 0.0000 | - | - | 트랜잭션 제어 |
| 5 | 0.9994 | 43521 | 0.10111 | 4400.438936 | 2.1996% | 0.0000 | - | - | 트랜잭션 제어 |
| 6 | 0.9993 | 43513 | 0.097309 | 4234.245109 | 2.1165% | 0.0000 | - | - | 트랜잭션 제어 |
| 7 | 0.0000 | 1 | 0.283292 | 0.283292 | 0.0001% | 1.0000 | 1.0000 | 미상 | 서버 버전 코멘트(@@version_comment) 조회 |

## 쿼리 원문

[1] SELECT DISTINCTROW `c` . * FROM `courses` `c` WHERE MATCH ( `c` . `course_code` , `c` . `haksu_code` , `c` . `title_kr` , `c` . `title_en` ) AGAINST ( ? IN BOOLEAN MODE ) AND `c` . `status` = ? ORDER BY MATCH ( `c` . `course_code` , `c` . `haksu_code` , `c` . `title_kr` , `c` . `title_en` ) AGAINST ( ? IN BOOLEAN MODE )

[2] SELECT `s1_0` . `course_id` , `s1_0` . `id` , `s1_0` . `classroom` , `s1_0` . `day_of_week` , `s1_0` . `end_time` , `s1_0` . `period_code` , `s1_0` . `period_name` , `s1_0` . `start_time` FROM `course_schedules` `s1_0` WHERE `s1_0` . `course_id` IN (...)

[3] SET `autocommit` = ?

[4] COMMIT

[5] SET SESSION TRANSACTION READ ONLY

[6] SET SESSION TRANSACTION READ WRITE

[7] SELECT @@`version_comment` LIMIT ?
