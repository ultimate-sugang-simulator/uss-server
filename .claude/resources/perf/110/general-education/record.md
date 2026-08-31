# [PERF-110] GET /api/v1/courses/general-education

> 이슈: #110
> 브랜치: analysis/110-general-education-perf
> 대상 디렉토리: `.claude/resources/perf/110/general-education/`

이 파일은 대상 엔드포인트 하나만 다룬다. 같은 이슈의 다른 엔드포인트는 각자의 디렉토리에 각자의 `record.md`를 가진다.

## 진행 상태

> ⏳ 미완 / ✅ 완료 / ⏭️ 건너뜀. 재진입 시 ⏳로 표기된 가장 이른 Phase부터 재개한다.

**준비 (대상당 1회)**

| 1. 대상 | 2. 환경 | 3. 조건 | 4. 기준선 |
|---|---|---|---|
| ✅ | ✅ | ✅ (3-A ⏭️) | ✅ |

**사이클 (반복)**

| # | 기법 | 5. 설계 | 6. 스냅샷 | 7. 적용 | 8. 검증 |
|---|---|---|---|---|---|
| 1 | 복합 인덱스 | ✅ | ✅ | ✅ | ✅ |
| 2 | 캐싱 | ✅ | ✅ | ✅ | ✅ |

**재개 메모**: 상태 1의 1차 측정(2026-08-31 11시경)에서 하드웨어 독립 증거는 예상대로 개선됐으나 GC 이상(old gen 승격 80 MB → 2,479 MB, 정지 합 ×2.8)으로 p95/p99가 악화됐다. 기준선이 재부팅 전 측정이라 편차/구조 판별을 위해 앱 재기동 후 상태 1을 재측정한다. 1차 산출물은 `*-1-discarded.*`로 보존 (`query-plan-1.txt`는 JVM과 무관해 유지). 이전 메모:  이슈는 analysis로 열렸으나 2026-08-31 호출자 결정으로 개선 적용 사이클까지 이 이슈에서 진행한다(이슈 체크리스트도 갱신됨). 사이클 1 기법은 복합 인덱스로 확정, 설계는 5-B 협의 중. 시드는 #104 잔존 데이터를 공유하고 이 이슈의 `seeds.sql`은 없다. `tokens.json`은 발급됨.

## 대상

- 엔드포인트: `GET /api/v1/courses/general-education?course-area={영역명}`
- 실행 경로: `CourseController.getGeneralEducationCourses` → `CourseService.getGeneralEducationCourses` → `CourseRepository.findByArea`
  - 응답 조립: `GeneralEducationCourseResponse.from` - `CourseScheduleFormatter.format(course.getSchedules())`, `course.is75MinLesson()`이 `schedules` 컬렉션에 접근
  - `JwtAuthenticationFilter`가 돌지만 서명 검증만 한다 (DB 접근 없음)
- 인증: `@Auth` 없음 → 토큰만 (화이트리스트 아님, `access-token` 헤더 필요, 회원 시드 불필요)
- 예상 쿼리 목록 (요청 1회 기준)
  1. `CourseRepository.findByArea` - `courses`에서 `area = ? AND status = 'ACTIVE'` 필터 후 `grade_code, classification_code, haksu_code` 정렬. 시간표 조인 페치 없음
  2. `Course.schedules` 지연 로딩 배치 - `@BatchSize(size = 1000)`이므로 `ceil(N / 1000)`번. 앱 시드 기준 가장 큰 영역(학문의기초)이 249건이라 1번. 시드로 영역당 강의 수를 1000 넘게 늘리면 2번 이상이 된다
  - 합계: 요청당 2회 (N ≤ 1000일 때)

## 측정 환경

> 값이 바뀌면 그 사실을 사이클 기록에 남긴다.

