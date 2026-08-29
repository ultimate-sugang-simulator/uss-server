# [PERF-104] GET /api/v1/courses/major

> 이슈: #104
> 브랜치: refactor/104-major-courses-perf
> 대상 디렉토리: `.claude/resources/perf/104/major/`

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
| 1 | `courses` 복합 인덱스 추가 | ✅ | ✅ | ✅ | ✅ |
| 2 | JPQL `DISTINCT` 제거 | ✅ | ✅ | ✅ | ✅ |
| 3 | fetch join 제거 + `@BatchSize` 활용 | ✅ | ✅ | ✅ | ✅ |
| 4 | 정적 부분 Redis 캐싱 + `isRegisterable` 라이브 조립 | ✅ | ✅ | ✅ | ✅ |

**재개 메모**: **완료.** 사이클 4까지 끝났고 2026-08-30 Phase 9 보고까지 마쳤다. 남은 대상 없음(대기 목록은 Phase 1에서 알린 것이 없다). 커밋은 `commit-push`, PR은 `open-pr`로 잇는다. 이 이슈에서 다른 대상을 잰다면 Phase 1부터 새 슬러그 디렉토리로 시작한다.

**재개 전 확인할 것.**
1. `../tokens.json`은 2026-08-30 00:03에 재발급했고 **2026-08-31 00:03:13에 만료**된다
2. 내렸으면 `docker-compose -f docker/docker-compose-local.yml up -d mysql redis` 뒤 `bootRun --args='--spring.profiles.active=perf'`. 재기동하면 캐시가 flush되므로 워밍업이 다시 채운다
3. Phase 7 범위: `build.gradle`에 `spring-boot-starter-data-redis` + `spring-boot-starter-cache` 추가, `global/config`에 캐시 설정(`@EnableCaching`, `RedisCacheManager` JSON 직렬화 + TTL 24h), `application-perf.yml`에 `spring.data.redis` + `spring.cache.redis.enable-statistics=true`, 테스트 프로파일은 Redis 없이 돌아야 하므로 `spring.cache.type: none`(또는 대체) 확인, 기동 시 flush, 정적 레코드 + 래퍼(`course/dto/common/`), projection 레코드, `@Cacheable(sync = true)` 별도 빈, `CourseService.getMajorCourses` 조립. Boot 4.0.1 = Jackson 3라 Spring Data Redis 직렬화기 클래스명을 실물로 확인한다
4. 사이클 1~3의 코드 변경(`CourseRepository`, `V1_10`, `V1_11`)은 아직 **커밋 전**이다. 커밋 시점은 호출자가 정한다
5. 남은 위험 신호 4개는 사이클 3 **판정** 절에 있다. 이 설계는 ①(두 쿼리 합 점유율)과 ③(세트 B Sort)에 닿고, ②(추정 괴리)와 ④(DB 밖 정체)는 그대로 남는다. Phase 8 카운터는 기동 이후 누적이라 측정 구간 전후 차로 읽는다

**인덱스를 걷어낸 상태에서 다시 시작한다.** `V1_10__drop_indexes_for_redesign.sql`로 측정 근거 없이 만들어진 보조 인덱스 6개를 드롭했다
(`courses`의 `idx_department_sort`, `idx_area_sort`, `idx_huss_sort`, `members.idx_student_id`, `course_sync_jobs`의 `idx_started_at`, `idx_status`).
PK, UNIQUE 제약, FK 인덱스, FULLTEXT는 남겼다. 대상 쿼리 `findByDepartmentIn`이 탈 수 있는 인덱스가 이제 없다.
인덱스는 Phase 6의 실행계획을 근거로 사이클에서 다시 설계한다.

**두 사이클을 지나도 확정하지 않은 것.** DB 밖 비중이 사이클마다 커진다 - 요청 시간 대비 DB 시간이 Phase 4 약 22%(400.13940 ms 대 med 1,816.719 ms),
사이클 1 약 14.3%(191.8663 대 1,345.978), 사이클 2 약 5.7%(약 64.6 대 1,135.2585)다. DB를 더 깎아도 응답시간에 돌아오는 몫이 5.7% 안쪽이라는 뜻이다.
남은 94.3%가 직렬화(DTO 1,498개 / 755 KB)인지, 커넥션 획득 대기(VU 30 대 풀 10)인지, GC인지는 아직 갈리지 않았다.
셋 다 "행이 많아서"가 원인이라는 점은 같으나 줄일 지점이 다르다.
가를 때는 부하 중 actuator를 긁는다 (`hikaricp_connections_acquire_seconds_sum`, `usage_seconds_sum`, `jvm_gc_pause_seconds_sum`, `jvm_gc_memory_allocated_bytes_total`).

**사이클 3의 기법 후보.** Phase 5-A에서 이미 제시했고 아직 쓰지 않은 것들이다. 표를 새로 만들 때 근거 수치는 `query-stats-summary-2.md`와 `query-plan-2.txt`로 갱신하라.

- **DTO projection (읽는 컬럼 축소)** - `MajorCourseResponse`가 쓰지 않는 `courses` 컬럼이 7개다(`academic_year`, `term`, `area`, `area_code`, `area_name`, `college`, `english_code`).
  1:N 컬렉션이 걸림돌이다 - `schedule` 문자열과 `is75MinLesson`이 `schedules` 전체를 요구해 단순 생성자 projection으로 안 접힌다.
  호출자가 5-B에서 `GROUP_CONCAT` + 사용자 정의 함수 방향을 언급했다(사이클 2 **설계 결정**의 배제한 안에 판단 기록이 있다)
- **fetch join 제거 + `@BatchSize` 활용** - `Course.schedules`에 이미 `@BatchSize(size = 1000)`이 붙어 있다(`Course.java:56`).
  fetch join을 빼면 N+1이 아니라 `IN` 배치 몇 건으로 나뉘어 courses 1,498행과 schedules 4,570행을 곱집합 없이 따로 받는다.
  조인 폭발(SQL이 4,570행을 돌려주고 Hibernate가 1,498개로 접는 구조)을 없애는 가장 싼 선택지다. 요청당 쿼리 수가 는다
- **`IN` 분해 조회** - 세트 B에 `Sort_rows` 3,378이 남아 있다. 적용 범위가 좁다(`IN` >= 2인 회원 학과는 74개 `CourseDepartment` 중 7개, 부하 회원의 62.5%가 `IN` 1개)
- **커넥션 풀 / 트랜잭션 경계** - 위 DB 밖 정체를 actuator로 먼저 갈라야 기법이 확정된다
- **조회 결과 캐싱** - **1~4의 효과를 전부 가린다.** 적중한 요청은 쿼리를 아예 돌지 않아 이후 사이클에서 쿼리 개선의 하드웨어 독립 증거를 잴 분모가 사라진다.
  무효화는 `isRegisterable`이 `current_enrollment`에 의존해 수강신청·취소마다 바뀌는 것이 걸린다

폐기 이전 측정에서 이미 확인해 그대로 유효한 사실 셋.

1. 이슈 #104의 유일한 대상이다. `/api/v1/courses/other-department`가 같은 `findByDepartmentIn`과 같은 응답 DTO를 쓰므로, 개선이 그쪽에 그대로 옮겨갈지는 Phase 9에서 확인한다.
2. Phase 2에서 `docker/docker-compose-local.yml`의 mysql `command`에 `--performance-schema-max-digest-length=4096`과 `--performance-schema-max-sql-text-length=4096`을 추가했다(기본 1024). 조인 페치 select 목록이 36컬럼이라 digest가 잘리면 `findByDepartmentIn`, `findByDepartment`, `findByArea`, `findHussCourses`가 한 digest로 합쳐지기 때문이다.
   다만 `max_digest_length`는 아직 1024라 대상 쿼리 원문이 966자에서 잘린다. 같은 select 목록을 쓰는 쿼리가 둘 이상 생기면 한 digest로 합쳐지므로, 그때는 mysql `command`에 `--max-digest-length=4096`을 추가하고 재기동한다.
3. 실행 간 편차가 크다. 같은 스크립트, 같은 데이터로 잰 두 실행이 RPS 14.06 / med 1,504.2ms 와 RPS 18.06 / med 1,238.7ms 였다(28% 차이). k6, JVM, mysqld가 한 노트북에서 도는 탓이다. **Phase 8의 개선 판정을 응답시간과 RPS로 하지 마라.** 하드웨어 독립 지표(요청당 쿼리 수, `rows_per_call`, `Sort_rows`, `Handler_*`, 커넥션 점유시간, 요청당 할당량)를 근거로 삼는다.

`../tokens.json`(회원 id 900001~901000)과 시드는 그대로 쓸 수 있다. 회원과 강의 데이터는 건드리지 않았다.

`IN` 목록이 큰 학과일수록 조회 행 수도 같이 크다(아래 **회원 학과별 조회량**). 두 변수가 함께 움직이므로, 이 경로가 느리게 나와도
원인이 `IN` 목록 크기인지 행 수인지는 부하 테스트만으로 갈리지 않는다. Phase 6의 `EXPLAIN ANALYZE`에서
파라미터를 바꿔 분리한다. (Phase 3-A에서 호출자와 확인하고 감안하기로 한 사항)

## 대상

- 엔드포인트: `GET /api/v1/courses/major`
- 실행 경로: `CourseController.getMajorCourses` → `CourseService.getMajorCourses` → `MemberRepository.findById`, `CourseRepository.findByDepartmentIn`
- 인증: `@Auth` 있음 → 회원 시드 필요. `JwtAuthenticationFilter`는 서명만 검증하고 DB를 보지 않으며, `AuthArgumentResolver`는 request attribute만 읽는다
- 예상 쿼리 목록 (요청 1회 기준)
  1. `MemberRepository.findById` - 회원 단건 조회(PK). `Member`에 연관관계 매핑이 없어 추가 SELECT가 없다
  2. `CourseRepository.findByDepartmentIn` - `courses` + `course_schedules` 조인 페치. `DISTINCT`, `department IN (...)`, `status = ACTIVE`, `ORDER BY grade_code, classification_code, haksu_code`
- 지연 로딩 참조 지점: 없음. `MajorCourseResponse.from`이 만지는 `getSchedules()`는 조인 페치로 초기화되고, `getDepartment()`는 enum 컬럼이다.
  `Course.schedules`의 `@BatchSize(size = 1000)`은 이 경로에서 작동하지 않으며, 조인 페치를 걷어내는 설계로 가야 살아난다
- 분기: `CourseDepartment.ownedBy`가 빈 리스트면 `CourseService.java:52`에서 조기 반환해 쿼리가 1번이다. 시드에서 학과를 잘못 잡으면 이 경로로 빠진다

## 측정 환경

> 값이 바뀌면 그 사실을 사이클 기록에 남긴다.

| 항목 | 값 |
|---|---|
| 프로파일 | perf (`application-perf.yml`) |
| DB | MySQL 8.0 (`mysql:8.0`) / InnoDB (`uss-mysql`, `127.0.0.1:3307`, `uss_db`) |
| 커넥션 풀 크기 | 10 (`maximum-pool-size`, `minimum-idle`도 10) |
| InnoDB 버퍼 풀 크기 | 128 MiB (MySQL 기본값) |
| 데이터 규모 | `courses` 26,439 (앱 시드 2,439 + 성능 시드 24,000), `course_schedules` 79,819 (7,819 + 72,000), `members` 1,000 |
| 규모 근거 | 운영(앱 시드) 대비 약 10배. 회원 학과 하나가 조회하는 강의가 43~90건에서 843~918건이 됐다. 지금 규모에서 견디는 쿼리가 데이터와 트래픽이 늘어난 구조에서도 견디는지를 미리 확인하는 것이 목적이다. 운영 규모 재현이 아니라 성장 여지를 재는 설정이다 |
| 카디널리티 | `courses` 전체 26,439행 기준: `department` 88종(최대 그룹 918), `status` 1종(전 행 `ACTIVE`), `grade_code` 10종(최대 그룹 4,800), `classification_code` 15종(최대 그룹 4,000), `haksu_code` 전 행 고유. 회원 1,000명이 8개 학과에 균등 분포하고, 학과별 `department IN` 목록 크기와 조회량은 아래 표와 같다 |
| 시드 코드 포맷 | 앱 시드(`id < 900000`, 2,439행)와 성능 시드(`id >= 900000`, 24,000행)의 코드 값이 하나도 겹치지 않는다. `grade_code`는 앱이 `0`~`4`, 성능 시드가 `01`~`05`. `classification_code`는 앱이 `41`/`31`/`11`/`25`/`23`/`21`/`50`/`80`/`70`, 성능 시드가 `01`~`06`. Phase 3에 적었던 87 / 5 / 9는 앱 시드만의 값이었다. `varchar` 정렬이라 `'0' < '01' < … < '05' < '1'` 순으로 두 시드가 섞여 정렬된다 |
| 부하 조건 | VU 30 (풀 10의 3배), ramp-up 30s + 유지 1m + ramp-down 30s = 총 2m, USER_COUNT 1000 |
| 캐시 상태 | warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고 애플리케이션 캐시는 없다. 매 측정 전 같은 워밍업으로 맞춘다 |
| 인덱스 상태 | `courses`에 보조 인덱스 없음. PK, `uk_year_term_haksu`, `ft_idx_course_search`만 남았다 (`V1_10__drop_indexes_for_redesign.sql`) |
| 되돌리기 절차 | 불필요 (읽기 전용 엔드포인트) |
| 시드 | `../seeds.sql` + `member.sql` + `course.sql` (`enrollment.sql` 미사용 - 대상 쿼리가 `carts`, `registrations`를 읽지 않는다). 변수: `@member_count` 1000, `@member_dept_count` 8, `@course_count` 24000, `@course_dept_count` 29, `@schedules_per_course` 3 |
| 토큰 | `../tokens.json` (`mint-tokens.sh`, 회원 id 900001~901000). Phase 4에서 발급 |

### 회원 학과별 조회량

> 시드 회원 1,000명이 아래 8개 학과에 균등 분포한다(각 125명). `joined_rows`는 `LEFT JOIN course_schedules` 후 행 수로, `DISTINCT`가 접기 전의 크기다.

| 회원 학과 | `IN` 목록 | 강의 수 | 조인 후 행 수 |
|---|---|---|---|
| `ELECTRONICS_ENGINEERING_SCHOOL` | 4 | 3,378 | 10,246 |
| `LIFE_SCIENCE_SCHOOL` | 3 | 2,526 | 7,674 |
| `GLOBAL_TRADE_SERVICE` | 2 | 1,693 | 5,087 |
| `MECHANICAL_ENGINEERING` | 1 | 918 | 3,055 |
| `BUSINESS_ADMINISTRATION` | 1 | 895 | 2,659 |
| `COMPUTER_ENGINEERING` | 1 | 879 | 2,735 |
| `ECONOMICS` | 1 | 852 | 2,573 |
| `MATHEMATICS` | 1 | 843 | 2,514 |

## 기준선 (Baseline)

> `V1_10__drop_indexes_for_redesign.sql` 적용 후, `courses`에 보조 인덱스가 없는 상태에서 측정했다.

| 지표 | 값 |
|---|---|
| p95 | 4217.297049999999 ms |
| p99 | 8610.90285 ms |
| RPS | 11.361679173551456 |
| 에러율 | 0 |
| check 통과율 | 1 (3개 항목 전부 1364/1364) |
| 요청당 쿼리 수 | SELECT 2건 (트랜잭션 제어문까지 포함하면 6.9948건) |

med 1824.5304999999998 ms / max 11885.935 ms, 요청 1364건, 요청당 응답 크기 754,827 B (`bytes_received` 1,029,584,776 B / 1364).

### 쿼리 통계 (total_ms 상위)

> 전체: `query-stats-summary-0.md` / k6 요약: `k6-test-summary-0.json`. 진단 근거로 쓴 행만 옮긴다.

| 요청당 | mean_ms | total_ms | 비중 | 읽은행/반환행 | 출처 |
|---|---|---|---|---|---|
| 1.0000 | 393.78531 | 537123.163886 | 98.4120% | 7.7942 | `CourseRepository.findByDepartmentIn` |
| 1.0000 | 1.500286 | 2046.391447 | 0.3749% | 1.0000 | `MemberRepository.findById` |

대상 쿼리의 `rows_per_call`은 4562.5169이고, `examined_per_sent` 7.7942를 곱하면 호출당 읽은 행이 약 35,561이다.
반환 4,563행을 빼면 약 30,999행을 읽고 버린다. `courses` 전체가 26,439행이므로 이 안에 테이블 풀스캔 1회분이 통째로 들어 있다.

### 진단

