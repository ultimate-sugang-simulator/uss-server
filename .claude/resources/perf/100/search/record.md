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
| ✅ | ⏳ | ⏳ | ⏳ |

**사이클 (반복)**

| # | 기법 | 5. 설계 | 6. 스냅샷 | 7. 적용 | 8. 검증 |
|---|---|---|---|---|---|
| 1 | | ⏳ | ⏳ | ⏳ | ⏳ |

**재개 메모**: 2026-08-28 기준 측정 환경은 철거된 상태다. 중단 사유: 2,000,000행 시드 적재를 스킬이 대기하다 토큰만 소모해 중간에 끊었다. 새 구조에서는 적재를 호출자가 직접 실행하고 스킬은 명령만 제시한다. 2,000,000행 시드를 지우고 스키마를 Flyway로 재생성해 앱 시드 상태
(`courses` 2,439, `course_schedules` 7,819, `members` 0)이며 버퍼 풀은 128MiB다. Phase 2부터 다시 한다.
`../seeds.sql`은 옛 모듈이 스키마와 맞지 않던 시점에 2,000,000행 기준으로 직접 쓴 것이다. Phase 3에서 규모를 운영 대비 배수로
다시 확정하고, 변수 블록(`@course_title_mode = 'search'`)으로 다시 쓴다. 모듈 상한을 넘는 규모면 README의 대량 적재 절차를 따른다.
`../tokens.json`은 2026-08-22에 만료됐다. Phase 4에서 다시 만든다. `test-script.js`는 유효하다.

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

> 값이 바뀌면 그 사실을 사이클 기록에 남긴다. 아래는 철거 전 값이며 Phase 2, 3에서 재확정한다.

| 항목 | 값 |
|---|---|
| 프로파일 | perf (`application-perf.yml`) |
| DB | MySQL 8.0.46 / InnoDB (`uss-mysql`, `127.0.0.1:3307`, `uss_db`). `ngram_token_size=2` |
| 커넥션 풀 크기 | 10 (`minimum-idle`도 10) |
| InnoDB 버퍼 풀 크기 | 128MiB (현재). 철거 전에는 시드 약 3GB를 담기 위해 2GiB로 온라인 리사이즈했었다 |
| 데이터 규모 | 재확정 필요. 철거 전: `courses` 2,002,440 (시드 2,000,000 + 실데이터 2,440), `course_schedules` 6,007,819 |
| 규모 근거 | 미기록. 철거 전 시드는 운영(앱 시드 2,439행) 대비 약 820배였고 근거가 남아 있지 않다. Phase 3에서 배수와 이유를 확정한다 |
| 카디널리티 | `status` 1종(`ACTIVE`) - `AND c.status = 'ACTIVE'`가 거르는 행이 없다. `department` 20종, `area` 8종 균등. 검색 키워드는 접두 40 x 접미 25 = 1,000조합, 조합당 매칭 = 강의 수 / 1,000. 실데이터 선택도(`컴퓨터` 21/2,440 = 0.86%)를 그대로 확대하면 응답이 건당 6~8MB가 되어 2,000건 수준으로 낮추기로 했었다 |
| 부하 조건 | VU 30, 유지 1m (ramp-up 30s + 유지 1m + ramp-down 30s = 총 2m), USER_COUNT 50. 풀 10의 3배 |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고 애플리케이션 캐시는 없다. 매 측정 전 같은 워밍업으로 맞춘다 |
| 되돌리기 절차 | 불필요 (읽기 전용) |
| 시드 | `../seeds.sql` - Phase 3에서 변수 블록 + `course.sql`(`@course_title_mode = 'search'`)로 다시 쓴다 |
| 토큰 | `../tokens.json` (`mint-tokens.sh`, 회원 id 900001~900050). 만료됨, Phase 4에서 재발급 |

## 기준선 (Baseline)

| 지표 | 값 |
|---|---|
| p95 | |
| p99 | |
| RPS | |
| 에러율 | |
| check 통과율 | |
| 요청당 쿼리 수 | |

### 쿼리 통계 (total_ms 상위)

> 전체: `query-stats-summary-0.md` / k6 요약: `k6-test-summary-0.json`. 진단 근거로 쓴 행만 옮긴다.

| 요청당 | mean_ms | total_ms | 비중 | 읽은행/반환행 | 출처 |
|---|---|---|---|---|---|

### 진단

- 병목 성격: {Phase 4에서 확정한 판정}
- 근거: {관측된 수치}
- 예상 쿼리 목록과 어긋난 지점: {무엇이 어떻게 / `없음`}

---

## 최종 요약

| 구분 | 지표 | 최초 | 최종 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | | | |
| | p99 | | | |
| | RPS | | | |
| 하드웨어 독립 | 요청당 쿼리 수 | | | |
| | 읽은 행 / 반환 행 | | | |
| | 접근 방식과 인덱스 | | | |
| | `Handler_read_rnd_next` | | | |
| | 캐시 hit / miss, 적중률 | | | |

적용한 기법: {사이클 순서대로}

운영 반영 시 유의점: {Phase 9에서 확인한 마이그레이션 영향 / `스키마 변경 없음` / `미확인`}