| 항목 | 값 |
|---|---|
| 프로파일 | perf (`application-perf.yml`) |
| DB | MySQL 8.0 / InnoDB (`uss-mysql`, `127.0.0.1:3307`, `uss_db`) |
| 커넥션 풀 크기 | 10 |
| InnoDB 버퍼 풀 크기 | 128 MiB |
| 데이터 규모 | `courses` 26,439 (앱 시드 2,439 + 성능 시드 24,000), `course_schedules` 79,819 (7,819 + 72,000). `members` 1,000, `carts` 0, `registrations` 0 (대상 쿼리가 읽지 않음) |
| 규모 근거 | 운영(앱 시드) 대비 약 10배. #104와 같은 규모로 맞춰 대상 간 비교가 되게 하고, 데이터가 늘어난 구조에서도 견디는지(성장 여지)를 본다. 성능 시드가 채운 교양 5개 영역은 각 3,015~3,249건으로 `@BatchSize(1000)`을 넘어 시간표 배치 쿼리가 요청당 4번이 된다 |
| 카디널리티 | `area`: 교양 13종, 성능 시드 5개 영역은 균등(3,000 + 앱 시드: 학문의기초 3,249, 기초과학공학 3,044, 핵심인문 3,031, 핵심사회 3,022, INU세미나 3,015), 앱 시드만 있는 8개 영역은 8~56건(인문 56, 예술체육 51, 사회 44, 과학기술 38, 외국어 25, 핵심외국어 21, 핵심과학기술 11, 핵심예술체육 8). `status` 1종(전 행 `ACTIVE`). 정렬 컬럼은 #104 기록과 동일: `grade_code` 10종, `classification_code` 15종, `haksu_code` 전 행 고유 |
| 부하 조건 | VU 30 (풀 10의 3배, #104와 동일), ramp-up 30s + 유지 1m + ramp-down 30s = 총 2m, USER_COUNT 1000. 요청마다 교양 13개 영역을 순환 조회한다 (큰 영역 5 : 작은 영역 8) |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고, Redis 캐시는 워밍업이 채운다. 매 측정 전 같은 워밍업으로 맞춘다. 비운 상태는 재지 않는다 |
| 되돌리기 절차 | 불필요 (읽기) |
| 시드 | 이 이슈에서 적재하지 않았다. #104가 적재한 시드가 DB에 남아 있어 그대로 쓴다 (`.claude/resources/perf/104/seeds.sql` + `member.sql` + `course.sql`, 변수: `@course_count` 24000, `@course_dept_count` 29, `@course_area_count` 8, `@schedules_per_course` 3, `@member_count` 1000). `count(*)`와 영역별 분포로 잔존을 확인했다 |
| 토큰 | `../tokens.json` (`mint-tokens.sh`, 회원 id 900001~901000). `@Auth`가 없어 회원 존재 여부는 무관하다. Phase 4에서 발급 |

## 기준선 (Baseline)

| 지표 | 값 |
|---|---|
| p95 | 2,419.0 ms |
| p99 | 2,783.3 ms |
| RPS | 16.7 (요청 2,006건 / 실측 구간 약 2m) |
| 에러율 | 0% |
| check 통과율 | 100% (3항목 모두 2006/2006) |
| 요청당 쿼리 수 | 대상 3.15 (findByArea 1.00 + schedules 배치 2.15), 트랜잭션 제어 포함 8.12 |
| heap 최대 / heap max | 798.8 / 4,096 MB (old gen 7.6~8.3% 안정) |
| GC 일시정지 합 / 최장 | 2,691 ms (요청당 1.34 ms) / 71 ms (offset 0의 145 ms는 워밍업 잔재 게이지) |
| GC overhead | 최대 0.011 |
| HikariCP pending 최대 / acquire max | 20 / 2,883 ms |
| 커넥션 보유 평균 | 540.5 ms |
| blocked 스레드 최대 | 0 |
| process CPU 최대 | 0.23 (평균 0.135, system 최대 0.83 - 같은 머신에 k6와 MySQL) |
| 캐시 적중률 (구획 없으면 `-`) | - (측정 구간 캐시, Redis 접근 없음) |

- 응답 크기: 수신 총량 1,151,642,129 B / 2,006건 = 요청당 평균 약 574 KB
- 할당량: 요청당 16.2 MB
- 요청당 DB 실행 시간 합: 약 157 ms (findByArea 125.9 + 배치 2.15×13.5 + 제어문 1.8)
- 리포지토리 호출: findByArea 요청당 1.0, mean 242.3 ms (digest mean 125.9 ms와의 차이는 결과 전송, 엔티티 하이드레이션 구간)

### 쿼리 통계 (total_ms 상위)

> 전체: `query-stats-summary-0.md` / k6 요약: `k6-test-summary-0.json`. 진단 근거로 쓴 행만 옮긴다.

| 요청당 | mean_ms | total_ms | 비중 | 읽은행/반환행 | 출처 |
|---|---|---|---|---|---|
| 1.0000 | 125.9 | 252,485 | 80.3% | 22.9 (호출당 약 27,644행 읽고 1,205행 반환, 테이블 전체 26,439행) | `CourseRepository.findByArea` |
| 2.1545 | 13.5 | 58,158 | 18.5% | 1.0 (호출당 1,660행) | `Course.schedules` `@BatchSize` 배치 로딩 |

### 진단

- 병목 성격: 1) `findByArea` 쿼리 자체 비효율 - area 필터를 받칠 인덱스가 없어(V1_10에서 제거) 호출마다 테이블 전량을 읽고 3컬럼 정렬을 수행. 2) 그 결과로 트랜잭션이 커넥션을 오래 쥐어(보유 540.5 ms vs 요청당 DB 실행 157 ms) 풀 10에서 VU 30이 커넥션 대기.
- 근거: #1이 DB 시간의 80.3%, 읽은행/반환행 22.9. HikariCP pending 최대 20(= VU 30 - 풀 10), acquire max 2,883 ms가 p99(2,783 ms)와 같은 자릿수. 유지 구간 내내 active 10 고정.
- 예상 쿼리 목록과 어긋난 지점: 없음 (schedules 배치 2.15 = Phase 3에서 재계산한 (5×4+8×1)/13과 일치)