- 병목 성격: 대상 쿼리가 요청 1회에 **읽는 행 수**. `department IN (...)`을 좁혀 줄 인덱스가 없어 `courses` 풀스캔이 들어가고, 읽은 행의 87%를 버린다. 쿼리 호출 수 문제(N+1)가 아니다 - 요청당 SELECT는 2건뿐이다.
- 근거: 대상 쿼리가 DB 시간의 98.4120%(나머지 6개 합계 1.5880%). `examined_per_sent` 7.7942, 호출당 읽은 행 약 35,561 대 반환 4,562.5169. `courses` 전체 행 수 26,439. 같은 데이터·같은 스크립트로 인덱스가 있던 측정에서는 `examined_per_sent`가 1.9569, mean_ms가 194.37749였고, 인덱스를 걷어내자 각각 약 4배와 약 2배가 됐다.
- 미확정: 요청당 DB 시간 400.13940 ms 대 `waiting_ms` med 1,816.719 ms로 요청 시간의 약 78%가 DB 밖에 있다. 직렬화, 커넥션 획득 대기, GC 중 무엇인지는 기준선 자료로 갈리지 않아 사이클 2로 넘겼다.
- 예상 쿼리 목록과 어긋난 지점: 없음. 요청당 SELECT 2건으로 Phase 1의 예상과 일치한다. 예상 목록에 없던 것은 트랜잭션 제어문 4종(`SET autocommit` 요청당 1.9963회, `SET SESSION TRANSACTION READ WRITE`, `SET SESSION TRANSACTION READ ONLY`, `COMMIT`)이며 합쳐서 비중 1.2130%다.

---

## 사이클 1: `courses` 복합 인덱스 추가

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 대상 컬럼 | `department`, `grade_code`, `classification_code`, `haksu_code` | `WHERE`의 유일한 필터 컬럼이 `department`이고, 나머지 셋은 `ORDER BY` 전체다. MySQL은 `ORDER BY` 컬럼이 하나라도 인덱스에서 빠지면 인덱스 순서로 정렬을 대신하지 못한다(부분 정렬 최적화 없음) |
| 컬럼 순서 | `(department, grade_code, classification_code, haksu_code)` 전부 ASC | 등호·`IN`으로 좁히는 `department`가 선두여야 뒤 컬럼이 정렬된 상태로 남는다. 뒤 셋의 순서가 `ORDER BY`와 어긋나면 정렬 대체가 무효다. `ORDER BY`가 전부 ASC다 |
| 프리픽스 길이 | 없음 (전체 길이) | 네 컬럼 최대 합 52 B로 InnoDB DYNAMIC 키 한도 3,072 B에 한참 못 미친다. `department` 평균이 17.6358 B라 프리픽스 25로 잘라도 절감이 사실상 0이다(88종 전체가 25에서 이미 구별된다). 무엇보다 프리픽스는 등호를 프리픽스 범위로 바꿔 `ORDER BY` 대체를 무너뜨린다 |
| 커버링 여부 | 커버링하지 않음 | 대상 쿼리가 `courses` 28개 컬럼을 전부 읽는다. `title_kr`, `title_en`이 각 `varchar(255)`라 커버링하면 인덱스가 테이블 사본이 된다. 읽는 컬럼을 줄인 뒤에야 의미가 생긴다 |
| 감수할 쓰기 비용 | 관리자 동기화 배치의 인덱스 유지 1개분 | 인덱스 컬럼에 `current_enrollment`가 없어 수강신청·취소 `UPDATE`는 보조 인덱스를 갱신하지 않는다. 비용은 `CourseSyncApplier`의 학기당 약 2,439행 DELETE/INSERT에만 붙고, 비유니크 인덱스라 체인지 버퍼가 삽입을 모아 처리한다. 드롭 전 보조 인덱스가 3개였으므로 쓰기 부담은 그때보다 가볍다 |

- 검토했지만 택하지 않은 안
  - `status`를 인덱스에 포함 - 전 행이 `ACTIVE`라 실제값 1종이다. 걸러내는 행이 0이고, 28개 컬럼을 다 읽어야 해서 커버링 목적도 없다. 엔트리당 7 B만 늘어난다. (드롭한 `idx_department_sort`는 `status`를 선두에 뒀었다)
  - `haksu_code`를 빼고 세 컬럼만 - 읽고 버리는 행 문제는 해결되지만 `IN` 1개에서도 filesort가 남는다. 정렬 대상이 강의 843~918건이 아니라 조인 후 2,514~3,055행이고 36컬럼 폭이라 `sort_buffer_size` 262,144 B를 넘길 공산이 크다. 넣는 대가는 행당 11.9 B, 총 약 307 KB(버퍼 풀 128 MiB의 0.23%)다
  - `department` 프리픽스 인덱스 - 위 프리픽스 항목의 근거와 같다
- 호출자가 예상한 효과: `courses` 풀스캔 제거. `examined_per_sent` 7.7942 → 인덱스가 있던 폐기 측정 수준(1.9569)으로 하락. `IN` 1개 경로에서 `Sort_rows` 0

### 개선 전 지표

| 지표 | 값 |
|---|---|
| p95 / p99 | 4217.297049999999 ms / 8610.90285 ms |
| RPS | 11.361679173551456 (에러율 0, check 통과율 1) |
| 요청당 쿼리 수 | SELECT 2건 (트랜잭션 제어문 포함 6.9948건) |
| 대상 쿼리 calls / mean_ms / total_ms | 1364 / 393.78531 / 537123.163886 (비중 98.4120%) |
| 읽은 행 / 반환 행 (`examined_per_sent`) | 7.7942 (호출당 읽은 행 약 35,561 / 반환 4562.5169) |

**실행계획**

> 원본: `query-plan-0.txt` (개선 후는 `query-plan-1.txt`)

- EXPLAIN 파라미터 (이후 모든 사이클에서 동일)
  - 세트 A (기준): `department IN ('COMPUTER_ENGINEERING')`, `status = 'ACTIVE'`
  - 세트 B (대조): `department IN ('ELECTRONICS_ENGINEERING_SCHOOL','ELECTRONICS_ENGINEERING_MAJOR','ELECTRONICS_ENGINEERING','SEMICONDUCTOR_CONVERGENCE_MAJOR')`, `status = 'ACTIVE'`
- 값 선정 근거: 세트 A는 `IN` 1개인 다섯 학과(843 / 852 / **879** / 895 / 918) 중 행 수 중앙값이라 한쪽 끝이 아니다. 부하 회원의 62.5%가 `IN` 1개이고, 운영에서도 `IN` ≥ 2인 회원 학과는 74개 `CourseDepartment` 중 7개뿐이라 흔한 값이다. 세트 B는 `IN` 목록 크기와 조회 행 수가 함께 움직이는 문제를 분리하기 위한 최악 경로 대조군이다
- 접근 방식: `Table scan on c1_0` (풀스캔), 사용 인덱스: **없음**. 조인 상대인 `course_schedules`만 `idx_course_id`를 탄다
- 실측 rows 대 반환 행 수: 세트 A는 26,439행을 읽어 879행으로 걸러 조인 후 2,735행 반환. 세트 B는 26,439행 → 3,378행 → 10,246행
- 옵티마이저 추정 대 실측: `Table scan` 26,046 / 26,439 (일치), `Filter` 260 / 879 (3.4배), `Nested loop` 750 / 2,735 (3.6배). 10배 괴리 없음
- 카운터 (세트 A / 세트 B): `Handler_read_rnd_next` 29,176 / 36,687, `Handler_read_key` 3,615 / 13,625, `Handler_read_next` 2,732 / 10,243, `Handler_write` 2,735 / 10,246, `Sort_scan` 1 / 1, `Sort_rows` 2,735 / 10,246, `Sort_merge_passes` 3 / 9
- 해석: 비용이 `c1_0` 풀스캔에 몰려 있다. 세트 A 총 54.4 ms 중 풀스캔이 26.6 ms(**48.9%**), 필터 2.2 ms, 조인 4.7 ms로 읽기 단계가 33.5 ms(61.6%)다. 나머지는 중복 제거 임시 테이블 11.6 ms(21.3%)와 정렬 9.3 ms(17.1%)다. `Handler_read_rnd_next` 29,176 = `courses` 26,439 + 임시 테이블 되읽기 2,736으로 맞아떨어진다. 879건을 쓰려고 26,439행을 읽는 구조다
- 위험 신호 대조: 단일 쿼리 `total_ms` 점유율 98.4120%(기준 30% 이상)와 1만 행 이상 테이블의 풀스캔(기준 해당)이 걸린다. `examined_per_sent` 7.7942, 추정 대 실측 최대 3.6배, 요청당 호출 1.0000, `Sort_rows` 2,735 = 반환 행 수는 기준에 걸리지 않는다
- 중복 제거 임시 테이블은 입출력 행 수가 같다 (세트 A 2,735 → 2,735, 세트 B 10,246 → 10,246). 단 한 행도 접지 못한다. 폐기 측정에서 `idx_department_sort`가 있던 상태에서도 이 임시 테이블은 그대로였고 인덱스가 없앤 것은 정렬뿐이었으므로, 인덱스와 `DISTINCT` 제거는 서로의 효과를 가리지 않는 독립 기법이다. 사이클 2에서 다룬다

### 적용 내용

- `src/main/resources/database/migration/V1_11__add_index_to_courses.sql` 추가. `ALTER TABLE courses ADD INDEX idx_department_sort (department, grade_code, classification_code, haksu_code);` 한 문장이다. 자바 코드 변경은 없다 - 쿼리는 그대로다
- 이름은 V1_10에서 걷어낸 것과 같은 `idx_department_sort`를 다시 썼다. 컨벤션이 `idx_{용도}`이고 용도가 같아서다. 컬럼 구성은 다르다 (선택도 0인 `status`를 선두에서 뺐다)
- `ANALYZE TABLE courses` 실행. 갱신된 카디널리티는 `department` 88, `grade_code` 470, `classification_code` 1400, `haksu_code` 25998이다
- 적용 확인: `EXPLAIN`에서 `type: ref`, `key: idx_department_sort`, `key_len: 202`(= `varchar(50)` utf8mb4 200 + 길이 2), 추정 `rows: 879`로 실측과 일치. **`Extra`에서 `Using filesort`가 사라졌다.** `Using temporary`는 남았다 - `DISTINCT` 중복 제거 임시 테이블이고 Phase 6에서 인덱스와 독립이라고 본 그대로다
- `key_len` 202는 `department` 하나만 접근 조건으로 쓰였다는 뜻이다. 나머지 세 컬럼은 정렬을 대신하는 역할이며 설계 의도와 일치한다
- 테스트: `./gradlew test` BUILD SUCCESSFUL (52s), 실패 0건
- Flyway `1.11` 적용 확인 후 애플리케이션 재기동 완료

### 개선 후 지표

| 구분 | 지표 | 개선 전 | 개선 후 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 4217.297049999999 ms | 2619.0532 ms | −37.9% |
| | p99 | 8610.90285 ms | 3314.4916799999987 ms | −61.5% |
| | RPS | 11.361679173551456 | 16.372238821922682 | +44.1% |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 2건 (전체 6.9948) | SELECT 2건 (전체 6.9960) | 동일 |
| | 대상 쿼리 total_ms | 537123.163886 | 370836.376959 | −31.0% (mean_ms 393.78531 → 188.720802) |
| | 읽은 행 / 반환 행 | 7.7942 | 1.9560 | **−74.9%** |
| | 접근 방식과 인덱스 | `Table scan on c1_0`, 인덱스 없음, 26,439행 | `Index lookup using idx_department_sort`, 879행 | 풀스캔 제거 |
| | `Handler_read_rnd_next` | 29,176 | 2,736 | **−90.6%** |
| | `Sort_rows` | 2,735 (`Sort_scan` 1, `Sort_merge_passes` 3) | 0 (세 카운터 모두 부재) | **filesort 제거** |
| | 캐시 hit / miss, 적중률 (캐싱 사이클만, 아니면 `-`) | - | - | - |

`rows_per_call`은 4562.5169 → 4563.1405로 사실상 같다. 응답으로 나가는 결과는 달라지지 않았고 읽는 방식만 바뀌었다.
요청당 응답 크기도 754,827 B → 754,939 B로 동일하다.

세트 B(`IN` 4개)는 `Index range scan ... with index condition`으로 바뀌어 총 134 ms → 105 ms, `Handler_read_rnd_next` 36,687 → 10,247이 됐으나
`Sort_rows` 10,246 / `Sort_merge_passes` 9는 그대로다. `IN`이 2개 이상이면 값별 구간을 합쳐야 해 정렬이 남는다는 Phase 5-B의 판단과 일치한다.

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): **있음.** `examined_per_sent` 7.7942 → 1.9560, `Handler_read_rnd_next` 29,176 → 2,736, `Sort_scan`/`Sort_rows`/`Sort_merge_passes` 1/2,735/3 → 0/0/0. 셋 다 타이머가 아니라 행 수 카운터라 부하 조건과 머신 상태에 흔들리지 않는다. 접근 방식 자체가 `Table scan` → `Index lookup`으로 실행계획에 찍혀 있어 측정 편차로 설명되지 않는다. 요청당 쿼리 수가 그대로이므로 쿼리를 줄여서 얻은 것도 아니다
- 5-B 예상과 대조: 예상 세 가지(풀스캔 제거 / `examined_per_sent` 약 1.9569 / `IN` 1개에서 `Sort_rows` 0)가 모두 적중했다. 특히 `examined_per_sent`는 1.9560으로 예상치와 0.0009 차이다
- 측정 이력: `-1`의 첫 측정은 실패해 폐기하고 재측정했다. 애플리케이션 재기동 2분 45초 뒤에 재는 바람에(`-0`은 최소 4분 30초) JIT가 덜 된 상태에서 일부 요청이 커넥션을 10초씩 붙들어 풀이 고갈됐다(`checks_rate` 0.9965, 500 6건). 중단된 문장 때문에 `COMMIT`의 `SUM_TIMER_WAIT`가 BIGINT UNSIGNED 최대값 근처로 래핑돼 통계 수집 쿼리의 `100 * SUM_TIMER_WAIT`가 오버플로했다. 재측정은 같은 인스턴스를 그대로 두고 A 블록만 다시 돌려 `checks_rate` 1로 받았다. `query-plan-1.txt`는 단건 캡처라 첫 시도의 것을 그대로 쓴다
- 남은 위험 신호
  1. 단일 쿼리 `total_ms` 점유율이 98.36059%다 (기준 30% 이상). Phase 6에서 걸린 위험 신호 둘 중 이건 해소되지 않았다
  2. 중복 제거 임시 테이블 비중이 커졌다. 세트 A 총 18.5 ms 중 읽기+필터+조인 6.99 ms(37.8%), 중복 제거 11.51 ms(**62.2%**)다. Phase 6에서 21.3%였던 것이 인덱스로 분모가 줄면서 드러났다. `DISTINCT`는 여전히 한 행도 접지 못한다(2,735 → 2,735)
  3. `IN` 2개 이상 경로의 filesort가 남았다 (세트 B `Sort_rows` 10,246 / `Sort_merge_passes` 9)
  4. DB 밖 비중이 더 커졌다. 요청당 DB 시간 191.8663 ms 대 `waiting_ms` med 1,345.978 ms로 DB가 약 14.3%다. Phase 4의 약 22%에서 내려갔다는 뜻은 남은 85.7%가 직렬화·커넥션 대기·GC라는 것이다
- 다음 사이클 진행 여부: **계속.** 종료 조건 셋 중 어느 것도 성립하지 않는다. 하드웨어 독립 증거가 크게 움직였고(`examined_per_sent` 7.7942 → 1.9560), Phase 6 위험 신호 둘 중 풀스캔은 해소됐으나 단일 쿼리 `total_ms` 점유율 98.36059%는 남았다. 호출자가 사이클 2 진행을 선택했다

---

