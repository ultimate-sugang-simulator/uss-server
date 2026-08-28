# [PERF-100] GET /api/v1/courses/search

> 이슈: #100
> 브랜치: refactor/100-search-performance
> 대상 디렉토리: `.claude/resources/perf/100/search/`

이 파일은 대상 엔드포인트 하나만 다룬다. 같은 이슈의 다른 엔드포인트는 각자의 디렉토리에 각자의 `record.md`를 가진다.

## 진행 상태

> ⏳ 미완 / ✅ 완료 / ⏭️ 건너뜀. 재진입 시 ⏳로 표기된 가장 이른 Phase부터 재개한다.

**준비 (대상당 1회)**

| 1. 대상 | 2. 환경 | 3. 조건 | 4. 기준선 |
|---|---|---|---|
| ✅ | ✅ | ✅ | ✅ |

**사이클 (반복)**

| # | 기법 | 5. 설계 | 6. 스냅샷 | 7. 적용 | 8. 검증 |
|---|---|---|---|---|---|
| 1 | 없음 (병목 없음 판정으로 사이클 미진행) | ⏭️ | ⏭️ | ⏭️ | ⏭️ |

**재개 메모**: 2026-08-28 Phase 9까지 완료. 기준선에서 병목 없음으로 판정해 사이클 없이 종료했다. 코드 변경 없음.
측정 중 발견한 정렬 문제는 #101로 분리했다(검색 결과 학년 정렬 누락, 관련도 정렬 방향 미지정).
측정 후 시드 24,000행(id 1,000,001~1,024,000)과 `../tokens.json`은 지웠다. 다시 재려면 Phase 3-A 적재와 Phase 4 토큰 발급부터 한다.

## 대상

- 엔드포인트: `GET /api/v1/courses/search?keyword={keyword}`
- 실행 경로: `CourseController.searchCourses` (`CourseController.java:59`) → `CourseService.searchCourses` (`CourseService.java:108`) → `CourseRepository.findByKeyword` (`CourseRepository.java:46`)
- 인증: 화이트리스트 밖이라 `JwtAuthenticationFilter`를 탄다. 컨트롤러에 `@Auth`가 없고 필터는 DB를 보지 않으므로 회원 시드 없이 토큰만 있으면 된다.
- 예상 쿼리 목록 (요청 1회 기준, 매칭 강의 수를 n이라 할 때)
  1. `CourseRepository.findByKeyword` - 1회. 네이티브 쿼리. `MATCH(course_code, haksu_code, title_kr, title_en) AGAINST(? IN BOOLEAN MODE)`로 걸러 `status = 'ACTIVE'`를 추가 조건으로 두고, 같은 `MATCH` 식으로 `ORDER BY`한다. `SELECT DISTINCT c.*`.
  2. `Course.schedules` 지연 로딩 - `ceil(n / 1000)`회. `SearchedCourseResponse.from`이 `CourseScheduleFormatter.format(course.getSchedules())`로 컬렉션을 건드린다(`SearchedCourseResponse.java:41`). `@BatchSize(size = 1000)`(`Course.java:55`)이 걸려 있어 N회가 아니다.
- 쿼리가 붙지 않는 지점 (확인 완료)
  - `course.is75MinLesson()` (`Course.java:232`) - 2에서 초기화된 컬렉션을 재사용한다
  - `course.getDepartment().getName()` - `CourseDepartment`는 enum이다
  - `ApiPerformanceInterceptor` - `System.nanoTime()` 기반 로깅만 한다
  - `SearchedCoursesResponse.of` - 리스트 래핑만 한다

## 측정 환경

> 값이 바뀌면 그 사실을 사이클 기록에 남긴다. 2026-08-28 Phase 2에서 재확인한 값이다. 데이터 규모, 카디널리티, 부하 조건은 Phase 3에서 확정한다.