---

## 사이클 1: `courses` 복합 인덱스 추가

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 대상 컬럼 | `area`, `grade_code`, `classification_code`, `haksu_code` | WHERE의 실질 필터 컬럼이 `area` 하나이고(`status`는 실제값 1종), 나머지 셋은 `ORDER BY` 전체다. `haksu_code`는 선택도 목적이 아니라 정렬 대체 목적 - MySQL은 `ORDER BY` 컬럼이 하나라도 인덱스에서 빠지면 부분 정렬 최적화가 없어 결과 전체를 filesort한다 |
| 컬럼 순서 | `(area, grade_code, classification_code, haksu_code)` 전부 ASC | 등호로 좁히는 `area`가 선두여야 뒤 세 컬럼이 정렬된 상태로 남는다. 뒤 셋은 `ORDER BY` 순서 그대로 |
| 프리픽스 길이 | 없음 (전체 길이) | 네 컬럼 합이 수십 바이트로 InnoDB 키 한도 3,072 B에 한참 못 미친다. 프리픽스는 등호를 범위 조건으로 바꿔 정렬 대체를 무너뜨린다 |
| 커버링 여부 | 커버링하지 않음 | 대상 쿼리가 28컬럼 전부를 읽고 MySQL에는 INCLUDE(비키 포함 컬럼)가 없어 커버링하려면 전부 키 컬럼이어야 한다. 그러면 인덱스가 테이블 사본이 된다. 읽는 컬럼을 줄인 뒤에야 재검토할 의미가 생긴다 |
| 감수할 쓰기 비용 | 관리자 동기화 배치의 인덱스 유지 1개분 (해당 기능은 제거 예정) + 저장 공간 | 수강신청, 취소는 `current_enrollment`만 UPDATE하고 이 컬럼은 인덱스에 없어 보조 인덱스 갱신이 없다. 상시 경로의 추가 비용은 사실상 0 |

- 검토했지만 택하지 않은 안
  - `status`를 인덱스에 포함 - 전 행이 `ACTIVE`라 실제값 1종. 걸러내는 행이 0이다
  - 커버링 인덱스 - 위 커버링 항목의 근거와 같다
  - 프리픽스 인덱스 - 위 프리픽스 항목의 근거와 같다
- 호출자가 예상한 효과: `courses` 풀스캔 제거 (읽은행/반환행 22.9 → 약 1, 호출당 27,644행 → 반환 행 수준), filesort 제거 (`Sort_rows` 3,249 → 0)

### 개선 전 지표

| 지표 | 값 |
|---|---|
| p95 / p99 | 2,419.0 / 2,783.3 ms |
| RPS | 16.7 |
| 요청당 쿼리 수 | 대상 3.15 (트랜잭션 제어 포함 8.12) |
| 대상 쿼리 calls / mean_ms / total_ms | 2,006 / 125.865 / 252,485 (비중 80.3%) |
| 읽은 행 / 반환 행 (`examined_per_sent`) | 22.9427 (호출당 약 27,644행 읽고 1,204.9행 반환) |
| 쿼리 전체 소요 (EXPLAIN ANALYZE) | 40 ms (ACADEMIC_FOUNDATION 3,249행 기준) |
| GC 일시정지 합 / HikariCP pending 최대 | 2,691 ms / 20 |
| 요청당 리포지토리 호출 수 / 할당량 | findByArea 1.0 / 16.2 MB |