## 사이클 2: JPQL `DISTINCT` 제거

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 중복 제거 방식 | Hibernate의 엔티티 자동 중복 제거에 맡긴다. 명시적 장치를 두지 않는다 | **추측이 아니라 현재 동작이다.** SQL이 요청당 4563.1405행을 돌려주는데 응답 DTO는 1,498개다(학과별 강의 수 8개 균등 분포 평균 = (3,378+2,526+1,693+918+895+879+852+843)/8 = 1,498.0). 축약비 3.05가 강의당 schedule 평균 3.0190과 일치한다. SQL `DISTINCT`가 0행을 접었으므로(2,735 → 2,735) 이 축약은 전부 Hibernate가 자바에서 한 것이다. Hibernate 7.2.0.Final(`runtimeClasspath` 확인), 6.0부터 fetch join 부모 중복은 결과 목록 생성 시 자동으로 접힌다. 같은 리포지토리의 `findByIdWithSchedules`(`CourseRepository:59-65`)가 이미 `DISTINCT` 없이 `LEFT JOIN FETCH`를 쓴다 |
| 적용 범위 | `LEFT JOIN FETCH c.schedules` + `SELECT DISTINCT c` 패턴 5곳 전부 (`findByDepartment` 19, `findByDepartmentIn` 29, `findByArea` 39, `findHussCourses` 90, `findAllBySemesterWithSchedules` 111) | 같은 근거가 5곳에 그대로 적용된다. 사용처를 전부 확인해 순서·중복 의존이 없음을 확인했다 - 앞 넷은 `ORDER BY` 3컬럼이 쿼리에 있고 `stream().map().toList()`로 받는다. 다섯 번째는 `ORDER BY`가 없으나 `LinkedHashMap.put(haksuCode, course)`로 받아(`CourseSyncApplier:212-218`) 중복에 무해하고, 순서 변화의 영향은 `closeMissingCourses`에 넘어가는 `details` 나열 순서에 그친다 |
| 건드리지 않을 `DISTINCT` | `findCategories`(68), `findTerms`(76), `findDepartmentsIn`(83), `findByKeyword`(49) | 앞 셋은 스칼라 projection이라 중복 제거가 실제로 일을 한다. `findByKeyword`는 native이고 fetch join이 없다 |
| 측정과 PR 기준 | `findByDepartmentIn` 하나 | Phase 8이 덮는 것은 대상 쿼리뿐이다. 나머지 4개는 같은 근거로 함께 고치되 개선을 주장하지 않는다 |

- 검토했지만 택하지 않은 안
  - `Set<Course>` 반환으로 중복 제거 - `ORDER BY` 3컬럼의 순서 보장이 깨진다. 반환 타입이 `Set`이면 구현체가 보장되지 않아 정렬이 무의미해진다
  - 서비스단 `stream().distinct()` - Hibernate가 이미 접은 결과에 같은 일을 한 번 더 한다
  - `findByDepartmentIn` 하나만 수정 - 호출자가 5곳 전부를 택했다
  - DTO projection + `GROUP_CONCAT` (사용자 정의 함수) - 호출자가 5-B에서 대안으로 제기했으나 이번 기법의 범위 밖이다. 전제는 정확하다(`schedule` 문자열은 `CourseScheduleFormatter`가 강의실별 그룹핑 + 요일·시작시각 정렬 + `[강의실:요일(교시),...]` 조립을 하므로 중첩 `GROUP_CONCAT`이 필요하다). 추가로 걸리는 지점 셋: `is75MinLesson`도 schedules 전체를 요구하고, `DayOfWeek.getName()`은 자바 enum 표시명이라 SQL `CASE` 매핑이 필요하며, `group_concat_max_len` 기본값이 1,024 B다. 조인 폭발을 없애는 더 싼 선택지로 `Course.schedules`에 이미 붙은 `@BatchSize(size = 1000)`(`Course.java:56`)이 있다 - fetch join을 빼면 courses 1,498행과 schedules 4,563행을 곱집합 없이 따로 받는다. 사이클 3 이후 재료로 남긴다
- 호출자가 제기한 우려와 판정: "중복 제거를 Hibernate가 메모리에서 하니 오버헤드"에 대해 - 현상은 맞으나 이 기법의 비용이 아니다. `DISTINCT`가 0행을 접으므로 JDBC로 넘어오는 행 수는 `DISTINCT` 유무와 무관하게 4,563행으로 동일하다. 힙 적재량도 Hibernate가 접는 횟수도 변하지 않는다. 이 기법은 트레이드오프가 아니라 순수 제거다. 지적한 메모리 부담은 조인 폭발 자체의 비용으로 남으며 `DISTINCT`와 다른 문제다
- 호출자가 예상한 효과: 임시 테이블에 관한 비용이 사라지고 그에 따른 성능 개선이 뒤따른다. Phase 8 대조를 위해 지표로 옮긴 형태는 아래와 같다

| 구분 | 지표 | 상태 1 | 예상 |
|---|---|---|---|
| 움직인다 | `EXPLAIN`의 `Extra` | `Using temporary` | 사라짐 |
| | 중복 제거 노드 (세트 A) | 11.51 ms / 총 18.5 ms | 노드 소멸 |
| | `Handler_write` (세트 A / B) | 2,735 / 10,246 | 0 / 0 |
| | `Handler_read_rnd_next` (세트 A) | 2,736 | ≈0 (상태 1의 2,736은 전부 임시 테이블 되읽기다) |
| | 대상 쿼리 `mean_ms` / `total_ms` | 188.720802 / 370836.376959 | 감소 |
| 안 움직인다 | `rows_per_call` | 4563.1405 | 동일 |
| | 요청당 응답 크기 | 754,939 B | 동일 |
| | 요청당 쿼리 수 | SELECT 2건 | 동일 |
| | `examined_per_sent` | 1.9560 | 동일 |
| | 세트 B `Sort_rows` / `Sort_merge_passes` | 10,246 / 9 | 동일 (filesort는 `ORDER BY` 때문이라 `DISTINCT`와 별개) |

- 판정 시 유의: 세트 A의 62.2%를 `mean_ms` 감소폭으로 기대하지 마라. `EXPLAIN ANALYZE`의 18.5 ms는 부하 없는 단건 실행이고 부하 중 `mean_ms`는 188.72 ms로 10배다. 그 차이는 경합이라 임시 테이블과 무관하다. 1차 근거는 `Handler_write` → 0이고 `mean_ms`는 보조다

### 개선 전 지표

> 상태 1 = 사이클 1(`courses` 복합 인덱스) 적용 후. 출처: `k6-test-summary-1.json`, `query-stats-summary-1.md`.

| 지표 | 값 |
|---|---|
| p95 / p99 | 2619.0532 ms / 3314.4916799999987 ms |
| RPS | 16.372238821922682 (에러율 0, check 통과율 1, 요청 1965건) |
| 요청당 쿼리 수 | SELECT 2건 (트랜잭션 제어문 포함 6.9960건) |
| 대상 쿼리 calls / mean_ms / total_ms | 1965 / 188.720802 / 370836.376959 (비중 98.36059%) |
| 읽은 행 / 반환 행 (`examined_per_sent`) | 1.9560 (행/호출 4563.1405) |
| 요청당 응답 크기 | 754,939 B (`bytes_received` 1,483,456,990 / 1965) |

**실행계획**

> 원본: `query-plan-1.txt` (개선 후는 `query-plan-2.txt`). 사이클 1에서 이미 캡처해 둔 파일을 다시 뜨지 않고 그대로 읽었다.

- EXPLAIN 파라미터: 사이클 1에서 확정한 세트 A / 세트 B를 그대로 썼다 (세트 A `department IN ('COMPUTER_ENGINEERING')`, 세트 B `IN` 4개)
- 접근 방식: 세트 A `Index lookup on c1_0 using idx_department_sort` (`key_len` 202, `department`만 접근 조건), 세트 B `Index range scan` + index condition. `s1_0`은 `idx_course_id`
- 노드별 몫 (자식 시간 제외)
  - 세트 A 총 18.5 ms: 인덱스 조회 1.85(10.0%) / 필터 0.13(0.7%) / 조인 5.01(27.1%) / **임시 테이블 쓰기 10.51(56.8%)** / 되읽기 1.00(5.4%)
  - 세트 B 총 105 ms: 읽기 9.94(9.5%) / 필터 0.46(0.4%) / 조인 15.3(14.6%) / **임시 테이블 쓰기 40.2(38.3%)** / 되읽기 3.4(3.2%) / 정렬 35.7(34.0%)
- 카운터 (세트 A / 세트 B): `Handler_read_key` 3,615 / 13,628, `Handler_read_next` 3,611 / 13,621, `Handler_read_rnd_next` 2,736 / 10,247, `Handler_write` 2,735 / 10,246, `Sort_scan` 부재 / 1, `Sort_rows` 부재 / 10,246, `Sort_merge_passes` 부재 / 9
- 임시 테이블의 소재 (일회성 조회, 파일로 남기지 않음): `Created_tmp_tables` 1 / 1, **`Created_tmp_disk_tables` 0 / 0**, `Created_tmp_files` 0 / 3.
  두 세트 다 디스크로 넘어가지 않았다. 세트 A의 10.51 ms는 파일 I/O가 섞이지 않은 순수 메모리 작업이다(2,735행 x 36컬럼 복사 + 키 계산 + 인덱스 삽입).
  세트 B의 `Created_tmp_files` 3은 임시 테이블이 아니라 filesort의 머지 파일이며 `Sort_merge_passes` 9와 짝이다. 이번 기법으로 남는다
- 확정된 해석 (호출자 판정): **임시 테이블 쓰기가 최대 비용 노드다.** 세트 A 56.8%, 세트 B 38.3%로 단일 노드 최대이며 세트 B에서는 정렬(34.0%)보다도 크다.
  `Handler_write` 2,735가 `Nested loop`가 낸 2,735행과 같아 한 행도 걸러지지 않았다. select 목록에 `s1_0.id`가 있어 같은 키가 나올 수 없기 때문이다.
  `actual time=17.5..17.5`로 첫 행과 마지막 행 시각이 같은 블로킹 노드라, 상위 `Table scan on <temporary>`가 같은 데이터를 처음부터 다시 읽는다
- 위험 신호 대조
  - **단일 쿼리 `total_ms` 점유율 98.36059%** (기준 30% 이상) - 사이클 1에서 해소되지 않은 채 잔존
  - **추정 대 실측 행 수 10배 이상 괴리** (기준 10배) - 상태 0에 없던 신규 신호다. 세트 A `Filter` 87.9 / 879 = 10.0배, `Nested loop` 246 / 2,735 = 11.1배.
    원인은 `filtered: 10.00` 하나다. 옵티마이저가 `status = 'ACTIVE'`의 선택도를 10%로 가정하는데 실제로는 전 행이 `ACTIVE`라 100%다(879 x 0.10 = 87.9로 정확히 10배).
    사이클 1에서 `status`를 인덱스에서 뺐고 히스토그램도 없어 MySQL이 기본 추정치를 쓴다.
    다만 계획을 뒤집지는 않았다 - 테이블이 둘뿐이고 `LEFT JOIN`이라 조인 순서 선택지가 없으며, 과소 추정이 임시 테이블 크기 예측을 246행으로 낮췄으나 `Created_tmp_disk_tables` 0이라 실해가 없었다. 이번 기법으로 해당 노드가 사라진다
  - 걸리지 않는 것: `examined_per_sent` 1.9560, 요청당 호출 1.0000, `courses` 풀스캔 부재, `Sort_rows` 10,246 = 반환 행 수(초과 아님)
### 적용 내용

- `src/main/java/uss/code/course/repository/CourseRepository.java` 한 파일. `SELECT DISTINCT c` -> `SELECT c`로 5곳(19, 29, 39, 90, 111)을 바꿨다. **마이그레이션 없음, 자바 로직 변경 없음.** 스키마도 엔티티도 그대로다
- 건드리지 않은 `DISTINCT` 4곳을 그대로 두었음을 확인했다 - `findByKeyword`(49, native + fetch join 없음), `findCategories`(68), `findTerms`(76), `findDepartmentsIn`(83, 셋 다 스칼라 projection이라 중복 제거가 실제로 일을 한다)
- 테스트: `./gradlew test` BUILD SUCCESSFUL (32s), 실패 0건.
  **이 통과가 설계의 핵심 가정을 검증한다.** `CourseServiceTest`의 `컴퓨터공학부_학생이_전공과목을_조회하면_성공한다`가 `hasSize(6)`을,
  `전학년_1학년_2학년_순서로_정렬되어_조회된다`가 `containsExactly` 6건을 검증하는데 픽스처의 COM001은 schedule이 2개다(`[07-401:월(1-2A),수(1-2A)]`).
  `DISTINCT` 없이도 6건이 나왔다는 것은 Hibernate 7.2가 fetch join 부모 중복을 자바에서 접었다는 관측 증거다. 5-B에서 근거로만 들었던 것이 여기서 확인됐다
- 적용 확인: 재기동 후 요청 1건을 보내 digest 원문을 확인했다. `SELECT DISTINCTROW c1_0 . id ...` -> **`SELECT c1_0 . id , c1_0 . academic_yea...`**
  로 바뀌어 새 코드가 응답 중임을 확인했다. 코드 변경이라 `EXPLAIN`이 아니라 Hibernate가 실제로 내보내는 SQL로 확인했다
- 재기동 완료. 토큰은 재발급하지 않는다 (`tokens.json` 만료 2026-08-29 16:48:56, 유효)

### 개선 후 지표

| 구분 | 지표 | 개선 전 | 개선 후 | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 2619.0532 ms | 2187.408 ms | -16.5% |
| | p99 | 3314.4916799999987 ms | 2805.6815300000003 ms | -15.3% |
| | RPS | 16.372238821922682 | 19.13123181488946 | +16.9% |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 2건 (전체 6.9960) | SELECT 2건 (전체 6.9939) | 동일 |
| | 대상 쿼리 total_ms | 370836.376959 | 145326.547837 | **-60.8%** (mean_ms 188.720802 -> 63.185455, -66.5%) |
| | `total_ms` 점유율 | 98.36059% | 96.9232% | -1.44%p |
| | 읽은 행 / 반환 행 | 1.9560 | 1.5351 | **-21.5%** |
| | 행/호출 | 4563.1405 | 4570.2709 | +0.16% |
| | 요청당 응답 크기 | 754,939 B | 755,470.9222415291 B | +0.07% |
| | 접근 방식과 인덱스 | `Index lookup using idx_department_sort` + 중복 제거 임시 테이블 | 인덱스 접근 동일, **임시 테이블 소멸** | 노드 소멸 |
| | `Handler_write` (A / B) | 2,735 / 10,246 | **0 / 0** | 소멸 |
| | `Handler_read_rnd_next` (A / B) | 2,736 / 10,247 | **0 / 0** | 소멸 |
| | `Handler_read_key` (A / B) | 3,615 / 13,628 | 880 / 3,382 | -2,735 / -10,246 |
| | 세트 B `Sort_rows` / `Sort_merge_passes` | 10,246 / 9 | 3,378 / 4 | -67.0% / -55.6% |
| | 캐시 hit / miss, 적중률 (캐싱 사이클만, 아니면 `-`) | - | - | - |

**실행계획 (상태 2)** - 원본 `query-plan-2.txt`. 파라미터는 세트 A / 세트 B 동일, 쿼리에서 `DISTINCTROW`만 뺐다.

- 세트 A: 구조는 그대로고 위 두 노드만 사라졌다. 총 18.5 ms -> **6.76 ms (-63.5%)**.
  노드별 몫: 인덱스 조회 1.87 / 필터 0.12 / 조인 4.77. 남은 세 노드 합이 6.99 -> 6.76으로 사실상 불변이므로 줄어든 11.74 ms는 전부 임시 테이블 몫이다.
  `EXPLAIN FORMAT=JSON`에서 `duplicates_removal` 블록과 `using_temporary_table`이 통째로 사라졌다
- 세트 B: **계획 구조가 바뀌었다.** 상태 1 `Sort(10,246) <- Table scan<temp> <- Temp dedup <- Nested loop`에서
  상태 2 `Nested loop <- Sort(3,378) <- Filter <- Range scan`으로 **정렬이 조인 아래로 내려갔다.** 총 105 ms -> **41.1 ms (-60.9%)**.
  노드별 몫: 범위 스캔 10.4 / 필터 0.4 / 정렬 15.6 / 조인 14.7
- `Handler_read_key` 감소분이 임시 테이블 유니크 키 탐색 횟수와 정확히 일치한다: 세트 A 3,615 - 880 = 2,735, 세트 B 13,628 - 3,382 = 10,246
- 추정 대 실측: 세트 A는 `Filter` 87.9 / 879 = **10.0배**로 잔존(`status`의 `filtered: 10.00`), `Nested loop` 296 / 2,735 = 9.24배.
  세트 B는 `Filter`와 `Range scan`이 3,378 / 3,378, `Nested loop` 11,362 / 10,246으로 **괴리가 사라졌다**

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): **있음.** 호출자 판정 "기법 효과 맞음".
  `Handler_write` 2,735 / 10,246 -> 0 / 0, `Handler_read_rnd_next` 2,736 / 10,247 -> 0 / 0으로 임시 테이블 관련 카운터가 전부 소멸했다.
  셋 다 타이머가 아니라 행 수 카운터라 부하 조건과 머신 상태에 흔들리지 않는다. `EXPLAIN FORMAT=JSON`에서 `duplicates_removal` 블록이 사라져 실행계획으로도 확인된다.
  요청당 쿼리 수가 그대로이므로 쿼리를 줄여서 얻은 것이 아니고, `rows_per_call`(+0.16%)과 응답 크기(+0.07%)가 그대로이므로 응답을 줄여서 얻은 것도 아니다