| 항목 | 값 |
|---|---|
| 프로파일 | perf (`application-perf.yml`). `application="uss-server-perf"` 확인, SQL 로깅 OFF, p6spy 미사용, `open-in-view: false` |
| DB | MySQL 8.0 / InnoDB (`uss-mysql`, `127.0.0.1:3307`, `uss_db`). `ngram_token_size=2`, utf8mb4_unicode_ci |
| 커넥션 풀 크기 | 10 (`minimum-idle`도 10) |
| InnoDB 버퍼 풀 크기 | 128MiB (2026-08-28). 철거 전에는 시드 약 3GB를 담기 위해 2GiB로 온라인 리사이즈했었다. Phase 3-A의 시드 규모가 이를 넘으면 리사이즈하고 여기에 시점을 남긴다 |
| 응답시간 히스토그램 | `http_server_requests_seconds_bucket` 75개 (SLO 100ms~5s) |
| performance_schema | ON. `statements_digest`, `events_statements_current` YES. digest 44건 적재 확인 |
| digest 길이 상한 | `max_digest_length` 1024. 대상 쿼리(`findByKeyword` 네이티브, `schedules` 배치 로딩)는 각 300자 안팎이라 잘리지 않는다. 1024를 넘는 건 Flyway 시드의 다중행 `INSERT IGNORE INTO courses`(1053자)뿐이며 Phase 4의 digest 리셋으로 측정 구간에서 사라진다. 올리지 않는다 |
| 데이터 규모 | `courses` 26,439 (앱 시드 2,439 + 성능 시드 24,000, id 1,000,001~1,024,000), `course_schedules` 79,819 (7,819 + 72,000, 시드 강의당 3.0), `members` 0. 2026-08-28 적재, 검증값 일치 |
| 규모 근거 | 운영(앱 시드 2,439행) 대비 10x. `findByKeyword`가 학년도·학기로 거르지 않으므로 학기가 ~10개 쌓인 운영 상태를 재현한다. 행당 실측(`courses` 1.19KB, `course_schedules` 0.24KB, 데이터+인덱스) 기준 시드 약 46MB로 버퍼 풀 128MiB 안에 든다. 철거 전 2,000,000행(약 820x)은 근거가 없었고 적재에 실패했다 |
| 카디널리티 | `status` 1종(`ACTIVE`) - `CourseStatus` 상수가 하나뿐이라 `AND c.status = 'ACTIVE'`가 거르는 행이 없다. `department` 20종, `area` 8종 균등(검증값 20 / 8). 검색 제목은 접두 40 x 접미 25 = 1,000조합, 조합당 매칭 = 24,000 / 1,000 = 24건(검증: `컴퓨터공학` 시드 24 / 실데이터 0). 실데이터 `컴퓨터` 21/2,440 = 0.86%와 같은 수준이다. ngram 구 검색의 DB 비용은 최종 매칭 수보다 가장 흔한 바이그램(접미 `공학` 등, 전체의 1/25 ≈ 960행)의 포스팅 리스트 크기에 먼저 비례한다 |
| 부하 조건 | VU 30, 유지 1m (ramp-up 30s + 유지 1m + ramp-down 30s = 총 2m), USER_COUNT 50, 키워드 조합어 10개 순환. 풀 10의 3배라 커넥션 대기가 섞이는 조건이며 호출자가 이를 알고 확정했다(2026-08-28). 워밍업은 VU 5, 30s |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고 애플리케이션 캐시는 없다. 매 측정 전 같은 워밍업으로 맞춘다 |
| 되돌리기 절차 | 불필요 (읽기 전용) |
| 시드 | `../seeds.sql`(변수 블록) + `course.sql`(`@course_title_mode = 'search'`). `member.sql` 미사용. 적재 직후 `fts_indexed` 24,000으로 일치했으나 호출자가 FULLTEXT 인덱스를 떼고 다시 만들어 색인은 재구축본이다. 되돌리기: `DELETE FROM courses WHERE id BETWEEN 1000001 AND 1024000` |
| 토큰 | `../tokens.json` (`mint-tokens.sh`, 회원 id 900001~900050). 만료됨, Phase 4에서 재발급 |

## 기준선 (Baseline)

| 지표 | 값 |
|---|---|
| p95 | 114.4194 ms |
| p99 | 172.74108 ms |
| med | 63.861 ms |
| RPS | 362.86 |
| 에러율 | 0% (43,545건 중 0) |
| check 통과율 | 100% (세 항목 모두 43,545 통과) |
| 요청당 쿼리 수 | 2.00 (`findByKeyword` 1.00 + `schedules` 배치 1.00) + 트랜잭션 제어문 5.0 |
| 요청당 DB 시간 | 4.6 ms (digest 총합 200,057 ms / 43,545건, 트랜잭션 제어문 포함) |
| 응답 크기 | 약 12.5 KB (543.9 MB / 43,545건, 평균 26.2건) |

보조 관측 - 경합 없는 단건 (VU 1, 30s, `k6-probe-vu1.json`): med 7.282 / p95 16.7214 / p99 22.5705 ms, 110.79 rps (평균 왕복 9.0 ms).

### 쿼리 통계 (total_ms 상위)