**실행계획**

> 원본: `query-plan-{n-1}.txt` (개선 후는 `query-plan-{n}.txt`)

- EXPLAIN 파라미터: area = 'ACADEMIC_FOUNDATION', status = 'ACTIVE' (이후 모든 사이클에서 동일)
- 값 선정 근거: 흔한 값. 치우친 분포(5개 영역 3,015~3,249건, 8개 영역 8~56건)에서 부하 시간의 대부분이 큰 영역에서 발생하고, ACADEMIC_FOUNDATION이 최대 그룹(3,249건)이다
- 쿼리 전체 소요: 40 ms (루트 Sort 노드)
- 비용 상위 노드: Table scan 26.4 ms / Filter 누적 29.1 ms (순수 약 2.7) / Sort 누적 40 ms (순수 약 10.9, `Sort_merge_passes` 4)
- 접근 방식: 풀스캔 (access_type ALL), 사용 인덱스: 없음
- 실측 rows 대 반환 행 수: 26,439 / 3,249
- 옵티마이저 추정 대 실측: 스캔 rows=25,951 / 26,439 (근접), 필터 후 추정 259 (filtered 1.00%) / 실측 3,249 (12.5배 과소)
- 카운터: `Handler_read_rnd_next` 26,440 / `Handler_read_key` 1 / `Handler_read_next` 0 (미출현) / `Sort_rows` 3,249 (`Sort_scan` 1, `Sort_merge_passes` 4)
- 확정 해석: 최대 비용 노드는 Table scan (26.4 ms, 전체의 66%). 정렬(약 10.9 ms)은 부차이며 둘 다 이번 인덱스가 제거 대상으로 삼는다

{기법별 추가 캡처 - 캐싱이면 반복 호출 비율과 무효화 경로, 로직이면 호출 스택, 풀이면 획득 대기 시간, 락이면 대기 현황}

### 적용 내용

- `src/main/resources/database/migration/V1_12__add_area_index_to_courses.sql` 추가 - `ALTER TABLE courses ADD INDEX idx_area_sort (area, grade_code, classification_code, haksu_code)`. 코드 변경 없음 (쿼리 그대로)
- 적용 확인: 재기동으로 Flyway V1_12 적용, `ANALYZE TABLE courses` 후 EXPLAIN에서 `key: idx_area_sort`, `type: ref`, `Extra: Using where` (`Using filesort` 사라짐). 추정 rows 5,846 (실측 3,249 - 통계 추정치)
- 테스트: `./gradlew test` BUILD SUCCESSFUL (43s)

### 개선 후 지표

> 상태 1의 1차 측정은 GC 이상(승격 ×31, 정지 합 ×2.8)으로 폐기했다(`*-1-discarded.*` 보존). 앱 재기동 후 재측정에서 이상이 재현되지 않아(승격 2,479.2 → 13.1 MB, 같은 코드) 런 편차로 기록한다. 아래는 재측정본 기준이다.