- 5-B 예상과 대조: 예상 10개 중 8개 적중(`Using temporary` 소멸, 중복 제거 노드 소멸, `Handler_write` 0, `Handler_read_rnd_next` ≈0, `mean_ms` 감소, `rows_per_call` 동일, 응답 크기 동일, 요청당 쿼리 수 동일).
  **2개는 어긋났고 둘 다 예상보다 좋은 쪽이다.**
  1. 세트 B `Sort_rows` 10,246 -> 3,378, `Sort_merge_passes` 9 -> 4 (예상: 동일). 틀린 가정은 "filesort는 `ORDER BY` 때문이라 `DISTINCT`와 별개"였다.
     정렬의 **존재**는 별개가 맞았으나 정렬의 **위치와 대상 행 수**는 `DISTINCT`에 묶여 있었다.
     `DISTINCT`가 있는 동안에는 중복 제거를 조인 결과 전체에 대해 끝낸 뒤에야 정렬할 수 있어 10,246행을 정렬했는데,
     사라지자 옵티마이저가 정렬을 조인 아래로 내려 `courses` 쪽 3,378행만 정렬한다. Nested loop이 바깥 순서를 보존하므로 결과 순서는 같다
  2. `examined_per_sent` 1.9560 -> 1.5351 (예상: 동일). `ROWS_EXAMINED`에 임시 테이블 되읽기가 포함돼 있었고 그것이 빠진 것으로 보인다.
     다만 부하 평균이라 세트 A/B 실행계획 수치와 1:1로 맞춰 검산하지는 못했다
- 측정 이력: 재측정 없이 1회로 끝났다. `checks_rate` 1, `failed_rate` 0.
  요청 수가 1,965 -> 2,302로 늘었는데 이는 처리량 상승의 결과이지 조건 변경이 아니다. `total_ms` -60.8%는 호출 수가 17% 늘어난 상태에서 나온 값이다
- 남은 위험 신호
  1. 단일 쿼리 `total_ms` 점유율이 96.9232%다 (기준 30% 이상). 98.36059%에서 1.44%p 내렸을 뿐 해소되지 않았다
  2. 세트 A의 추정 대 실측 10.0배 괴리가 남았다 (`Filter` 87.9 / 879). 원인은 `status = 'ACTIVE'`의 `filtered: 10.00`이며 전 행이 `ACTIVE`라 실제로는 100%다.
     세트 B에서는 사라졌다. 계획을 뒤집지는 않는다
  3. 조인 폭발은 그대로다. SQL이 요청당 4,570.2709행을 돌려주고 Hibernate가 1,498개로 접는다. 힙 적재와 직렬화 부담은 이 사이클이 건드리지 않았다
  4. DB 밖 비중이 또 커졌다. 요청당 DB 시간 약 64.6 ms 대 `waiting_ms` med 1,135.2585 ms로 DB가 약 5.7%다.
     Phase 4의 약 22%, 사이클 1의 약 14.3%에서 계속 내려간다. 남은 94.3%의 정체(직렬화, 커넥션 대기, GC)는 여전히 미확정이다
- 다음 사이클 진행 여부: **계속.** 종료 조건 셋 중 어느 것도 성립하지 않는다. 하드웨어 독립 증거가 크게 움직였고, Phase 6 위험 신호 둘 중 `total_ms` 점유율 96.9232%는 잔존하며 추정 대 실측 괴리도 세트 A에 10.0배로 남았다. 호출자가 사이클 3 진행을 선택했다

---

## 사이클 3: fetch join 제거 + `@BatchSize` 활용

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용.

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 적용 범위 | `LEFT JOIN FETCH c.schedules`를 쓰는 6곳 중 조회 API 4곳: `findByDepartment`(18), `findByDepartmentIn`(28), `findByArea`(38), `findHussCourses`(89). JPQL에서 `LEFT JOIN FETCH c.schedules` 한 줄씩 삭제 | 넷 다 `CourseService`의 `@Transactional(readOnly = true)` 메서드(47, 66, 95, 119행) 안에서 같은 DTO 변환 경로로 `getSchedules()`를 호출하므로 배치 로딩이 같은 방식으로 작동한다. `open-in-view: false`여도 변환이 트랜잭션 안이라 `LazyInitializationException` 위험이 없다. 호출자 판단: "비슷한 류의 쿼리라 특정 쿼리에서 측정 이상이 나타날 것 같지 않다. 추후 한번 쭉 점검할 예정이라 그때 개선한다" |
| 제외한 곳 | `findByIdWithSchedules`(59, `CartService:63`), `findAllBySemesterWithSchedules`(110, `CourseSyncApplier:215`) | 문제 형태가 다르다. 전자는 강의 1건이라 조인 폭발이 없고 fetch join이 쿼리 1회로 가장 싼 경로다(빼면 쿼리만 +1). 후자는 `replaceSchedules`·orphanRemoval이 걸린 쓰기 경로라 이 사이클의 측정이 덮지 않는다 |
| batch size | 1000 유지 (`Course.java:55`, 변경 없음) | 요청당 강의 수 843~3,378 분포에서 부하 회원의 62.5%(IN 1개 학과)가 schedules 쿼리 1회, 최대 4회(3,378). 호출자 근거: 100~1000 범위에서 오버헤드가 크지 않다는 학습 자료. MySQL 쪽 정확성 제약은 없음(`IN` 길이 제한 없음, `eq_range_index_dive_limit` 200 초과는 추정치 문제, `range_optimizer_max_mem_size` 8 MB 여유). `findByKeyword`도 같은 값을 타므로 값을 바꾸면 검색 경로도 같이 바뀐다는 점을 확인하고 유지 |
| DTO projection 결합 | 이번 사이클에서 제외 | projection 결과는 영속성 컨텍스트에 안 들어가 `@BatchSize`가 작동하지 않는다 — 이 기법의 전제를 없앤다. 결합하려면 schedules를 `course_id IN` 직접 조회 + 자바 그룹핑 코드가 필요하다. 사이클 4 이후 후보로 남기되 이 조건을 붙인다 |
| 컬렉션 초기화 방식 | A. 암묵적 — `MajorCourseResponse.from` → `CourseScheduleFormatter.format(course.getSchedules())` 첫 접근 시 배치 로딩 | `findByKeyword`(native, fetch join 없음)가 지금 정확히 이 방식으로 운영 경로에서 돈다. 새 동작이 아니다. 명시적 초기화(`Hibernate.initialize` 루프)는 SQL이 같고 서비스 코드와 `org.hibernate` 의존만 는다 |
| 측정과 PR 기준 | `findByDepartmentIn` 하나 + 신규 schedules 배치 쿼리 | Phase 8이 덮는 것은 대상 경로뿐이다. 나머지 3곳은 같은 근거로 함께 고치되 개선을 주장하지 않는다 |

- 기법 적용 후 예상되는 쿼리 형태 (Phase 7에서 p6spy로 실물 확인)
  1. `SELECT c.* FROM courses WHERE department IN (...) AND status = ? ORDER BY grade_code, classification_code, haksu_code` — 28컬럼, 세트 A 879행 / 세트 B 3,378행
  2. `SELECT s.* FROM course_schedules WHERE course_id IN (?×1000)` — 8컬럼, `idx_course_id`(V1_0 FK 인덱스) 사용. Hibernate 7.2 MySQL 방언은 배열 파라미터 미지원이라 `IN` 목록이며, 슬롯 수를 batch size로 고정하고 남는 자리를 `null`로 채우는 것으로 추정. 세트 A 1회(2,735행) / 세트 B 4회(합 10,246행)
  - JDBC로 넘어오는 값의 수(세트 A): 4,570행 × 36컬럼 = 98,460 → 879 × 28 + 2,735 × 8 = 46,492 (약 -53%)
- 검토했지만 택하지 않은 안
  - 6곳 전부 제거 — 호출자가 처음 제시했으나 `findByIdWithSchedules`, `findAllBySemesterWithSchedules`는 위 근거로 스스로 제외했다
  - batch size 변경 — 관측 분포에서 1000이 요청당 쿼리 수를 3~6에 묶는다. 바꿀 근거가 없다
  - DTO projection 결합 — `@BatchSize` 전제 상실, 사이클 분리 원칙 위배
  - 명시적 초기화(B) — SQL 동일, 코드만 는다
- 해당 없는 항목: 페이징 충돌(페이징 없음), 중복 제거(컬렉션 조인이 사라져 부모 중복 자체가 발생하지 않음 — 사이클 2의 Hibernate 자동 중복 제거도 할 일이 없어진다)
- 호출자가 예상한 효과: "요청당 쿼리 수는 늘고, 요청당 응답 크기는 고정. 실행계획은 줄어든다. 나머지는 DB 왕복 비용과 절감 비용을 저울질해야 해서 측정해봐야 안다." 호출자가 모르겠다고 한 지표는 스킬이 근거와 함께 채웠다

| 구분 | 지표 | 상태 2 | 예상 | 근거 / 출처 |
|---|---|---|---|---|
| 움직인다 | 요청당 쿼리 수 | SELECT 2건 | 3~6건, 부하 평균 3.75건 | 호출자 예상(증가). schedules 쿼리 = ⌈강의 수/1000⌉, 8개 학과 (1,1,1,1,1,2,3,4) 평균 1.75건 |
| | 대상 쿼리 `rows_per_call` | 4570.2709 | ≈1,498 | courses 행만 돌아온다(강의 수 평균) |
| | 신규 schedules 쿼리 `rows_per_call` | - | ≈2,611 | 4,570.27 / 1.75 |
| | 세트 A 실행계획 | 총 6.76 ms (인덱스 조회 1.87 / 필터 0.12 / 조인 4.77) | 조인 노드 소멸, courses 쿼리 ≈2 ms. schedules 쿼리는 `idx_course_id` 범위 스캔 879구간 → 2,735행 | 호출자 예상(축소). 조인 노드 = 행마다 `course_schedules`를 찾던 비용이 IN 범위 스캔으로 옮겨간다 |
| | 세트 B 실행계획 | 총 41.1 ms (범위 스캔 10.4 / 필터 0.4 / 정렬 15.6 / 조인 14.7) | 조인 노드 14.7 소멸. 정렬 15.6 / `Sort_rows` 3,378은 그대로 | 정렬은 courses 쪽에만 걸려 있고 이미 조인 아래에 있다 |
| | `examined_per_sent` | 1.5351 | 두 쿼리 모두 ≈1.0 | 각 쿼리가 필요한 행만 읽고 그대로 보낸다 |
| | 단일 쿼리 `total_ms` 점유율 | 96.9232% | 하락 | 일이 두 digest로 나뉜 결과이지 일이 준 게 아니다. **Phase 8 판정은 두 쿼리 합으로 본다** |
| 안 움직인다 | 요청당 응답 크기 | 755,470.9222415291 B | 동일 | 호출자 예상(고정). DTO 내용이 같다 |
| | 결과 순서 | `ORDER BY` 3컬럼 | 동일 | courses 쿼리에 `ORDER BY`가 남는다 |
| | `Handler_read_key` 합계 (A / B) | 880 / 3,382 | 두 쿼리 합이 거의 동일 | 지금의 879는 행마다 하는 `course_id` 탐색이고, 분리 후 IN 범위 879구간 탐색으로 바뀔 뿐 InnoDB가 만지는 행은 같다. 이 기법은 DB가 읽는 양이 아니라 DB가 만들어 보내는 행의 폭을 줄인다 |
| 측정해야 안다 | DB 시간 합계 (두 쿼리 `total_ms` 합) | 145326.547837 | 미확정 | 조인 소멸(-) 대 왕복 +1.75회와 1,000슬롯 IN 파싱(+) |
| | 요청당 힙 할당, 응답시간 | - / med 1,135.2585 ms | 미확정 | JDBC 값 수 -53%가 얼마나 돌아오는지는 actuator 없이 못 가른다 |

- 판정 시 유의: 점유율 하락과 대상 쿼리 `mean_ms` 하락을 개선 증거로 쓰지 마라. 둘 다 쿼리를 쪼갠 것만으로 생긴다. 1차 근거는 (1) 조인 노드 소멸과 (2) 두 쿼리 `rows_per_call` 합 대 상태 2의 4,570.27, (3) 두 쿼리 `total_ms` 합 대 145326.547837이다


### 개선 전 지표

> 상태 2 = 사이클 2 적용 후. `k6-test-summary-2.json`, `query-stats-summary-2.md`, `query-plan-2.txt`에서 옮겼다. 대상 쿼리가 상태 2와 같으므로 다시 뜨지 않았다.

| 구분 | 지표 | 값 |
|---|---|---|
| 하드웨어 의존 | p95 / p99 | 2187.408 / 2805.6815300000003 ms |
| | RPS | 19.13123181488946 |
| | 에러율 / check 통과율 | 0 / 1 (2,302건) |
| | med / waiting med | 1147.598 / 1135.2585 ms |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 2건 (`findById` 0.9991, `findByDepartmentIn` 0.9991), 제어문 포함 6.9939 |
| | 대상 쿼리 `calls` / `mean_ms` / `total_ms` | 2300 / 63.185455 / 145326.547837 (점유율 96.9232%) |
| | 대상 쿼리 `rows_per_call` / `examined_per_sent` | 4570.2709 / 1.5351 |
| | 요청당 응답 크기 | 755,470.9222415291 B |
| 로직 개선용 추가 캡처 | 호출 스택 | `CourseController.getMajorCourses` → `CourseService.getMajorCourses` (`@Transactional(readOnly = true)`) → `MemberRepository.findById` → `CourseRepository.findByDepartmentIn` → `MajorCourseResponse.from` × 1,498. `getSchedules()`는 fetch join으로 이미 초기화돼 `@BatchSize`가 작동하지 않는다 |

**실행계획 (상태 2)** - 원본 `query-plan-2.txt`. 파라미터는 사이클 1·2와 동일: 세트 A `department = 'COMPUTER_ENGINEERING'` (IN 1개, 879강의, 흔한 쪽 - 부하 회원의 62.5%가 IN 1개), 세트 B `ELECTRONICS_ENGINEERING_SCHOOL` 소속 4개 학과 (IN 4개, 3,378강의, 드문 쪽 - 최대 조회량).

- 세트 A 총 6.76 ms. 노드 자체 몫(자식 시간 제외): Nested loop 4.77 (70.6%) / Index lookup `c1_0` (`idx_department_sort`, ref) 1.87 (27.7%) / Filter `status` 0.12 (1.8%).
  `s1_0`은 `idx_course_id` ref로 loops 879, 총 2,735행. 카운터 `Handler_read_key` 880, `Handler_read_next` 3,611, `Handler_read_rnd_next` / `Handler_write` / `Sort_*` 0
- 세트 B 총 41.1 ms. 노드 자체 몫: Sort 15.6 (38.0%) / Nested loop 14.7 (35.8%) / Index range scan `c1_0` (4구간) 10.4 (25.3%) / Filter `status` 0.4 (1.0%).
  `s1_0`은 loops 3,378, 총 10,246행. 카운터 `Handler_read_key` 3,382, `Handler_read_next` 13,621, `Sort_scan` 1 / `Sort_rows` 3,378 / `Sort_merge_passes` 4, `Handler_read_rnd_next` / `Handler_write` 0
- 호출자 해석: "둘 다 조인과 필터가 가장 많이 먹는다". 판정: 조인은 맞다(세트 A 1위, 세트 B 2위). 필터는 어긋난다 - `actual time`이 자식을 포함하므로 세트 A Filter 1.99 중 1.87은 자식 Index lookup 몫이고 자체는 0.12다.
  필터가 눈에 띄는 것은 시간이 아니라 추정 괴리(세트 A 87.9 대 879)다. 세트 B의 1위인 Sort 15.6은 호출자가 놓쳤다
- **확정 해석**: 세트 A는 조인(4.77)이, 세트 B는 정렬(15.6)과 조인(14.7)이 비용을 먹는다. 필터는 두 세트 모두 무시할 수준이다.
  이 사이클의 기법은 조인 노드만 없애므로 세트 B의 Sort는 그대로 남는다
- 위험 신호 (사이클 2 판정에서 이월): 단일 쿼리 `total_ms` 점유율 96.9232% (30% 초과), 세트 A 추정 대 실측 10.0배 (`status` `filtered: 10.00`), 세트 B `Sort_rows` 3,378 = 반환 courses 행 수 (인덱스로 정렬을 못 풂).
  `examined_per_sent` 1.5351, 풀스캔 없음, 요청당 호출 1회는 정상 범위

### 적용 내용

- `src/main/java/uss/code/course/repository/CourseRepository.java` 한 파일. `findByDepartment`, `findByDepartmentIn`, `findByArea`, `findHussCourses`에서 `LEFT JOIN FETCH c.schedules` 4줄을 지웠다.
  **마이그레이션 없음, 엔티티·서비스 변경 없음.** `Course.schedules`의 `@BatchSize(size = 1000)`(`Course.java:55`)은 그대로다