> 전체: `query-stats-summary-0.md` / k6 요약: `k6-test-summary-0.json`. 진단 근거로 쓴 행만 옮긴다.

| 요청당 | mean_ms | total_ms | 비중 | 읽은행/반환행 | 출처 |
|---|---|---|---|---|---|
| 0.9999 | 2.439837 | 106235.42 | 53.10% | 2.0 (26.2행/호출) | `CourseRepository.findByKeyword` |
| 0.9999 | 1.645839 | 71658.19 | 35.82% | 1.0 (77.2행/호출) | `Course.schedules` 배치 로딩 (`@BatchSize(1000)`) |
| 4.9955 (4건 합) | 0.10 | 22163.81 | 11.08% | - | 트랜잭션 제어 (`SET autocommit` ×2, `COMMIT`, `SET SESSION TRANSACTION READ ONLY` / `READ WRITE`) |

### 진단

- 병목 성격: 없음. DB 쪽은 요청당 쿼리 2건(N+1 없음), `examined_per_sent` 2.0 / 1.0(읽고 버리는 행 없음), 합계 4.6 ms. 앱 쪽은 경합 없는 단건 7.3 ms로 DB 4.6 ms와의 차이가 3~4 ms에 그친다.
- 근거: VU 30에서 평균 왕복 82.7 ms는 단건 9.0 ms의 9배인데 처리량은 3.3배(110.8 → 362.9 rps)만 늘었다. 늘어난 74 ms는 요청 하나의 처리가 아니라 포화 대기다. 커넥션 대기는 아니다(풀 점유율 362.9 rps × 4.6 ms ≈ 17%). k6, JVM, MySQL(Docker VM)이 같은 8코어를 나눠 쓴 환경이라 포화 지점이 어느 프로세스 때문인지는 이 자료에 없다.
- 예상 쿼리 목록과 어긋난 지점: 대상 쿼리 두 건은 예상(1회 / ⌈n/1000⌉ = 1회)과 일치. 예상 목록에 없던 것은 요청당 5건의 트랜잭션 제어문(Spring이 `@Transactional(readOnly = true)` 경계에서 보내는 것)으로 합계 11.1%, 0.5 ms.
- 행/호출 26.2는 시드 24건 + 실데이터 매칭. 키워드 10개 중 일부(`화학실험`, `기계설계` 등)가 실제 강의 제목에도 걸린다.

### 측정 중 발견한 사항 (성능 외)

- `findByKeyword`의 `ORDER BY MATCH ... AGAINST`에 방향이 없어 관련도 오름차순(낮은 순)으로 나간다. 정책(`service-policy/course.md` 강의 검색 절 "관련도가 높은 순")과 반대다. 검색 결과에는 다른 조회와 달리 학년 정렬도 없다. → #101로 분리.
- 시드 모듈 `course.sql`의 `grade_code`는 `'01'`~`'05'`(전학년 = `'05'`)인데 실데이터는 `'0'`(전학년)~`'4'`다. 검색은 이 컬럼을 보지 않아 이번 측정에는 영향이 없다. 학년 정렬을 재는 대상이 생기면 모듈을 실데이터 형식에 맞춘다.

---

## 최종 요약

| 구분 | 지표 | 최초 | 최종 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 114.4194 ms | 114.4194 ms | 없음 (동일 상태 0) |
| | p99 | 172.74108 ms | 172.74108 ms | 없음 |
| | RPS | 362.86 | 362.86 | 없음 |
| 하드웨어 독립 | 요청당 쿼리 수 | 2.00 (+ 트랜잭션 제어 5.0) | 2.00 (+ 5.0) | 없음 |
| | 읽은 행 / 반환 행 | 2.0 / 1.0 | 2.0 / 1.0 | 없음 |
| | 접근 방식과 인덱스 | FULLTEXT `ft_idx_course_search` (ngram) + `idx_course_id` (실행계획 미캡처, Phase 6 미진행) | 동일 | 없음 |
| | `Handler_read_rnd_next` | 미측정 (Phase 6 미진행) | - | - |
| | 캐시 hit / miss, 적중률 | 해당 없음 (애플리케이션 캐시 없음) | - | - |

적용한 기법: 없음. 기준선에서 병목 없음으로 판정하고 종료했다.

운영 반영 시 유의점: 스키마 변경 없음. 코드 변경 없음.

측정 조건 요약: 운영 대비 10x(`courses` 26,439), VU 30 / 유지 1m / 캐시 warm, 키워드당 매칭 약 26건. 이 규모에서 요청당 DB 4.6 ms, 경합 없는 단건 7.3 ms.