| 구분 | 지표 | 개선 전 | 개선 후 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 2,419.0 ms | 1,709.6 ms | -29.3% |
| | p99 | 2,783.3 ms | 2,255.4 ms | -19.0% |
| | RPS | 16.71 | 25.76 | +54.2% |
| | 쿼리 전체 소요 | 40 ms | 6.52 ms | -84% |
| | heap 최대 | 798.8 MB | 1,182.9 MB | +48% (처리량 +54% 동반) |
| | GC 일시정지 합 / 최장 | 2,691 ms (요청당 1.341) / 71 ms | 1,758 ms (요청당 0.568) / 99 ms | 요청당 -58% |
| | HikariCP pending 최대 | 20 | 20 | 변화 없음 (VU 30 > 풀 10 구조) |
| | 커넥션 보유 평균 | 540.5 ms | 349.1 ms | -35.4% |
| | process CPU 최대 | 0.230 | 0.373 | +62% (처리량 +54% 동반) |
| 하드웨어 독립 | 요청당 쿼리 수 | 3.15 | 3.15 | 변화 없음 (예상대로) |
| | 요청당 리포지토리 호출 수 | 1.000 | 0.999 | 변화 없음 |
| | 요청당 할당량 (MB) | 16.165 | 15.933 | 변화 없음 |
| | 대상 쿼리 total_ms | 252,485 (mean 125.865, 80.3%) | 30,568 (mean 9.902, 30.5%) | mean -92.1% |
| | 읽은 행 / 반환 행 | 22.9427 | 1.0000 | 읽고 버리는 행 소멸 |
| | 접근 방식과 인덱스 | ALL (풀스캔), 없음 | ref, `idx_area_sort` | 설계 의도대로 |
| | `Handler_read_rnd_next` | 26,440 | 0 (미출현, `Handler_read_next` 3,249로 대체) | 풀스캔 소멸 |
| | `Sort_rows` | 3,249 (`Sort_merge_passes` 4) | 0 (미출현) | filesort 소멸 |
| | 캐시 hit / miss, 적중률 (캐싱 사이클만, 아니면 `-`) | - | - | - |

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): 있음 - 접근 방식 ALL → ref, 읽은행/반환행 22.9 → 1.0, `Handler_read_rnd_next` 26,440 → 0, `Sort_rows` 3,249 → 0. 요청당 작업량(쿼리 수, 할당량)이 동일한 상태에서 바뀐 것이므로 k6 개선(p95 -29.3%, RPS +54.2%)은 기법의 효과로 판정
- 남은 위험 신호: ① `schedules` 배치 요청당 2.15회 (1회 초과), DB 시간의 66.2% ② HikariCP pending 최대 20 (유지 구간 내내 풀 포화, VU 30 > 풀 10) ③ 응답 평균 574 KB, 요청당 할당 15.9 MB
- 다음 사이클 진행 여부: 계속 (사이클 2)

---

## 사이클 2: 영역별 교양 목록 캐싱

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 캐시 값 범위 | 정적 필드만 (제목, 학점, 시간표 문자열 등). `isRegisterable`은 캐시에서 빼고 요청마다 실시간 정원 조회로 병합 | 정원 마감 여부는 수강신청 기간에 초 단위로 변한다. 캐시에 넣으면 TTL과 무관하게 오답 구간이 생긴다. major-courses에서 검증된 분리 |
| 캐시 키 | `CourseArea.name()` (영문 상수명 13종) | `Enum.name()`은 final이라 국문 `getName()` 필드와 무관하게 상수 식별자를 준다. `from()`의 `toUpperCase()` 정규화로 대소문자 변형에도 키가 갈라지지 않는다. major의 `MemberDepartment.name()`과 같은 규칙 |
| TTL / 재적재 | TTL 25h(안전망) + 기동 시 + 매일 04:10 `@CachePut` 재적재 | major(04:00)와 10분 시차를 둬 두 캐시의 재적재 DB 조회가 겹치지 않게 한다 |
| 무효화 | 명시적 무효화 없음. 재적재 주기(최대 24h) 지연 감수 | 캐시 목록을 바꾸는 쓰기 경로가 관리자 동기화 하나뿐이고 제거 예정. 폐강은 정원 병합의 `containsKey` 필터(`status = ACTIVE` 조건)가 즉시 걸러낸다. major와 동일한 감수 |

- 검토했지만 택하지 않은 안
  - `isRegisterable`을 캐시에 포함 - 정원 오답 구간이 생긴다 (위 캐시 값 범위 근거)
  - 동기화 쓰기 경로에 `@CacheEvict` - 경로가 제거 예정이고 major 선례도 재적재 감수다
- 호출자가 예상한 효과: 적중 시 findByArea, schedules 배치가 요청 경로에서 빠져 요청당 쿼리 3.15 → 1 (실시간 정원 쿼리). DB 시간 96.7% 제거, 대신 Redis GET 왕복과 574 KB 역직렬화가 추가

### 개선 전 지표

> 상태 1 (사이클 1 적용 후, 재측정본) 기준.

| 지표 | 값 |
|---|---|
| p95 / p99 | 1,709.6 / 2,255.4 ms |
| RPS | 25.76 |
| 요청당 쿼리 수 | 대상 3.15 (트랜잭션 제어 포함 8.12) |
| 대상 쿼리 calls / mean_ms / total_ms | schedules 배치 6,668 / 9.929 / 66,208 (66.2%), findByArea 3,087 / 9.902 / 30,568 (30.5%) |
| 읽은 행 / 반환 행 (`examined_per_sent`) | 둘 다 1.0000 |
| 쿼리 전체 소요 (EXPLAIN ANALYZE) | 6.52 ms |
| GC 일시정지 합 / HikariCP pending 최대 | 1,758 ms (요청당 0.568) / 20 |
| 요청당 리포지토리 호출 수 / 할당량 | findByArea 1.0 / 15.9 MB |