- 유지한 fetch join 2곳을 확인했다 - `findByIdWithSchedules`(59행), `findAllBySemesterWithSchedules`(109행)
- 테스트: `./gradlew test` BUILD SUCCESSFUL (33s), 290건 통과, 실패 0, 에러 0.
  **이 통과가 설계의 핵심 가정을 검증한다.** `CourseServiceTest:194`의 `schedule()` 문자열 검증(`[07-401:월(1-2A),수(1-2A)]`)과 `:201`이 통과했다는 것은
  fetch join 없이 `@Transactional(readOnly = true)` 안에서 지연 로딩으로 schedules가 채워졌다는 관측 증거다(H2)
- 적용 확인: 재기동 후 회원 900001(`COMPUTER_ENGINEERING`, 세트 A와 같은 값)로 요청 1건을 보내 digest를 확인했다. p6spy는 perf 프로파일에서 꺼져 있다(`application-perf.yml:17`).
  `courses`·`course_schedules`를 읽은 digest가 **두 줄**로 나왔다
  1. `SELECT c1_0.id, ... (28컬럼) FROM courses c1_0 WHERE c1_0.department IN (...) AND c1_0.status = ? ORDER BY c1_0.grade_code, c1_0.classification_code, c1_0.haksu_code` - calls 1, rows_sent **879**. 조인 없음
  2. `SELECT s1_0.course_id, s1_0.id, s1_0.classroom, s1_0.day_of_week, s1_0.end_time, s1_0.period_code, s1_0.period_name, s1_0.start_time FROM course_schedules s1_0 WHERE s1_0.course_id IN (...)` - calls 1, rows_sent **2,732**. `@BatchSize` 배치 쿼리 1회
- 덤으로 확인된 사실
  1. 2,732 + 3 = 2,735. 상태 2의 세트 A LEFT JOIN 행 수 2,735는 schedule이 없는 강의 3건의 NULL 행을 포함한 값이었다
  2. courses 쿼리의 select 목록이 36 → 28컬럼으로 줄면서 digest 원문이 더는 잘리지 않는다. `WHERE`와 `ORDER BY`가 온전히 남는다(사이클 1·2의 966자 절단 해소). `max_digest_length` 변경은 필요 없어졌다
- 재기동 완료. 토큰은 재발급하지 않는다 (`tokens.json` 만료 2026-08-29 16:48:56, 유효)

### 개선 후 지표

> 상태 3. `k6-test-summary-3.json`, `query-stats-summary-3.md`, `query-plan-3.txt`. 측정 조건은 상태 2와 동일(VU 30 / 2m / USER_COUNT 1000 / 풀 10 / warm). `failed_rate` 0, `checks_rate` 1 (2,567건).

| 구분 | 지표 | 개선 전 (상태 2) | 개선 후 (상태 3) | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 2187.408 ms | 2110.25 ms | -3.5% |
| | p99 | 2805.6815300000003 ms | 2602.2771199999997 ms | -7.2% |
| | med / waiting med | 1147.598 / 1135.2585 ms | 1028.616 / 1018.409 ms | -10.4% / -10.3% |
| | RPS | 19.13123181488946 | 21.370335865922097 | +11.7% (요청 2,302 → 2,567) |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 2건 (전체 6.9939) | SELECT 3종 3.7479건 (전체 8.7544): `courses` 0.9996 + `course_schedules` 1.7495 + `members` 0.9988 | +1.7495 |
| | `rows_per_call` | 4570.2709 (조인 1쿼리) | `courses` 1498.5078 / `course_schedules` 2609.2532 | 조인 폭발 소멸 |
| | 요청당 JDBC 값 수 (행 × 컬럼) | ≈164,530 (4,570 × 36) | ≈78,460 (1,498 × 28 + 4,565 × 8) | **-52.3%** |
| | `examined_per_sent` | 1.5351 | `courses` 1.6338 / `course_schedules` 1.0000 | 아래 판정 참조 |
| | `total_ms` | 145326.547837 (calls 2300) | `courses` 56947.172354 + `course_schedules` 73491.63137 = 130438.803724 (calls 2566 + 4491) | 합 -10.2% (호출 +11.6% 상태에서) |
| | 요청당 DB 시간 | 63.13 ms | 50.81 ms (22.19 + 1.7495 × 16.36) | -19.5%. 응답시간 대비 5.7% → 5.0% |
| | `total_ms` 점유율 | 96.9232% (단일) | 42.0369% + 54.2496% = 96.2865% (합) | -0.64%p |
| | 요청당 응답 크기 | 755,470.9222415291 B | 755,702.8079470198 B | +0.03% |
| | 접근 방식과 인덱스 | Nested loop: `idx_department_sort` ref/range → `idx_course_id` ref × loops | `courses`: `idx_department_sort` 동일. `course_schedules`: `idx_course_id` range scan (IN 구간 879 / 1,000) | **Nested loop 노드 소멸** |
| | `Handler_read_key` (A) | 880 | 1 + 879 = 880 | 동일 |
| | `Handler_read_next` (A) | 3,611 | 879 + 2,732 = 3,611 | 동일 |
| | 세트 B `Sort_rows` / `Sort_merge_passes` | 3,378 / 4 | 3,378 / 4 | 동일 |
| | 캐시 hit / miss, 적중률 (캐싱 사이클만, 아니면 `-`) | - | - | - |

- 신규 digest 2종: `SET character_set_results = ?`, `SELECT @@SESSION.transaction_read_only` (각 calls 6, 요청당 0.0023). Connector/J 커넥션 초기화문으로 측정 중 커넥션 6개가 새로 열렸다는 뜻이며 이 기법과 무관하다

**실행계획 (상태 3)** - 원본 `query-plan-3.txt`. 파라미터는 Phase 6과 동일. 대상이 두 쿼리라 세트당 2개, 총 4개를 캡처했다. schedules 쿼리의 `IN` 목록은 그 세트의 courses 결과 id를 결과 순서로 넣었고, 세트 B는 첫 배치 1,000개다.

- 세트 A: courses 1.8 ms (Filter 자체 0.13 / Index lookup `idx_department_sort` 1.67, `using_filesort: false`) + schedules 4.41 ms (`idx_course_id` range scan 879구간, 추정 1,758 / 실측 2,732) = **6.21 ms** (상태 2 조인 1쿼리 6.76, -8.1%).
  조인 노드 자체 몫 4.77이 range scan 4.41로 옮겨갔을 뿐 DB 작업량은 거의 그대로다 - `Handler_read_key` 880, `Handler_read_next` 3,611이 상태 2와 정확히 일치한다
- 세트 B: courses 25.7 ms (Sort 자체 15.2 / Filter 0.4 / range scan 10.1, `Sort_rows` 3,378 / `Sort_merge_passes` 4) + schedules 첫 배치 4.88 ms (1,000구간, 추정 2,000 / 실측 3,159).
  세트 B는 배치 4회(1,000 / 1,000 / 1,000 / 378)이고 첫 배치만 캡처했으므로 상태 2의 41.1 ms와 1:1 비교가 아니다. 첫 배치 비례로 어림하면 schedules 합 ≈16 ms, 총 ≈42 ms로 상태 2와 비슷하다
- 추정 대 실측: courses 세트 A `Filter` 87.9 / 879 = 10.0배 잔존 (`status` `filtered: 10.00`). schedules는 `idx_course_id` 통계가 구간당 2행으로 잡아 1.55배(A) / 1.58배(B) 과소이나 10배 기준 미만

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): **있음.** 호출자 판정 "기법의 효과". 근거는 결정론적으로 바뀐 셋 - 쿼리 구조(1 → 2종), `rows_per_call` 4570.2709 → 1498.5078 + 2609.2532, 실행계획에서 Nested loop 노드 소멸.
  p95 -3.5% / RPS +11.7%는 실행 간 편차 28% 안이라 근거로 쓰지 않는다.
  이 기법이 줄인 것은 DB 작업이 아니라 **전송 폭과 Hibernate 중복 접기**다 - InnoDB 카운터(`Handler_read_key` 880, `read_next` 3,611)는 상태 2와 동일하고, JDBC 값 수 -52.3%, Hibernate가 4,570행을 1,498개로 접던 일이 사라졌다. 요청당 DB 시간 -19.5%(63.13 → 50.81 ms)는 타이머라 보조 근거다
- 5-B 예상과 대조: 예상 9개 중 8개 적중(요청당 쿼리 수 3.7479 대 3.75, courses 1498.5078 대 ≈1,498, schedules 2609.2532 대 ≈2,611, 세트 A/B 조인 소멸, 세트 B Sort 잔존, 단일 점유율 하락, 응답 크기 동일, `Handler_read_key` 합 동일).
  **1개 어긋남**: courses `examined_per_sent` 1.6338 (예상 ≈1.0). 틀린 가정은 "courses 쿼리는 필요한 행만 읽는다"였다. filesort가 정렬 대상 행을 다시 읽는 것이 `ROWS_EXAMINED`에 잡힌다.
  IN ≥ 2인 3개 학과(1,693 + 2,526 + 3,378 = 7,597행)는 examined 2배, IN 1개 5개 학과(4,387행)는 1배 → (4,387 + 15,194) / 11,984 = 1.6339로 실측과 일치한다.
  상태 2에도 있던 되읽기다(상태 2 요청당 examined 7,016 = courses 1,498 + 정렬 되읽기 950 + schedules 4,570으로 검산). 새 비용이 아니라 세트 B `Sort_rows` 3,378과 같은 신호다
- 측정 이력: 재측정 없이 1회로 끝났다. `checks_rate` 1, `failed_rate` 0. 요청 수 2,302 → 2,567은 처리량 상승의 결과이지 조건 변경이 아니다
- 남은 위험 신호
  1. 두 쿼리 합 점유율 96.2865% (기준 30% 이상). 이 엔드포인트가 하는 일이 이 조회뿐이라 구조적으로 남는다. 단일 쿼리 기준으로는 42.0369% / 54.2496%로 나뉘었으나 일이 준 것이 아니다
  2. 세트 A 추정 대 실측 10.0배 괴리 잔존 (`status` `filtered: 10.00`). 세트 B에는 없다
  3. 세트 B `Sort_rows` 3,378 잔존. courses `examined_per_sent` 1.6338의 정체가 이것이다. 적용 범위는 IN ≥ 2인 학과(부하 회원 37.5%)
  4. DB 밖 비중 95.0% (50.81 / 1,018.409). Phase 4 약 22% → 14.3% → 5.7% → 5.0%. 정체(직렬화, 커넥션 대기, GC)는 여전히 미확정. 측정 중 커넥션 6개가 새로 열린 흔적(초기화문 digest)은 있으나 대기 시간은 재지 않았다
  - 해소: 조인 폭발(사이클 2 위험 신호 3번). SQL이 요청당 4,570행을 돌려주고 Hibernate가 1,498개로 접던 구조가 사라졌다
- 다음 사이클 진행 여부: **판정은 계속.** 종료 조건 셋 중 앞의 둘은 성립하지 않는다(하드웨어 독립 증거가 바뀌었고 위험 신호 4개 잔존). 호출자 선택 대기



## 사이클 4: 정적 부분 Redis 캐싱 + `isRegisterable` 라이브 조립

### 설계 결정

> Phase 5-B에서 호출자와 확정한 내용. 5-A에서 호출자가 캐싱(6번 후보)을 골랐고, 전략의 형태(전체 캐싱 + 무효화가 아니라 정적 부분 캐싱 + 라이브 조립)는 호출자가 제안했다.
> 2026-08-30 Phase 7 도중 호출자와 다시 협의해 락·적재·TTL·장애 항목을 뒤집었다. 뒤집은 근거는 각 행과 **검토했지만 택하지 않은 안**에 있다.

**도입 근거** (호출자가 "Redis 적용, 성능 개선 끝"이 아니라 서비스 특성에서 출발한 근거를 요구해 정리했다)

- 이 캐시가 아끼는 것은 응답시간이 아니라 **원본 DB의 요청당 작업량**이다. 상태 3에서 DB 시간은 응답시간의 5.0%뿐이지만, 요청 하나가 DB에 시키는 일은 1,498행 인덱스 조회 + 3,378행 정렬(세트 B) + 4,565행 범위 스캔 + 78,460값 전송 + 엔티티 6,063개 하이드레이션이다. 적중 시 이 중 3컬럼 1,498행만 남는다(값 -94%). 즉 근거는 "같은 DB로 더 많은 동시 사용자를 받는다"(피크 수용량)이고, 짧고 높은 피크에 전 학과가 같은 대용량 정적 목록을 반복 조회하는 수강신청의 성격에 맞는다. 수치는 Phase 8이 준다
- 사전 적재까지 하면 Redis는 지연 캐시가 아니라 **물질화된 읽기 모델**이다. 원본과 다른 점은 빠름이 아니라 담긴 형태다 - 이미 조인·정렬·포맷된 응답 조각(`schedule` 문자열까지)을 키 1개 GET으로 읽고, 진실의 원천이 아니며(날아가면 원본에서 재구축), 정합성 책임이 없다(변동 필드는 원본에서 라이브). 설계를 한 문장으로: **강의 데이터를 변동성으로 쪼개, 정적 부분은 Redis에 읽기 모델로 물질화하고 정원은 원본에서 라이브로 조립한다**
- 읽기 복제본(RDB 하나 더)과의 비교: 복제본은 일을 나누지 줄이지 않는다 - 위 작업량이 요청마다 그대로 반복된다. 복제 지연은 `isRegisterable`을 낡게 해 라이브 조립으로 없앤 문제를 다시 산다. 물질화의 이점 자체는 Redis 고유가 아니며(MySQL에 JSON 조각 테이블을 둬도 대부분 얻는다) Redis를 고른 이유는 TTL·flush·전량 교체가 언어 차원에서 제공되고, 스키마·트랜잭션 없는 일회용 값에 맞는 도구이며, 보호하려는 DB에 뜨거운 읽기를 다시 얹지 않기 때문이다
- Redis만 쓰지 않는 이유: 정원 검사는 조건부 원자 UPDATE + 트랜잭션 + registrations/carts/members 참조 무결성 + 내구성을 요구한다. 트랜잭션이 필요한 진실은 RDB, 필요 없는 파생 읽기는 Redis - 이 경계가 곧 변동성으로 쪼갠 선이다
- 대상 선정 기준은 빈도가 아니라 **읽기 빈도 × 1회 비용 ÷ 변경 빈도, 그리고 키 공간의 유한성**이다. 호출자의 경험상 빈도는 장바구니 > 전공 ≈ 교양 > HUSS·연계전공이지만:

| API | 빈도 | 1회 비용 | 변경 빈도 | 키 공간 | 판정 |
|---|---|---|---|---|---|
| 전공 `/major` | 높음 | 매우 큼 | 학기 중 0 (정원 제외) | 62 | **적합** - 이번 대상 |
| 교양 `/general-education` | 높음 | 큼 (같은 형태) | 0 | 영역 수 | 적합 - 확장 1순위 |
| HUSS, 연계전공 | 낮음 (호출자 경험) | 큼 | 0 | 1 / 연계전공 수 | 제외. HUSS는 키 1개라 필요해지면 가장 싸게 넣을 수 있음 |
| 장바구니 `GET /carts` | **가장 높음** (페이지 진입) | 내 cart ≤10행은 작음, `cartCount` 집계(전체 회원 cart GROUP BY)는 피크에 큼 | 내 목록은 내 쓰기마다, `cartCount`는 **누가든** 담을 때마다 | 회원 수 | **응답 캐시 부적합** - 회원별 캐시 + 본인 쓰기 무효화로는 `cartCount`(경쟁률)가 낡는다. 비싼 부분이 카운터이므로 `current_enrollment`처럼 컬럼 카운터(RDB) 또는 Redis `INCR`로 다룬다. 별도 대상으로 측정 |
| 검색 | 높음 | 큼 (FULLTEXT) | 0 | 무한 (검색어) | 부적합 - 적중률 |

- 전량 적재의 근거: 키가 유한하고(62), 피크에 전부 뜨겁고(전 학과 학생이 수강신청), 재구축이 싸며(전체 ≈12 MB), 안 채울 때의 비용이 피크에 집중된다(cold 재기동). 키가 회원 단위였거나 특정 학과만 간헐적이면 채우지 않는 것이 맞다