**실행계획**

- `query-plan-1.txt` 재사용 (대상 쿼리와 상태가 같아 재캡처하지 않음). 파라미터, 해석은 사이클 1 개선 후와 동일: ref / `idx_area_sort`, filesort 없음, 쿼리 전체 6.52 ms
- 캐싱 추가 캡처 - 동일 입력 반복 비율: 입력이 13종뿐이라 측정 3,097건 중 첫 13건 이후 전부 반복(99.6%). 무효화가 필요한 쓰기 경로: 관리자 동기화 1개(제거 예정)

### 적용 내용

- 신규: `CachedGeneralEducationCourse(s)` (dto/common, 정적 필드 17개). 수정: `CourseCacheLoader`(`general-education-courses` 캐시, 키 `#courseArea.name()`), `CourseCacheWarmer`(기동 + 매일 04:10 재적재), `RedisCacheConfig`(Jackson 직렬화기), `CourseRepository.findCapacitiesByArea` 신설, `CourseService.getGeneralEducationCourses`(캐시 + 정원 `containsKey` 병합), `GeneralEducationCourseResponse.of(cached, isRegisterable)`, `application-perf.yml`(재적재 크론). `CourseArea.isGeneralEducationArea`를 public으로 (워머의 영역 필터용)
- 적용 확인: 재기동 후 Redis에 `general-education-courses::{영역}` 13키 생성. digest 리셋 후 요청 1회에 정원 쿼리(`SELECT id, current_enrollment, max_capacity ... WHERE area = ? AND status = ACTIVE`) 1건과 트랜잭션 제어문만 기록 - `findByArea`, `course_schedules IN` 소멸
- 테스트: `./gradlew test` BUILD SUCCESSFUL (29s)

### 개선 후 지표

**실행계획** (요청 경로의 쿼리가 `findCapacitiesByArea`로 바뀌어 이 쿼리를 캡처: `query-plan-2.txt`)

- EXPLAIN 파라미터: area = 'ACADEMIC_FOUNDATION', status = 'ACTIVE' (동일)
- 쿼리 전체 소요: 5.74 ms (Filter 루트). 접근 방식: ref / `idx_area_sort`, filesort 없음
- 카운터: `Handler_read_key` 1 / `Handler_read_next` 3,249 / `Handler_read_rnd_next`, `Sort_*` 미출현

| 구분 | 지표 | 개선 전 (상태 1) | 개선 후 (상태 2) | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 1,709.6 ms | 590.1 ms | -65.5% |
| | p99 | 2,255.4 ms | 806.4 ms | -64.2% |
| | RPS | 25.76 | 60.52 | +134.9% |
| | 쿼리 전체 소요 | 6.52 ms (findByArea) | 5.74 ms (findCapacitiesByArea - 쿼리가 달라 직접 비교 불가) | - |
| | heap 최대 | 1,182.9 MB | 447.6 MB | -62% |
| | GC 일시정지 합 / 최장 | 1,758 ms (요청당 0.568) / 99 ms | 509 ms (요청당 0.070) / 37 ms | 요청당 -88% |
| | HikariCP pending 최대 / acquire max | 20 / 2,225 ms | 20 / 976 ms | 피크 대기 여전, 대기 시간 -56% |
| | 커넥션 보유 평균 | 349.1 ms | 147.1 ms | -57.9% |
| | process CPU 최대 | 0.373 | 0.190 | -49% |
| 하드웨어 독립 | 요청당 쿼리 수 | 3.15 | 0.99 | findByArea, schedules 배치 소멸 |
| | 요청당 리포지토리 호출 수 | 1.0 (findByArea, mean 98.8 ms) | 1.0 (findCapacitiesByArea, mean 13.1 ms) | - |
| | 요청당 할당량 (MB) | 15.933 | 2.887 | -81.9% (엔티티 하이드레이션 소멸) |
| | 대상 쿼리 total_ms | 96,776 (schedules 66,208 + findByArea 30,568) | 28,156 (정원 쿼리) | -70.9% |
| | 읽은 행 / 반환 행 | 1.0000 | 1.0000 | 유지 |
| | 접근 방식과 인덱스 | ref / `idx_area_sort` | ref / `idx_area_sort` | 유지 |
| | `Handler_read_rnd_next` / `Sort_rows` | 0 / 0 | 0 / 0 | 유지 |
| | 캐시 hit / miss, 적중률 | - (캐시 없음) | 7,268 / 0, 100.0% | 신규 |
| | Redis GET (요청당 / mean / max) | - | 1.0 / 93.55 ms / 752.9 ms | 신규 - 최대 단일 비용 항목 |

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): 있음 - 요청당 쿼리 3.15 → 0.99, 요청당 할당 15.93 → 2.89 MB, 캐시 적중률 100%, 대상 쿼리 total -70.9%. 요청 경로 구조가 설계대로 바뀐 상태에서의 변화이므로 기법의 효과로 판정
- 남은 위험 신호: ① Redis GET 총 시간(약 680s)이 DB 쿼리 total(28s)의 24배 - 캐시 왕복이 새 1위 비용 (572 KB 값 × 동시 30요청, Lettuce 단일 커넥션) ② HikariCP pending 최대 20 (풀 포화 자체는 잔존, 보유 147 ms로 단축) ③ 응답 572 KB 유지
- 다음 사이클 진행 여부: 종료 (호출자 결정, 사이클 2에서 마무리)