| 결정 항목 | 정한 값 | 근거 |
|---|---|---|
| 전략 | **C. 정적 부분 캐싱 + `isRegisterable` 라이브 조립.** 응답에서 `isRegisterable`을 뺀 나머지(강의 21컬럼 + `schedule` 문자열 + `is75MinLesson`)를 Redis에 두고, 매 요청 `id, currentEnrollment, maxCapacity` projection 1건으로 `isRegisterable`을 계산해 조립한다 | 호출자 요구: 신청·취소로 인원이 바뀌면 응답에 **즉시** 반영(정합성 지연 0). 정책상 정원 검사는 신청 시점 조건부 UPDATE(`CourseRepository.java:123`)가 강제하므로 목록이 낡아도 정합성은 안 깨지지만, 시뮬레이터라는 제품 성격상 정원 마감을 목록에서 보여주는 것이 목적이라 호출자가 지연 0을 택했다. C는 이 요구를 쓰기 경로(`RegistrationService`)를 건드리지 않고 만족한다. 응답이 의존하는 나머지 데이터(`courses`·`course_schedules`)를 바꾸는 코드 경로는 `CourseSyncApplier.apply`뿐인데 호출자가 "제거될 기능"이라 했으므로, 정적 부분의 변경은 코드 밖(적재)에서만 일어난다 |
| 캐시 키 | `MemberDepartment` enum 이름 (네임스페이스는 Phase 7에서) | `CourseDepartment.ownedBy(memberDepartment)`가 응답 집합을 결정하므로 키와 응답이 1:1. 상한 62키(부하에서는 8키). 회원 학과 수정(`MemberService.updateDepartment`)은 다른 키를 읽게 될 뿐 무효화가 필요 없다. `ownedBy`가 빈 학과는 서비스가 DB를 안 가므로(`CourseService.java:53`) 캐시도 거치지 않는다. 부하 조건(1,000명 / 8학과 균등)에서 적중률 ≈ 1 − 8/2,567 ≈ 99.7% |
| 캐시에 넣는 것 | `isRegisterable`을 뺀 **정적 레코드를 신설**하고(`course/dto/common/`), 목록을 래퍼 레코드로 감싼다. `MajorCourseResponse`는 정적 레코드 + boolean으로 조립 | 캐시에 "판정 전 `isRegisterable`"이 들어가는 순간이 없다. 응답 레코드에 복사용 메서드를 붙이지 않는다. 래퍼는 JSON 역직렬화 시 `List<...>` 제네릭 소거 때문에 필요하다. `Course` 엔티티 캐싱은 준영속 `schedules` 지연 로딩이 터져 성립하지 않는다 |
| 직렬화 | JSON (Jackson) | 필드 변경에 견디고 값이 읽힌다. Boot 4.0.1은 Jackson 3라 Spring Data Redis 직렬화기 클래스는 Phase 7에서 실물 확인 |
| 계층과 방식 | `@Cacheable`(Spring Cache 추상화). 정적 부분을 읽는 `loadMajorCourses`와 강제 갱신 `refreshMajorCourses`(`@CachePut`)를 **별도 빈** `course/infra/CourseCacheLoader`에 두고 `@Transactional(readOnly = true)`. **락 없음** (non-locking writer, `sync` 없음) | 프록시 기반이라 같은 클래스 안 호출은 우회된다. miss 경로가 `getSchedules()` 지연 로딩을 타므로 트랜잭션 안이어야 한다(`open-in-view: false`). 처음엔 `sync = true` + locking writer로 확정했다가 **뒤집었다** - SDR 4.0.1 바이트코드로 확인한 사실: 락 키가 `{캐시명}~lock` 하나라 키가 아니라 **캐시 단위**이고, `execute()`가 모든 연산(hit인 `get` 포함) 앞에서 락 해제를 폴링 대기하며, 대기 스레드는 `findById`로 이미 잡은 Hikari 커넥션을 쥔 채 기다린다. 100학과가 비어 있으면 100번째 요청은 앞선 99로드(≈20 s)를 기다리고 그 사이 풀(10)이 고갈돼 수강신청까지 멈춘다. 락이 막는 것은 같은 키 중복 로드(DB 50 ms 몇 번)뿐이라 치르는 값이 크다. 빈 상태를 없애는 쪽(적재)으로 해결한다 |
| 적재 (warming) | 기동 시 `RedisCacheConfig`의 `ApplicationRunner`가 캐시 이름 전부를 `clear()` → `course/infra/CourseCacheWarmer`가 `ApplicationReadyEvent`에서 `ownedBy`가 비지 않은 `MemberDepartment`를 돌며 `refreshMajorCourses`(프록시, `@CachePut`) 호출. `spring.cache.type=redis`일 때만 빈이 뜬다(`@ConditionalOnProperty`) | "재기동 = flush"를 "재기동 = flush + 전량 적재"로 닫아 **비어 있는 상태 자체를 없앤다.** 학과마다 트랜잭션이 끝나 힙 상한이 있다(ACTIVE 24,000 + 시간표 72,000을 1쿼리로 올리는 안은 힙 수백 MB라 철회). 기동 지연 ≈62 × 150 ms ≈ 10 s. Tomcat이 runner보다 먼저 열리므로 그 사이 도착한 요청은 락 없이 스스로 로드한다(워머와 같은 값을 두 번 PUT할 뿐). 테스트(H2)·운영(`none`)에서는 워머가 뜨지 않는다 |
| 정적 부분 갱신 | **매일 04:00(Asia/Seoul) 같은 워머로 재적재** (`@Scheduled`, `@CachePut`이라 빈 창 없이 덮어씀) + **TTL 25h** 안전망 | 처음 "재기동 flush + TTL 24h"로 확정했다가 뒤집었다. 기동 때 62키를 한꺼번에 채우면 24h 뒤 같은 초에 전부 만료돼 하루 한 번 mini cold start가 생기고, 그 시각은 피크일 수 있다. 새벽 고정 재적재는 만료라는 사건을 운영에서 없애고 "재기동 없이 DB를 만진 날"도 다음 새벽에 잡는다. TTL 25h는 스케줄러가 죽었을 때의 최후 안전망이라 정상이면 발동하지 않는다. 지터 TTL(24h ± 2h)과 "그대로 감수"는 이 근거로 배제 |
| 장애 시 동작 | **fail-open.** `global/exception/handler/CacheExceptionHandler`(`CachingConfigurer.errorHandler`)가 get 오류를 WARN 로그 후 miss로 취급해 원본(DB)으로 넘기고 put/evict/clear 오류는 로그만 남긴다. `spring.data.redis.timeout=1s`, `connect-timeout=1s`, Lettuce `disconnectedBehavior=REJECT_COMMANDS` | 기본 `SimpleCacheErrorHandler`는 예외를 다시 던져 **Redis가 죽으면 DB가 멀쩡해도 `/major`가 500**이 된다(실제 메서드가 호출조차 안 됨). Redis가 "멈추면" Lettuce 기본 명령 타임아웃 60s 동안 요청 스레드와 커넥션을 쥐고 서서 앱 전체가 커넥션 고갈로 멈춘다. 캐시는 파생 데이터라 없으면 원본으로 가는 것이 정의다. fail-open은 장애를 조용히 흡수하므로 WARN 로그와 `cache_gets` miss 급증으로 알아채야 한다 |
| 허용할 정합성 지연 | 정원(`isRegisterable`) **0** / 폐강 **0** (라이브 맵에 없으면 제외) / 신규·수정 강의는 **다음 재적재까지** (최대 다음 새벽 04:00 또는 재기동) | 위 항목들의 결과 |
| 라이브 쿼리 | `SELECT c.id, c.currentEnrollment, c.maxCapacity FROM Course c WHERE c.department IN :departments AND c.status = ACTIVE` — **정렬 없음**, 결과는 `dto/common/` projection 레코드 | 순서는 캐시된 목록이 쥔다. 정렬을 빼면 세트 B의 `Sort_rows` 3,378(사이클 3 위험 신호 3번)이 이 경로에서 사라진다. `max_capacity`도 함께 읽으므로 정원 직접 조정이 즉시 반영된다 |
| 조립 규칙 | 캐시 목록 순서 유지. 강의 id로 라이브 결과 맵을 찾아 `isRegisterable` 계산. **맵에 없는 강의(캐시 이후 폐강)는 제외** | 라이브 쿼리가 `status = ACTIVE`를 걸므로 폐강은 자연히 걸러진다. 캐시 이후 새로 생긴 강의는 flush/TTL까지 안 보인다 — 적재 처리(재기동 flush)가 덮는 범위 |
| 적용 범위 | **`/major`만.** 같은 형태의 조회 4곳(`CourseService.java` 68, 86, 99, 121행)은 이후 도입 검토 | Phase 8이 재는 경로와 일치. 사이클 3과 달리 API마다 키가 다르고(교양은 영역, HUSS는 무파라미터) 한 번에 넓힐 근거가 약하다 |
| 측정 수단 | actuator `cache_gets_total{result="hit"\|"miss"}`, `cache_puts_total` (`spring.cache.redis.enable-statistics=true`). Redis `INFO stats` 대조는 선택 | `@Cacheable`이라 설정값 하나로 뜬다. 카운터는 기동 이후 누적이므로 **측정 구간 전후 값의 차**로 읽는다. non-locking이라 로드 1회 = miss 1 + put 1이다(locking 때는 miss 2였다). 기동 직후 puts = 워머가 채운 키 수 |

- 기법 적용 후 예상되는 쿼리 형태 (Phase 7에서 실물 확인)
  1. 적중(거의 전부): `members` PK 조회 1 + 라이브 projection 1 — `SELECT c1_0.id, c1_0.current_enrollment, c1_0.max_capacity FROM courses c1_0 WHERE c1_0.department IN (...) AND c1_0.status = ?`. 세트 A 879행 / 세트 B 3,378행, 3컬럼
  2. miss(키당 1회, 워밍업에서 소진): 위 + 상태 3의 `courses` 28컬럼 쿼리 + `course_schedules` 배치, 그 뒤 Redis SET
  - 워밍업 뒤 digest를 리셋하므로(commands.md A) 측정 구간의 `courses`·`course_schedules` digest는 **0건**이어야 한다. 0이 아니면 캐시가 우회된 신호다
- 검토했지만 택하지 않은 안
  - **A. 응답 전체 캐싱 + 신청·취소마다 evict** — `isRegisterable = currentEnrollment < maxCapacity`는 경계(`99 → 100`, `100 → 99`)에서만 뒤집히므로 응답이 안 바뀌는 쓰기에도 학과 키 전체(≤1.7 MB)를 재구축한다
  - **A'. 경계를 넘을 때 이벤트 발행 → evict** (호출자 제안, `CourseSyncExecutor.java:35`의 `@TransactionalEventListener(AFTER_COMMIT)` 패턴 재사용) — 세 가지로 배제. (1) 경계 감지: UPDATE는 affected rows만 돌려주므로 신청·취소마다 SELECT +1이 가장 뜨거운 쓰기 경로에 붙는다. (2) cache-aside 경합: 조회 X가 DB를 읽고(99) 캐시에 쓰기 전에 신청 Y가 커밋·evict하면 X가 낡은 `true`를 캐시에 남기고, 다음 경계 이벤트나 TTL 전까지 자가 회복이 없다. 21 RPS(요청 간격 ≈47 ms)에 재구축 50 ms 이상이라 창이 실질적이다. (3) 피크 = 강의들이 정원에 닿아 경계에서 진동하는 구간 = evict가 가장 잦은 구간. 게다가 이번 부하는 신청이 없어(시드 `enrollment.sql` 미사용, `current_enrollment` 전부 0) 이 경로를 검증하지 못한다
  - **A''. 학과 키를 Hash로 두고 뒤집힌 강의의 플래그만 `HSET`** — 재구축과 stampede는 없으나 감지 쿼리 +1은 그대로고, 두 리스너의 실행 순서가 커밋 순서와 어긋나면(`false` 뒤 `true`가 먼저) 낡은 값이 남아 버전 관리가 붙는다. 이후 사이클에서 "적중 시 쿼리 0"이 필요해지면 이 형태로 간다
  - **B. 응답 전체 캐싱 + 짧은 TTL, 무효화 없음** — 호출자가 정합성 지연 0을 요구해 배제. 1s TTL이면 21 RPS에서 요청의 ≈95%가 낡은 값을 볼 수 있다
  - 키 `memberId` — 같은 학과 회원이 동일 값을 각각 저장, 부하 적중률 ≈61%, 학과 수정 시 무효화 필요. 키 `CourseDepartment` 목록 — `MemberDepartment`와 동치이나 문자열만 김
  - JDK 직렬화 — 레코드에 `Serializable`, 필드 변경 시 기존 캐시 전부 실패. `RedisTemplate` 수동 — 프록시 함정은 없으나 hit/miss 카운터를 직접 달아야 하고 서비스에 캐시 코드가 드러남
  - TTL 없음(재기동 flush 단독) — 재기동 없이 데이터가 바뀌는 날 다음 배포까지 낡는다. 설정값 하나로 상한을 하루로 바꿀 수 있어 배제
  - 적용 범위 4곳 동시 — 위 표
  - **locking writer + `sync = true`** (Phase 7 1차 적용까지 갔다가 철회) — 위 "계층과 방식" 행. 1차 적용 확인에서 `cache_lock_duration_seconds` 0.0088 s(무경합)와 miss 2/로드(락 아래 재확인 때문)를 관측했다
  - **1쿼리 전량 적재** — 영속성 컨텍스트에 ACTIVE 24,000 + 시간표 72,000이 한꺼번에 올라 힙이 수백 MB 튄다. 학과별 프록시 호출로 대체
  - **TTL 지터 / TTL 그대로** — 위 "정적 부분 갱신" 행. 새벽 재적재가 만료 사건 자체를 없앤다
  - **키 단위 락 직접 구현, Redisson** — 코드가 늘고 `@Cacheable` 프록시와 섞이며, 빈 상태를 없애면 필요가 사라진다
  - **L1 로컬 캐시(Caffeine) ± L2 Redis** — 단일 인스턴스에 ≤12 MB 정적 데이터면 로컬만으로 충분할 수 있고 적중 경로의 Redis GET(0.4~1.7 MB) + JSON 역직렬화가 사라진다. 이번 사이클의 선택(Redis)을 되묻는 안이라 **Phase 8에서 적중 경로 비용이 나온 뒤** 다음 사이클 후보로 비교한다
  - **장바구니 응답 캐싱** (호출자 제안) — 위 도입 근거 표. `cartCount`가 전체 회원 집계라 본인 쓰기 무효화로는 낡는다. 카운터 방향으로 별도 대상에서 다룬다
- 해당 없는 항목: 무효화 대상 쓰기 경로(신청·취소는 라이브 조립으로 캐시와 무관, 동기화는 제거 예정, 회원 학과 수정은 키 선택으로 무관)
- 인스턴스가 늘어나면: 재기동 flush가 공유 Redis를 비우므로 한 인스턴스 재기동이 전체를 비운다(워머가 곧 채우고 다른 인스턴스는 스스로 로드하므로 동작은 함). 그때는 flush를 버전 키 교체로 바꾼다. 지금은 단일 인스턴스
- 호출자가 예상한 효과: 호출자가 수치로 명시하지 않았다. 스킬이 상태 3 수치로 채웠고 호출자가 읽고 넘어갔다

| 구분 | 지표 | 상태 3 | 예상 | 근거 / 출처 |
|---|---|---|---|---|
| 움직인다 | 요청당 SELECT 수 | 3.7479 (`courses` 0.9996 + `course_schedules` 1.7495 + `members` 0.9988) | ≈2.0 (`members` 1 + 라이브 projection 1) | `courses`·`course_schedules` digest는 워밍업에서 8키 miss로 소진, 측정 구간 0건 |
| | 대상 쿼리 `rows_per_call` | `courses` 1498.5078 / `course_schedules` 2609.2532 | 라이브 projection ≈1,498.5 | 같은 조건, 같은 행 수, 컬럼만 3개 |
| | `examined_per_sent` | `courses` 1.6338 | ≈1.0 | 정렬이 없어 filesort 되읽기가 없다 |
| | 세트 B `Sort_rows` / `Sort_merge_passes` | 3,378 / 4 | **0 / 0** | 라이브 쿼리에 `ORDER BY`가 없다 |
| | 요청당 JDBC 값 수 | ≈78,460 | ≈4,494 (1,498 × 3) | **-94.3%** |
| | 요청당 DB 시간 | 50.81 ms | `members` 0.56 + 라이브 projection (22.19 미만, 미확정) | `course_schedules` 몫(54.2%) 소멸. 라이브 쿼리는 `courses` 쿼리에서 정렬과 25컬럼 전송을 뺀 것 |
| | 접근 방식 | `idx_department_sort` ref/range → `idx_course_id` range | `idx_department_sort` ref/range + PK 행 조회 (비커버링) | `current_enrollment`·`max_capacity`는 인덱스에 없다 |
| | `Handler_read_key` / `read_next` (A) | 880 / 3,611 | 1 / 879 | schedules 몫 879 / 2,732 소멸 |
| | 캐시 hit / miss / puts | - | ≈2,560 / 0 / 0 (측정 구간 차) — 기동 시 워머가 puts ≈ 학과 수만큼 먼저 채우므로 k6 워밍업에서는 miss가 없어야 한다 | 워머가 전 학과를 채움, TTL 25h |
| 안 움직인다 | 요청당 응답 크기 | 755,702.8079470198 B | 동일 | DTO 내용이 같다 |
| | 결과 순서 | `ORDER BY` 3컬럼 | 동일 | 캐시 목록이 상태 3의 정렬 결과를 그대로 보존 |
| 측정해야 안다 | 응답시간 (med / p95) | 1028.616 / 2110.25 ms | 미확정 | 신호 ④(DB 밖 95.0%)의 정체에 달림. 적중 경로에 남는 일 = Redis GET(≈0.4~1.7 MB) + JSON 역직렬화(≈1,498 레코드) + 조립 + HTTP 직렬화 |
| | 요청당 Redis 읽기 크기 | - | 정적 부분 ≈ 응답 크기 − `isRegisterable` 몫 | 부하 21 RPS 기준 ≈16 MB/s (루프백) |

- 판정 시 유의: 1차 근거는 (1) 측정 구간에서 `courses`·`course_schedules` digest 0건 + 라이브 projection digest 등장, (2) 라이브 쿼리 `rows_per_call` ≈1,498.5 / `examined_per_sent` ≈1.0, (3) 세트 B `Sort_rows` 0, (4) `cache_gets` hit/miss 차. 응답시간은 편차 28% 안이면 근거로 쓰지 않는다. **이번 부하는 신청이 없어 `isRegisterable`의 실시간성은 측정이 아니라 통합 테스트로 확인한다**


### 개선 전 지표

> 상태 3 = 사이클 3 적용 후. `k6-test-summary-3.json`, `query-stats-summary-3.md`, `query-plan-3.txt`에서 옮겼다. 대상 쿼리가 상태 3과 같으므로 다시 뜨지 않았다.

| 구분 | 지표 | 값 |
|---|---|---|
| 하드웨어 의존 | p95 / p99 | 2110.25 / 2602.2771199999997 ms |
| | RPS | 21.370335865922097 |
| | 에러율 / check 통과율 | 0 / 1 (2,567건) |
| | med / waiting med | 1028.616 / 1018.409 ms |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 3.7479건 (`courses` 0.9996 + `course_schedules` 1.7495 + `members` 0.9988), 제어문 포함 8.7544 |
| | `courses` `calls` / `mean_ms` / `total_ms` / 점유율 | 2566 / 22.192974 / 56947.172354 / 42.0369% |
| | `course_schedules` `calls` / `mean_ms` / `total_ms` / 점유율 | 4491 / 16.364202 / 73491.63137 / 54.2496% |
| | 두 쿼리 합 `total_ms` / 점유율 | 130438.803724 / 96.2865% |
| | `rows_per_call` / `examined_per_sent` | `courses` 1498.5078 / 1.6338, `course_schedules` 2609.2532 / 1.0000 |
| | 요청당 DB 시간 | 50.81 ms (응답시간 대비 5.0%) |
| | 요청당 JDBC 값 수 | ≈78,460 (1,498 × 28 + 4,565 × 8) |
| | 요청당 응답 크기 | 755,702.8079470198 B |
| 캐싱용 추가 캡처 | 동일 입력의 반복 호출 비율 | 입력(`MemberDepartment`) 8종 / 요청 2,567건 → 입력당 ≈321회 반복, 첫 호출을 제외한 반복 비율 (2,567 − 8) / 2,567 = **99.69%**. 회원 1,000명이 8학과 균등이라 생긴 값이며 운영 분포(62학과, 학과별 회원 수 불균등)보다 유리하다 |
| | 무효화가 필요한 쓰기 경로 | 설계상 **없음.** `isRegisterable`은 라이브 조립(신청·취소가 캐시와 무관), 정적 부분의 변경 경로는 `CourseSyncApplier.apply` 하나였으나 제거 예정이라 코드 밖 적재만 남음 → 재기동 flush + TTL 24h. 회원 학과 수정은 키 선택(`MemberDepartment`)으로 무관 |
| | 호출 스택 | `CourseController.getMajorCourses` → `CourseService.getMajorCourses` (`@Transactional(readOnly = true)`) → `MemberRepository.findById` → `CourseRepository.findByDepartmentIn` → `MajorCourseResponse.from` × 1,498 (첫 `getSchedules()`에서 `@BatchSize` 배치 발행) |

**실행계획 (상태 3)** - 원본 `query-plan-3.txt`. 파라미터는 사이클 1~3과 동일: 세트 A `department = 'COMPUTER_ENGINEERING'` (IN 1개, 879강의, 흔한 쪽 - 부하 회원의 62.5%가 IN 1개), 세트 B `ELECTRONICS_ENGINEERING_SCHOOL` 소속 4개 학과 (IN 4개, 3,378강의, 드문 쪽 - 최대 조회량). 대상이 두 쿼리라 세트당 2개, 총 4개. schedules 쿼리의 `IN` 목록은 그 세트의 courses 결과 id를 결과 순서로 넣었고, 세트 B는 첫 배치 1,000개.

- 세트 A courses 1.8 ms: Filter `status` 자체 0.13 / Index lookup `idx_department_sort` (ref, `department = const`) 1.67, `using_filesort: false`. 추정 87.9 대 실측 879 (`filtered: 10.00`, 10.0배). 카운터 `Handler_read_key` 1 / `Handler_read_next` 879
- 세트 A schedules 4.41 ms: Index range scan `idx_course_id` 879구간, 추정 1,758 대 실측 2,732 (1.55배). 카운터 `Handler_read_key` 879 / `Handler_read_next` 2,732
- 세트 B courses 25.7 ms: Sort 자체 15.2 (59.1%) / Filter 자체 0.4 / Index range scan `idx_department_sort` 4구간 10.1. 추정 3,378 대 실측 3,378. 카운터 `Handler_read_key` 4 / `Handler_read_next` 3,378 / `Sort_scan` 1 / `Sort_rows` 3,378 / `Sort_merge_passes` 4
- 세트 B schedules 첫 배치 4.88 ms: Index range scan `idx_course_id` 1,000구간, 추정 2,000 대 실측 3,159 (1.58배). 카운터 `Handler_read_key` 1,000 / `Handler_read_next` 3,159
- 호출자 해석: "세트 A는 인덱스 범위 스캔, 세트 B는 정렬". 판정: 둘 다 타당. 세트 A는 두 쿼리 합 6.21 ms 중 schedules range scan 4.41(71.0%), courses Index lookup 1.67(26.9%)로 인덱스 접근 자체가 비용이고 필터 0.13은 무시할 수준. 세트 B는 courses 25.7 중 Sort 자체 15.2(59.1%)가 1위, range scan 10.1이 2위
- **확정 해석**: 세트 A는 schedules 범위 스캔(4.41)이, 세트 B는 정렬(15.2)과 범위 스캔(10.1)이 비용을 먹는다. 이 사이클의 기법이 닿는 곳 - 적중 요청에서 schedules 쿼리(A 4.41, B 4배치 ≈16)와 세트 B Sort 15.2는 **소멸**, courses 인덱스 접근(A 1.67, B 10.1)은 정렬 없는 3컬럼 라이브 쿼리로 **잔존**. 라이브 쿼리 몫은 세트 A ≈1.7 ms, 세트 B ≈10 ms 부근으로 예상하며 Phase 8 계획으로 확인한다
- 위험 신호 (사이클 3 판정에서 이월): 두 쿼리 합 점유율 96.2865% (30% 초과), 세트 A 추정 대 실측 10.0배 (`status` `filtered: 10.00`), 세트 B `Sort_rows` 3,378 = 반환 행 수. `examined_per_sent` 1.6338 / 1.0000, 풀스캔 없음, `course_schedules` 요청당 1.7495는 `@BatchSize` 배치라 N+1 아님

### 적용 내용

- 변경 파일 (마이그레이션 없음, 엔티티 변경 없음)
  - `build.gradle` - `spring-boot-starter-cache`, `spring-boot-starter-data-redis` 추가. 해석 결과 `spring-data-redis 4.0.1`, `lettuce-core 6.8.1`, `jackson-databind 3.0.3`
  - `global/config/RedisCacheConfig.java` (신규) - `@EnableCaching` + `CachingConfigurer`. (1) `errorHandler()` → `CacheExceptionHandler`(fail-open), (2) `RedisCacheManagerBuilderCustomizer`로 `major-courses` 캐시에 `builder.cacheDefaults()`(Boot 속성 반영: TTL은 yml `spring.cache.redis.time-to-live`) + `JacksonJsonRedisSerializer<CachedMajorCourses>`(Jackson 3, 타입 고정이라 `@class` 메타데이터 없음), writer는 Boot 기본(non-locking), (3) `LettuceClientOptionsBuilderCustomizer`로 `disconnectedBehavior = REJECT_COMMANDS`, (4) `ApplicationRunner cacheFlusher` - 기동 시 캐시 이름 전부 `clear()`
  - `global/config/SchedulingConfig.java` (신규) - `@EnableScheduling` (`AsyncConfig`와 대칭)
  - `global/exception/handler/CacheExceptionHandler.java` (신규, 호출자가 패키지를 `GlobalExceptionHandler`·`AsyncExceptionHandler` 옆으로 옮기고 이름을 맞춤) - `CacheErrorHandler` 4메서드(인터페이스 것이라 메서드명은 못 바꿈) 모두 `@Log4j2` WARN 한국어 로그만 남기고 삼킴
  - `course/dto/common/CachedMajorCourse.java` (신규) - `isRegisterable`을 뺀 정적 18필드, `from(Course)`. `CachedMajorCourses.java` (신규) - 목록 래퍼(제네릭 소거 대응). `CourseCapacity.java` (신규) - projection `(id, currentEnrollment, maxCapacity)` + `isRegisterable()`
  - `course/repository/CourseRepository.java` - `findCapacitiesByDepartmentIn`: `SELECT new CourseCapacity(c.id, c.currentEnrollment, c.maxCapacity) FROM Course c WHERE c.department IN :departments AND c.status = ACTIVE` (정렬 없음)
  - `course/infra/CourseCacheLoader.java` (신규) - `MAJOR_COURSES = "major-courses"`. `@Cacheable(key = "#memberDepartment.name()")` `loadMajorCourses`와 `@CachePut` `refreshMajorCourses`, 둘 다 `@Transactional(readOnly = true)`이고 같은 private 읽기 메서드를 공유. `admin/infra/CourseSyncApplier`가 리포지토리를 주입하는 선례를 따라 infra에 둠. 호출자 지시로 이름을 넓혔다(두 번째 캐시가 붙을 자리). 처음 `MajorCourseCacheReader`에 있던 기동 시 evict는 설정으로 옮겼다
  - `course/infra/CourseCacheWarmer.java` (신규) - `@ConditionalOnProperty(spring.cache.type=redis)`. `ApplicationReadyEvent` + `@Scheduled(cron = "${cache.major-courses.refresh-cron}", zone = "${cache.major-courses.refresh-zone}")`에 `ownedBy`가 비지 않은 `MemberDepartment`마다 `refreshMajorCourses`. 주기·타임존은 yml(운영 파라미터). 캐시 이름 `MAJOR_COURSES`는 코드 상수로 남김 - `@Cacheable`의 `cacheNames`는 `${}`를 풀지 않고(`SpringCacheAnnotationParser`에 `EmbeddedValueResolver` 참조 없음, `@Scheduled`는 있음) 캐시 이름은 테이블명 같은 코드 식별자라 서버 정보가 아님
  - `course/service/CourseService.java` - `getMajorCourses`가 `courseCacheLoader.loadMajorCourses` 정적 목록 + `findCapacitiesByDepartmentIn` 맵으로 조립. 맵에 없는 강의(캐시 이후 폐강)는 제외
  - `course/dto/response/MajorCourseResponse.java` - `of(CachedMajorCourse, boolean)` 추가. `from(Course)`는 `of(CachedMajorCourse.from(course), course.isRegisterable())`로 위임해 매핑 원천을 하나로 유지(타학과·HUSS 경로와 기존 테스트는 그대로 `from` 사용)
  - `application-perf.yml` - `spring.data.redis` (127.0.0.1:6379, `timeout=1s`, `connect-timeout=1s`), `spring.cache.type=redis`, `spring.cache.redis.enable-statistics=true`, `spring.cache.redis.time-to-live=25h`, `cache.major-courses.refresh-cron="0 0 4 * * *"`, `cache.major-courses.refresh-zone=Asia/Seoul`. 선행 명령에 `redis` 추가. 워머 빈이 뜨는(redis) 프로파일에만 `cache.major-courses.*`가 필요하고 없으면 기동 실패(fail-fast). **템플릿 원본(`template/application-perf.yml`)에는 반영하지 않았다** - 캐싱은 이 사이클 고유라 템플릿 갱신 여부는 호출자 판단
  - `src/test/resources/application.yml` - `spring.cache.type=none` (H2 테스트가 Redis 없이 돌도록. `@Cacheable`은 no-op이 되어 조립 로직만 검증)
  - `application-prod.yml` - `spring.cache.type=none`. **운영에 Redis가 없어서**(`docker-compose-prod.yml`에 서비스 없음) 이 값 없이 배포하면 `/major` 첫 호출이 6379 연결 거부로 실패한다. Redis를 붙일 때 `redis`로 바꾼다
- 실물 확인으로 정한 것
  - SDR 4.0.1의 `RedisCache.get(key, loader)`는 로더 호출을 `RedisCacheWriter.get(name, key, Supplier, ttl, ...)`에 위임하고, **락은 writer가 locking일 때만** 건다. 1차 적용은 `lockingRedisCacheWriter`를 명시했으나 락이 캐시 단위이고 대기가 커넥션을 쥔다는 사실(설계 결정 "계층과 방식")로 2차에서 걷어냈다
  - Boot 4의 `RedisCacheManagerBuilderCustomizer`는 `org.springframework.boot.cache.autoconfigure`, 캐시 지표는 `spring-boot-cache`의 `RedisCacheMetrics`(`cache_gets_total{result}`, `cache_puts_total`, `cache_lock_duration`)
- 테스트 (1차, locking 버전): `./gradlew test` BUILD SUCCESSFUL (51s), **290건 통과, 실패 0, 에러 0** (사이클 3과 같은 290건).
  `CourseServiceTest`의 `getMajorCourses` 11개 케이스가 새 조립 경로(로더 → projection 맵 → `of`)를 H2에서 통과했다. 캐시 자체(Redis 왕복, 직렬화 왕복, 적중 시 쿼리 생략)는 테스트 프로파일이 `none`이라 **검증되지 않았다** - 적용 확인이 그 몫이고, 회귀 방지용 Redis 통합 테스트는 이후 과제로 남긴다
- 테스트 (2차, 최종 코드 - 설정 외부화·`CacheExceptionHandler` 개명 후): `./gradlew test` BUILD SUCCESSFUL, **290건 통과, 실패 0, 에러 0.** `spring.cache.type=none`이라 `CourseCacheWarmer`는 뜨지 않았고(`@ConditionalOnProperty`, 플레이스홀더도 해석되지 않음), 조립 경로·fail-open 핸들러 등록·스케줄링 설정이 컨텍스트 기동을 깨지 않는 것까지 확인됐다
- 1차 적용 확인 (locking 버전, 2026-08-30 00:03 재기동 직후, 회원 900001 = `COMPUTER_ENGINEERING`, 세트 A와 같은 값). 기대값 5개 중 4개 일치, 1개는 통계 방식의 차이로 판명. **이 코드는 이후 뒤집혔으므로 수치는 참고용**
  1. 기동 직후 `major-courses*` 키 없음 (Redis 컨테이너가 새로 떠서 flush와 "원래 비어 있음"은 구분되지 않는다)
  2. 1회차(miss): 200, 443,894 B, 0.682 s. digest = `courses` 28컬럼 879행 + `course_schedules` 2,732행 + `members` 1행 + **capacity 3컬럼 879행**. 기대와 일치
  3. 2회차(hit): 200, **443,894 B (동일)**, 0.042 s. digest = **capacity 879행 + `members` 1행뿐.** `courses`·`course_schedules` 소멸. 캐시가 우회되지 않았다
  4. Redis: 키 `major-courses::COMPUTER_ENGINEERING` 1개, TTL 86,388(24h − 12s), STRLEN 424,543 B (응답 443,894보다 19,351 작음 = 879 × `"isRegisterable":true,` 22 B)
  5. actuator: `cache_gets_total` hit 1 / **miss 2** / pending 0, `cache_puts_total` 1, `cache_lock_duration_seconds` 0.008845584.
     miss가 2인 이유를 `DefaultRedisCacheWriter.get(name, key, Supplier, ttl, timeToIdle)` 바이트코드로 확인했다: locking writer는 락 없이 한 번 읽고(`get(name, key, ttl)` → `doGet` → `incMisses`), 락을 잡은 뒤 다시 읽어 확인하고(`execute` 안 `doGet` → `incMisses`) 그제야 로드한다. 실제 로드 1회 = miss 2 + put 1. digest에 `courses` 쿼리가 1회뿐인 것이 로드가 한 번이었다는 증거. non-locking으로 바꾼 최종 코드에서는 로드 1회 = miss 1 + put 1이어야 한다