---

## 최종 요약

| 구분 | 지표 | 최초 (상태 0) | 최종 (상태 2) | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 2,419.0 ms | 590.1 ms | -75.6% |
| | p99 | 2,783.3 ms | 806.4 ms | -71.0% |
| | RPS | 16.71 | 60.52 | +262.2% |
| | 쿼리 전체 소요 | 40 ms (findByArea 풀스캔+filesort) | 5.74 ms (findCapacitiesByArea, ref) | 요청 경로 쿼리 자체가 교체됨 |
| | heap 최대 | 798.8 MB | 447.6 MB | -44% |
| | GC 일시정지 합 / 최장 | 2,691 ms (요청당 1.341) / 71 ms | 509 ms (요청당 0.070) / 37 ms | 요청당 -94.8% |
| | HikariCP pending 최대 | 20 | 20 | 피크 포화 잔존 (acquire max 2,883 → 976 ms) |
| | 커넥션 보유 평균 | 540.5 ms | 147.1 ms | -72.8% |
| | process CPU 최대 | 0.230 | 0.190 | -17% (RPS +262% 상태에서) |
| 하드웨어 독립 | 요청당 쿼리 수 | 3.15 | 0.99 | -68.6% |
| | 요청당 리포지토리 호출 수 | 1.0 (findByArea, mean 242.3 ms) | 1.0 (findCapacitiesByArea, mean 13.1 ms) | 호출 대상 교체 |
| | 요청당 할당량 (MB) | 16.165 | 2.887 | -82.1% |
| | 읽은 행 / 반환 행 | 22.9427 (호출당 27,644행 읽음) | 1.0000 | 읽고 버리는 행 소멸 |
| | 접근 방식과 인덱스 | ALL (풀스캔), 없음 | ref, `idx_area_sort` | 사이클 1 |
| | `Handler_read_rnd_next` | 26,440 (`Sort_rows` 3,249) | 0 (`Sort_*` 미출현) | 풀스캔, filesort 소멸 |
| | 캐시 hit / miss, 적중률 | - (캐시 없음) | 7,268 / 0, 100.0% | 사이클 2 |

적용한 기법: 사이클 1 - `courses` 복합 인덱스 `idx_area_sort` (V1_12). 사이클 2 - 영역별 교양 목록 Redis 캐싱 (정적 목록 + 실시간 정원 병합, 기동/매일 04:10 재적재)

운영 반영 시 유의점: V1_12 인덱스 추가는 로컬 26,439행에서 즉시 완료됐다. 운영 행 수는 미확인이며, 앱 시드 규모(약 2,439행)와 같다면 영향은 미미할 것이나 실측 전에는 단정하지 않는다. 캐시는 운영 프로파일의 `spring.cache.type`과 `cache.general-education-courses.refresh-cron` 설정이 있어야 동작한다 (현재 크론 정의는 perf 프로파일에만 있음. prod는 `cache.type: none`이라 캐시와 워머가 비활성 - 운영 적용 시 설정 추가 필요)