- 2차 적용 확인 (최종 코드, 2026-08-30 새벽 재기동 직후, 회원 900001 = `COMPUTER_ENGINEERING`). **기대값 전부 일치**
  1. 워머: 키 60개(`ownedBy`가 비지 않은 `MemberDepartment` 수와 일치), TTL 89,976(25h − 24 s), `cache_puts_total` 60, `cache_gets` hit 0 / miss 0, `cache_lock_duration_seconds` 0
  2. 첫 요청: 200, 443,894 B, 0.223 s. digest = capacity 3컬럼 879행 + `members` 1행. `cache_gets` **hit 1 / miss 0** - 워밍 덕에 첫 요청부터 적중, locking 때의 "로드 1회 = miss 2" 현상 소멸
  3. Redis 정지(`docker stop uss-redis`) 중 요청: **200**, 같은 443,894 B, **0.085 s**. digest에 `courses` 28컬럼 879행 + `course_schedules` 2,732행 + capacity + `members` - fail-open으로 DB(상태 3 경로)에서 응답. 1 s 타임아웃보다 빠른 것은 `REJECT_COMMANDS`가 끊긴 연결에서 즉시 거부하기 때문
  4. Redis 복귀 후: 200, 0.113 s. 키는 `docker stop`으로 지워지지 않아 그대로 적중

### 개선 후 지표

> 상태 4. `k6-test-summary-4.json`, `query-stats-summary-4.md`, `query-plan-4.txt`, `cache-stats-4.txt`. 측정 조건은 상태 3과 동일(VU 30 / 2m / USER_COUNT 1000 / 풀 10 / warm). `failed_rate` 0, `checks_rate` 1 (6,350건). 측정 시작 시 캐시는 워머가 채운 60키.

| 구분 | 지표 | 개선 전 (상태 3) | 개선 후 (상태 4) | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 2110.25 ms | 766.3679499999996 ms | -63.7% |
| | p99 | 2602.2771199999997 ms | 1084.5673199999999 ms | -58.3% |
| | med / waiting med | 1028.616 / 1018.409 ms | 431.2765 / 424.481 ms | -58.1% / -58.3% |
| | RPS | 21.370335865922097 | 52.91244733992837 | +147.6% (요청 2,567 → 6,350) |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 3.7479건 (전체 8.7544) | SELECT 1.9929건 (capacity 0.9998 + `members` 0.9931; 전체 6.9735) | `courses` 28컬럼·`course_schedules` digest **0건** |
| | 대상 쿼리 `calls` / `mean_ms` / `total_ms` | `courses` 2566 / 22.192974 / 56947.172354 + `course_schedules` 4491 / 16.364202 / 73491.63137 (합 130438.803724) | capacity 6349 / 9.38515 / 59586.323607 | 요청 2.47배에서 total_ms 합 -54.3% |
| | `rows_per_call` | `courses` 1498.5078 / `course_schedules` 2609.2532 | capacity 1498.0433 | schedules 행 소멸 |
| | `examined_per_sent` | `courses` 1.6338 | capacity **1.0000** | 정렬 되읽기 소멸 |
| | 요청당 DB 시간 | 50.81 ms (응답 대비 5.0%) | 9.79 ms (9.38515 × 0.9998 + 0.405776 × 0.9931; 응답 대비 2.3%) | -80.7% |
| | 요청당 JDBC 값 수 | ≈78,460 | ≈4,494 (1,498 × 3) | -94.3% |
| | `total_ms` 점유율 | 42.0369% + 54.2496% = 96.2865% | capacity 85.3284% | 단일 |
| | 요청당 응답 크기 | 755,702.8079470198 B | 755,644.8935433071 B | -0.008% |
| | 접근 방식과 인덱스 | `idx_department_sort` ref/range → `idx_course_id` range (배치) | `idx_department_sort` ref/range + PK 행 조회 (비커버링, `using_index` 없음) | schedules 쿼리·Sort 노드 소멸 |
| | `Handler_read_key` / `read_next` (A) | 880 / 3,611 | 1 / 879 | schedules 몫 소멸 |
| | `Handler_read_key` / `read_next` (B, courses 쿼리만) | 4 / 3,378 | 4 / 3,378 | 동일 - InnoDB가 만지는 행은 같다 |
| | 세트 B `Sort_rows` / `Sort_merge_passes` | 3,378 / 4 | **0 / 0** (`Sort_*` 카운터 없음) | 해소 |
| | 캐시 hit / miss / puts (측정 구간 차) | - | **6,350 / 0 / 0** (누적 hit 1,427 → 7,777, puts 60 고정), 적중률 100% | Redis `keyspace_hits` 6,350 / `misses` 0으로 일치 |
| | Redis → 앱 전송 | - | `total_net_output_bytes` 4,583,067,865 → 요청당 721,742.97 B (응답의 95.5%), 부하 중 ≈38 MB/s | |
| | `cache_lock_duration_seconds` | - | 0 | 락 없음 |

- 사라진 digest: `courses` 28컬럼(`findByDepartmentIn`), `course_schedules` 배치. 나타난 digest: capacity projection(`findCapacitiesByDepartmentIn`). 세션 초기화문(`SET character_set_results`, `@@SESSION.transaction_read_only`)은 이번엔 없음(커넥션 재생성 없음)
- `members` digest 6,306회 = 요청 6,350보다 44회 적음(per_req 0.9931). 상태 3에도 3회 부족(0.9988)이 있었고 캐시 경로와 무관한 PK 조회라 측정 경계의 타이밍으로 본다. 해석에 쓰지 않는다

**실행계획 (상태 4)** - 원본 `query-plan-4.txt`. 파라미터는 Phase 6과 동일. 대상이 capacity projection 하나라 세트당 1개.

- 세트 A 1.29 ms (상태 3 courses 1.8 + schedules 4.41 = 6.21, -79%): Filter `status` 자체 0.11 / Index lookup `idx_department_sort` ref 1.18. 추정 87.9 대 실측 879. 카운터 `Handler_read_key` 1 / `Handler_read_next` 879, `Sort_*` 없음
- 세트 B 14.4 ms (상태 3 courses 25.7 + schedules 4배치 ≈16 = ≈42, -66%): Filter 자체 0.8 / Index range scan `idx_department_sort` 4구간 13.6. 추정 338 대 실측 3,378. 카운터 `Handler_read_key` 4 / `Handler_read_next` 3,378, **`Sort_*` 없음.** `actual time` 첫 행 0.168 ms - 정렬이 없어 스트리밍된다(상태 3 Sort는 24.8 ms에야 첫 행)
- 추정 대 실측: `status` `filtered: 10.00`은 두 세트 모두 10배 그대로. 계획 선택에는 영향 없음. `using_index` 없음 = 비커버링(`current_enrollment`·`max_capacity`는 PK로 행을 읽음), 예상대로

### 판정

- 개선 여부 (하드웨어 독립 증거 기준): **있음.** 호출자 판정 "기법의 효과". 1차 근거는 호출자가 지정하지 않아 스킬이 결정론적으로 바뀐 순서로 채웠다 - (1) 쿼리 구조: `courses` 28컬럼·`course_schedules` digest 0건, capacity projection 등장, 요청당 SELECT 3.7479 → 1.9929, (2) capacity `examined_per_sent` 1.0000 / 세트 B `Sort_rows` 0 / `Handler_read_next`(A) 3,611 → 879 / JDBC 값 -94.3%, (3) 캐시 hit 6,350 / miss 0 (앱·Redis 양쪽 일치).
  하드웨어 의존 변화(med -58.1%, p95 -63.7%, RPS +147.6%)는 사이클 3에서 본 실행 간 편차 28%를 크게 넘어 보조 근거로 쓴다
- 5-B 예상과 대조: 예상 10개 중 9개 적중(요청당 SELECT ≈2.0, `rows_per_call` ≈1,498.5, `examined_per_sent` ≈1.0, `Sort_rows` 0, JDBC 값 ≈4,494, digest 0건, DB 시간 22.19 미만, Handler 1/879, 접근 방식, 응답 크기 동일). 캐시 hit는 방향 적중이나 절대값은 처리량 상승으로 2.47배.
  **미확정으로 둔 응답시간이 갈렸다**: 이 기법이 없앤 것은 DB 작업 41 ms와 Hibernate 하이드레이션(6,063 엔티티) + DTO 변환이고 HTTP 직렬화는 그대로인데 med가 594 ms 줄었다. 따라서 상태 3의 "DB 밖 95%"의 대부분은 직렬화가 아니라 **엔티티 하이드레이션·변환**이었다 - 사이클 2부터 미확정이던 신호 ④가 이 측정에서 처음 갈렸다. 남은 424 ms(VU 30)에는 Redis GET 722 KB + JSON 역직렬화 1,498건 + HTTP 직렬화 + 대기가 들어 있고 그 분해는 남는다
- 이 기법이 줄인 것: DB에서는 행 폭(28 → 3컬럼), 정렬, schedules 쿼리 - InnoDB가 만지는 courses 행 수 자체(`read_next` 879 / 3,378)는 같다. 앱에서는 엔티티 하이드레이션과 DTO 변환 전부. 이 사이클의 근본 근거(도입 근거 절)대로 "요청당 DB 작업량을 줄여 같은 DB로 더 많은 사용자를 받는다"가 RPS +147.6%, 요청당 DB 시간 -80.7%로 수치화됐다
- 측정 이력: 재측정 없이 1회. `checks_rate` 1, `failed_rate` 0. 요청 2,567 → 6,350은 처리량 상승의 결과이지 조건 변경이 아니다. 이번 부하는 신청이 없어 `isRegisterable`의 실시간성(라이브 조립)은 측정이 아니라 2차 적용 확인(정지·복귀 시나리오)과 기존 통합 테스트로만 확인됐다
- 남은 위험 신호
  1. 단일 쿼리 점유율 85.3284% (기준 30% 이상). 이 엔드포인트가 DB에 시키는 일이 capacity 조회뿐이라 구조적으로 남는다
  2. 추정 대 실측 10배 (`status` `filtered: 10.00`, 세트 A·B 모두). 계획 선택에 무해. 닫으려면 `ANALYZE TABLE courses UPDATE HISTOGRAM ON status`(운영 작업, 적재마다 재실행)
  3. DB 밖 비중 97.7% (424 중 415 ms). 비율은 커졌으나 절대값은 968 → 415 ms. 남은 정체(Redis 왕복 722 KB, JSON 역직렬화, HTTP 직렬화, 대기)는 미분해. 다음 후보는 L1 로컬 캐시(역직렬화·Redis 왕복 제거) 또는 응답 압축
  - 해소: 세트 B `Sort_rows` 3,378 (사이클 3 위험 신호 3번), 조인 폭발·fetch join(사이클 2·3에서 이미), DB 밖 정체의 정체 일부(하이드레이션으로 판명)
- 다음 사이클 진행 여부: **종료.** 종료 조건 "호출자가 종료를 선택". 하드웨어 독립 증거는 바뀌었고 위험 신호 1·2·3은 남아 있으나 호출자가 이 대상은 여기서 닫기로 했다. 남은 후보(L1 로컬 캐시, 교양 조회 확장, 장바구니 카운터, 통계 히스토그램)는 Phase 9 보고에 남긴다


---

## 최종 요약

| 구분 | 지표 | 최초 (상태 0) | 최종 (상태 4) | 변화 |
|---|---|---|---|---|
| 하드웨어 의존 | p95 | 4217.297049999999 ms | 766.3679499999996 ms | -81.8% |
| | p99 | 8610.90285 ms | 1084.5673199999999 ms | -87.4% |
| | med / waiting med | 1824.5304999999998 / 1816.719 ms | 431.2765 / 424.481 ms | -76.4% / -76.6% |
| | RPS | 11.361679173551456 | 52.91244733992837 | +365.7% (2m 요청 1,364 → 6,350) |
| | 에러율 / check 통과율 | 0 / 1 | 0 / 1 | 동일 |
| 하드웨어 독립 | 요청당 쿼리 수 | SELECT 2건 (courses+schedules 조인 1 + 회원 1), 제어문 포함 6.9948 | SELECT 1.9929건 (capacity projection 0.9998 + 회원 0.9931), 제어문 포함 6.9735. **강의·시간표 본문 조회는 캐시 적중 시 0건** | |
| | 읽은 행 / 반환 행 | 7.7942 (`rows_per_call` 4562.5169, 호출당 읽은 행 ≈35,561) | 1.0000 (`rows_per_call` 1498.0433) | 읽고 버리는 행 소멸 |
| | 요청당 DB 시간 | 400.13940 ms (응답 대비 약 22%) | 9.79 ms (응답 대비 2.3%) | -97.6% |
| | 접근 방식과 인덱스 | `courses` **풀스캔** + `course_schedules` `idx_course_id` 조인 + filesort (`DISTINCT`, 조인 폭발 4,563행) | `idx_department_sort` ref/range + PK 행 조회 3컬럼, 정렬 없음, 조인 없음. 강의·시간표 본문은 Redis `major-courses::{MemberDepartment}` (60키, 워머 적재) | |
| | `Handler_read_rnd_next` (A / B) | 29,176 / 36,687 | 0 / 0 | 풀스캔 소멸 |
| | `Sort_rows` (A / B) | 2,735 / 10,246 | 0 / 0 | filesort 소멸 |
| | 캐시 hit / miss, 적중률 | - | 6,350 / 0, 100% (부하 조건이 유리: 8학과 균등, 신청 없음) | |
| | 요청당 응답 크기 | 754,827 B | 755,644.8935433071 B | 동일 (응답 내용 불변) |

적용한 기법: 1. `courses` 복합 인덱스 추가 (`V1_11`, `idx_department_sort(department, grade_code, classification_code, haksu_code)`) → 2. JPQL `DISTINCT` 제거 → 3. fetch join 제거 + `@BatchSize` 활용 → 4. 정적 부분 Redis 캐싱 + `isRegisterable` 라이브 조립 (워머 전량 적재, 04:00 재적재, TTL 25h, fail-open)

운영 반영 시 유의점:

1. **마이그레이션 2개** (사이클 1). `V1_10__drop_indexes_for_redesign.sql`이 보조 인덱스 6개를 드롭한다 - `courses.idx_department_sort`·`idx_area_sort`·`idx_huss_sort`, `members.idx_student_id`, `course_sync_jobs.idx_started_at`·`idx_status`. `V1_11__add_index_to_courses.sql`이 `courses.idx_department_sort`를 새 컬럼 구성으로 다시 만든다. 로컬 26,439행에서 `ALTER`는 즉시 끝났다. 운영 `courses` 행 수는 앱 시드 2,439행 + 동기화 결과로, 정확한 값은 모른다
2. **V1_10이 되돌리지 않은 것**: `idx_area_sort`(교양 조회 `findByArea`)와 `idx_huss_sort`(HUSS 조회)는 다시 만들지 않았다. 이 이슈는 `/major`만 측정했으므로 그 두 경로는 **인덱스가 없는 상태로 운영에 나간다.** 운영 행 수 규모(수천)에서 풀스캔이 실측으로 문제인지는 재지 않았다. 각 경로를 대상으로 잡아 근거를 만들거나, 그 전까지 V1_10에서 두 인덱스의 드롭을 빼는 것 중 호출자가 정한다
3. **운영에 Redis가 없다.** `application-prod.yml`은 `spring.cache.type=none`이라 배포해도 캐시·워머·스케줄러가 뜨지 않고 상태 3(fetch join 제거)까지의 성능으로 돈다. Redis를 붙일 때 필요한 것: 인프라(compose 서비스), `spring.data.redis.host/port/timeout/connect-timeout`, `spring.cache.type=redis`, `spring.cache.redis.enable-statistics`·`time-to-live`, `cache.major-courses.refresh-cron`·`refresh-zone`(없으면 기동 실패). 인스턴스가 여러 개면 재기동 flush를 버전 키 교체로 바꾼다(사이클 4 설계 결정)
4. `isRegisterable`의 실시간성(라이브 조립)과 캐시 왕복은 H2 테스트(`type=none`)가 덮지 않는다. Redis testcontainer 통합 테스트는 이후 과제
5. 남은 후보 (근거는 사이클 4 판정): L1 로컬 캐시(남은 응답시간의 Redis 왕복 722 KB·JSON 역직렬화 제거 여부), 교양 조회 캐싱 확장, 장바구니 `cartCount` 카운터화(별도 대상), `status` 히스토그램(추정 괴리 10배 해소, 시간 효과 없음)
