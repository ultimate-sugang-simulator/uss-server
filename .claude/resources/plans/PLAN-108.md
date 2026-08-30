# [PLAN-108] 성능 측정 스킬의 관측 수단 정비

> 이슈: #108
> 브랜치: chore/108-perf-skill-observability

## 목표
optimize-performance 스킬에서 (1) 쿼리 출처 매핑을 서브에이전트 위임 없이 메인이 직접 하고,
(2) k6 측정 중 JVM, 풀, 캐시, Redis 상태를 주기 샘플링해 병목 판정 재료에 넣고,
(3) 실행계획 표에 노드별 실제 소요 ms와 쿼리 전체 소요를 계산해 보여준다.
겸해서 #104 이후 템플릿과 어긋난 `application-perf.yml`을 맞추고, 스킬 안의 "애플리케이션 캐시 없음" 전제를 걷어낸다.

## 사전 확인 (2026-08-30 조사)
- `query-source-mapper` 참조는 세 곳뿐이다: `SKILL.md:8` allowed-tools, `phase-4-baseline.md:22`, `phase-8-verify.md:27`.
  `query-stats-template.md`의 작성 규칙에는 서브에이전트를 전제한 문구가 없다 (규칙 1의 "Read로 읽고 같은 경로에 덮어쓴다"는 메인이 해도 그대로 맞는다). 이슈의 해당 항목은 확인 결과 변경 불필요.
- 위임이 토큰을 더 쓰는 이유: 워커가 템플릿, `record.md`, k6 요약, 직전 가공본을 전부 다시 읽는다. 메인은 Phase 1에서 예상 쿼리 목록을 이미 확인했으므로 목록에 있는 쿼리는 Grep 없이 매핑할 수 있다.
  Grep은 **목록에 없는 쿼리**에만 필요하다. 여기에 `spring_data_repository_invocations_seconds_count{repository,method}`를 더하면 메서드별 호출 수가 지표로 나와 매핑을 검증할 수 있다.
- perf 프로파일 앱이 떠 있어 `/actuator/prometheus` 지표 이름을 실측했다 (Micrometer 1.16.1, 지표군 약 90개). 라벨 순서는 `application`이 맨 앞이다.

  | 지표군 | 노출 | 형태 |
  |---|---|---|
  | `jvm_memory_used_bytes{area="heap",id=...}`, `jvm_memory_max_bytes` | 있음 | id별 3행. max는 Eden, Survivor가 `-1`, Old Gen만 Xmx(4 GiB) |
  | `jvm_gc_pause_seconds_count`, `_sum`, `_max` | 있음 | `cause`, `gc` 라벨별 여러 행. count, sum은 누적 카운터 |
  | `jvm_gc_overhead`, `jvm_memory_usage_after_gc{pool="long-lived"}`, `jvm_gc_memory_allocated_bytes_total`, `_promoted_bytes_total` | 있음 | 단일 |
  | `jvm_threads_live_threads`, `jvm_threads_states_threads{state}` | 있음 | states는 blocked, runnable, waiting 등 6행 |
  | `hikaricp_connections_{active,pending,timeout_total,acquire_seconds_max,usage_seconds_sum,usage_seconds_count}{pool="HikariPool-1"}` | 있음 | 단일 |
  | `http_server_requests_active_seconds_gcount`, `_max` | 있음 | `uri`, `method` 등 라벨별 |
  | `process_cpu_usage`, `system_cpu_usage` | 있음 | 단일 |
  | `cache_gets_total{cache,result=hit\|miss\|pending}`, `cache_puts_total`, `cache_removals_total` | 있음 | 캐시명별. 지금은 `major-courses` 하나 |
  | `lettuce_seconds_count`, `_sum`, `_max{db_operation}` | 있음 | Redis 명령별(GET, SET, ...) |
  | `spring_data_repository_invocations_seconds_count`, `_sum`, `_max{repository,method}` | 있음 | 리포지토리 메서드별 |
  | `tomcat_threads_*` | **없음** | `server.tomcat.mbeanregistry.enabled`가 꺼져 있다. **포함하지 않는다** (결정 1). 서버 포화는 `http_server_requests_active`로 본다 |

- Micrometer `_max` 계열(`gc_pause_seconds_max`, `acquire_seconds_max`, `requests_active_seconds_max`)은 최근 창(기본 2분)에서 감쇠하는 값이라 유휴 상태에서 0이다. 부하 중 주기 샘플링이어야 잡힌다.
- **`src/main/resources/application-perf.yml`이 템플릿과 다르다.** #104가 src 쪽에 `spring.data.redis`, `spring.cache`(type redis, `enable-statistics: true`, TTL 25h), `cache.major-courses.refresh-cron`을 넣었고 헤더 주석("원본은 템플릿이다", 선행 명령에 `redis`)도 바꿨다. 템플릿엔 없다.
  src의 캐시 주석은 "Phase 8이 actuator의 `cache_gets_total`로 hit / miss를 읽는다"고 적었지만 스킬에는 그 절차가 없다. 이번에 샘플러가 그 역할을 맡는다.
- 캐시 사용처: `course/infra/CourseCacheLoader`(`@Cacheable`, `@CachePut` `major-courses`, 키는 학과명), `course/infra/CourseCacheWarmer`, `global/config/RedisCacheConfig`(`CacheErrorHandler`로 Redis 장애 시 DB 폴백). compose 서비스는 `uss-mysql`, `uss-redis`.
- 스킬 안에서 "애플리케이션 캐시 없음"을 전제한 곳: `SKILL.md:61`, `PERF-template.md:50`, `k6-script-template.js:50`(`app_cache: '없음'`), `phase-2-environment.md:27`(compose에 `redis` 없음). `fix-concurrency`의 `CONCURRENCY-template.md:74`에도 같은 문구가 있으나 다른 스킬이라 이번 범위 밖.
- `commands.md` A 블록은 이미 `jq`, `python3`를 쓴다. 샘플러는 `curl`, `awk`만 쓰면 새 의존이 없다.
- `EXPLAIN ANALYZE`의 `actual time=A..B`는 loops당 평균이다 (`.claude/resources/perf/104/major/query-plan-*.txt`). 노드 실제 소요는 `B × loops`, 쿼리 전체 소요는 루트 노드의 B다.
  이 값은 결과 전송 시간을 뺀 서버 실행 시간이라 digest의 `mean_ms`보다 작게 나오는 것이 정상이다.

## 영향 범위
### 신규 파일
- `.claude/skills/_shared/jvm-sampler.sh` — `/actuator/prometheus` 주기 샘플링(`sample`), 가공본 생성(`summarize`), 지표 노출 점검(`check`)

### 삭제 파일
- `.claude/agents/query-source-mapper.md`

### 수정 파일
- `.claude/skills/optimize-performance/SKILL.md` — allowed-tools에서 `Agent(query-source-mapper)` 제거, 측정 스택 표와 산출물 규약에 JVM 가공본 추가, "애플리케이션 캐시도 없다" 문구 교체
- `.claude/skills/optimize-performance/phases/phase-2-environment.md` — compose에 `redis`, 템플릿과 src yml의 diff 점검, Redis ping과 샘플러 `check` 게이트 추가
- `.claude/skills/optimize-performance/phases/phase-4-baseline.md` — 위임 절차를 메인 직접 가공으로 교체, JVM 가공본 제시와 진단 표 행 추가
- `.claude/skills/optimize-performance/phases/phase-6-snapshot.md` — 노드별 표에 `소요 ms` 칼럼과 `쿼리 전체` 한 줄 추가
- `.claude/skills/optimize-performance/phases/phase-8-verify.md` — 위임 절차 교체, JVM 전후 비교 추가
- `.claude/skills/optimize-performance/template/commands.md` — A 블록에 샘플러 기동, 정지, 가공 단계 추가, 워밍업 주석에 Redis
- `.claude/skills/optimize-performance/template/PERF-template.md` — 캐시 상태 문구 교체, 기준선, 개선 전후, 최종 요약에 JVM 행과 쿼리 전체 소요 행 추가
- `.claude/skills/optimize-performance/template/application-perf.yml` — **삭제** (결정 4). src가 유일한 정의
- `src/main/resources/application-perf.yml` — 헤더 주석과 캐시 주석만 수정. 설정값은 그대로
- `.claude/skills/optimize-performance/template/perf-env.sh` — "Phase 2에서 파일을 만든 뒤" 경고 문구 정정
- `.claude/spec/secret-convention.md` — 템플릿 경로 참조를 src로
- `.claude/skills/optimize-performance/template/k6-script-template.js` — `app_cache` 자리표시자

> `query-stats-template.md`, `phase-1/3/5/7/9`, `optimize-performance/template/output.md`, `fix-concurrency/*`는 손대지 않는다.
> `fix-concurrency/template/application-conc.yml`도 같은 구조(템플릿 + src 둘 다 추적)이지만 다른 스킬이라 범위 밖. 별도 이슈 권장.
> open-issue 스킬이 `feat-issue-template.md`를 참조하지만 실제 파일은 `feature-issue-template.md`인 문제는 이 이슈 범위 밖이다.

## 구현 계획

### 1. `jvm-sampler.sh` (신규, `.claude/skills/_shared/`)

`mint-tokens.sh`와 같은 형태: 상단 주석에 목적, 사용법, 출력 형태. `set -euo pipefail`. 인자는 `--key value` 파싱.

```
bash jvm-sampler.sh check     [--url URL]
bash jvm-sampler.sh sample    --out FILE [--url URL] [--interval SEC]
bash jvm-sampler.sh summarize --in FILE --out FILE [--requests N]
```
- `URL` 기본 `http://localhost:8081/actuator/prometheus`, `SEC` 기본 `5` (결정 2).

**두 종류의 원본.** 게이지는 시계열이 필요하고, 라벨이 가변인 카운터(캐시명별, Redis 명령별, 메서드별)는 증분만 필요하다. 그래서 `sample`은 두 가지를 남긴다.
- `{out}` (CSV): 매 회 게이지와 대표 카운터 한 행
- `{out}.first.prom`, `{out}.last.prom`: 첫 스크랩과 마지막 스크랩 원문. 매 회 `.last.prom`을 덮어쓴다

**공통 파서 `scrape_row()`** - `curl -s -m 3 $URL` 출력을 awk 한 번에 넘겨 CSV 한 행을 만든다. 칼럼 순서 고정:

```
offset_s,heap_used_mb,heap_max_mb,old_after_gc_pct,gc_count,gc_pause_ms,gc_pause_max_ms,gc_overhead,threads_live,threads_blocked,hikari_active,hikari_pending,hikari_timeout_total,hikari_acquire_max_ms,http_active,http_active_max_ms,process_cpu,system_cpu
```

| 칼럼 | 계산 |
|---|---|
| `heap_used_mb` | `jvm_memory_used_bytes` 중 `area="heap"` 행의 합 / 1048576 |
| `heap_max_mb` | `jvm_memory_max_bytes` 중 `area="heap"`이고 값 > 0인 행의 합 / 1048576 (G1은 Old Gen 한 행이 Xmx다) |
| `old_after_gc_pct` | `jvm_memory_usage_after_gc{pool="long-lived"}` × 100 |
| `gc_count`, `gc_pause_ms` | `jvm_gc_pause_seconds_count`, `_sum` 전 행의 합. `_sum`은 × 1000 |
| `gc_pause_max_ms` | `jvm_gc_pause_seconds_max` 전 행 중 최댓값 × 1000 |
| `gc_overhead` | `jvm_gc_overhead` 그대로 (0~1) |
| `threads_live`, `threads_blocked` | `jvm_threads_live_threads`, `jvm_threads_states_threads{state="blocked"}` |
| `hikari_*` | 해당 지표 그대로. `acquire_seconds_max` × 1000 |
| `http_active`, `http_active_max_ms` | `http_server_requests_active_seconds_gcount` 전 행의 합, `_max` 전 행 중 최댓값 × 1000 |
| `process_cpu`, `system_cpu` | 그대로 |

- 라벨은 `application`이 맨 앞이므로 `{area="heap"` 같은 접두 매칭을 쓰지 않는다. awk에서 `index($0, "area=\"heap\"")`으로 잡는다.
- 지표군이 응답에 없으면 그 칼럼을 **빈 값**으로 둔다. 0으로 채우지 않는다. summarize가 빈 칼럼을 `미수집`으로 보고한다.
- 지수 표기(`1.09051904E8`)는 awk가 숫자로 읽는다. printf로 고정 소수점 출력.

**`check`** - 한 번 긁어 지표군 7개(heap, gc, threads, hikari, http active, process cpu, system cpu)의 존재를 `있음 / 없음`으로 한 줄씩 출력하고, 하나라도 없으면 exit 1. 캐시, Redis, 리포지토리 지표는 대상에 따라 없을 수 있으므로 게이트가 아니고 `있음 / 없음`만 참고로 찍는다.
`없음`에는 조치를 붙인다: hikari, http active → "메인 포트로 요청 1회 뒤 재시도", gc → "GC가 아직 한 번도 안 돈 것. 워밍업 뒤 재시도", 그 외 → "perf 프로파일로 떴는지 1)을 확인". curl 자체가 실패하면 "앱이 8081에 떠 있지 않다"로 exit 1.

**`sample`** - `trap 'exit 0' TERM INT`. 파일이 없으면 헤더를 쓴다. 첫 스크랩을 `.first.prom`에 저장. `START=$(date +%s)`, 매 회 `offset_s = now - START`로 한 행 append하고 원문을 `.last.prom`에 덮어쓴 뒤 `sleep $INTERVAL`. 종료 신호를 받을 때까지 돈다.

**`summarize`** - CSV와 `.first.prom`, `.last.prom`을 읽어 아래 md를 `--out`에 쓴다. **증분이 전부 0인 구획은 표 대신 한 줄로 접는다** (결정 3).

```
# jvm-metrics-{n}

샘플 {N}건 / 간격 {s}s / 구간 0~{last}s / 요청 {requests}건

## 게이지 (샘플 중 최대, 평균)

| 지표 | 최대 | 평균 | 최대 시점(s) | 기준 |
|---|---|---|---|---|
| heap 사용 (MB) | | | | heap max {M} MB |
| GC 후 old gen 점유 (%) | | | | 바닥이 오르면 누수나 캐시 적재 |
| GC 최장 정지 (ms) | | - | | |
| GC overhead | | | | GC가 쓴 CPU 비율 |
| 스레드 수 / blocked | | | | blocked > 0이면 락 경합 |
| HikariCP active | | | | 풀 크기는 record.md 측정 환경 |
| HikariCP pending | | | | 0보다 크면 커넥션 대기 |
| HikariCP acquire max (ms) | | - | | |
| 처리 중 요청 수 / 최장 (ms) | | | | 서버 안 동시 요청 |
| process CPU | | | | 0~1 |
| system CPU | | | | 0~1 |

## 누적 (측정 구간 증분)

| 지표 | 시작 | 끝 | 증분 | 요청당 |
|---|---|---|---|---|
| GC 횟수 | | | | |
| GC 일시정지 합 (ms) | | | | |
| 할당량 (MB) | | | | |
| old gen 승격량 (MB) | | | | |
| HikariCP timeout | | | | |
| 커넥션 보유 평균 (ms) | - | - | usage_sum 증분 / usage_count 증분 | - |

## 리포지토리 호출

| repository.method | 호출 증분 | 요청당 | mean ms |
|---|---|---|---|
(호출 증분 > 0인 메서드만, 증분 내림차순)

## 캐시

| 캐시 | hit | miss | pending | put | removal | 적중률 |
|---|---|---|---|---|---|---|
(어느 캐시든 증분 > 0일 때만. 아니면 `캐시: 측정 구간 접근 없음`)

## Redis

| 명령 | 호출 증분 | 요청당 | mean ms | max ms |
|---|---|---|---|---|
(증분 > 0인 명령만. 아니면 `Redis: 측정 구간 호출 없음`)

## 타임라인

| offset_s | heap_used_mb | gc_pause_ms(증분) | gc_pause_max_ms | threads_blocked | hikari_active | hikari_pending | http_active | process_cpu |
|---|---|---|---|---|---|---|---|---|
```
- `{n}`은 `--out` 파일명(`jvm-metrics-{n}.md`)에서 딴다. 파일명이 그 형태가 아니면 파일명 그대로 쓴다.
- `--requests`가 없으면 "요청당" 칼럼은 `-`.
- 최대 시점은 그 칼럼이 최댓값을 처음 기록한 행의 `offset_s`.
- 타임라인의 `gc_pause_ms(증분)`는 직전 행과의 차. 첫 행은 0.
- 증분은 `.last.prom` - `.first.prom`. 라벨 집합이 다르면(끝에만 있는 메서드) 시작을 0으로 본다.
- 캐시 적중률 = hit / (hit + miss). 분모 0이면 `-`.
- 빈 칼럼은 요약 표에 `미수집`, 타임라인에 `-`.
- 소수 자릿수: MB와 ms는 1자리, CPU와 overhead는 3자리, 적중률은 % 1자리. 나머지는 정수.

### 2. `commands.md` A 블록

워밍업 주석에 Redis를 넣고, 5) 리셋과 6) 측정 사이에 샘플러 기동, 측정 뒤에 요청 수 확인, 샘플러 정지와 가공을 넣는다. 번호를 다시 매긴다.

```bash
# 2) 워밍업 (JIT, 커넥션 풀, InnoDB 버퍼 풀, Redis 캐시). 이 실행의 결과는 쓰지 않는다
k6 run -e PHASE=warmup $TARGET_DIR/test-script.js

# ... 3) 4) 기존 그대로

# 5) 쿼리 통계 리셋
mysqlp -e "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest;"

# 6) JVM 샘플러. 측정과 함께 돌고 8)에서 멈춘다
bash .claude/skills/_shared/jvm-sampler.sh sample --out $TARGET_DIR/jvm-samples-{n}.csv &
SAMPLER_PID=$!

# 7) 측정
k6 run -e PHASE=measure -e SUMMARY_OUT=$TARGET_DIR/k6-test-summary-{n}.json $TARGET_DIR/test-script.js

# 8) 샘플러 정지, 요청 수 확인, 가공. 원본은 가공본에 전부 들어가므로 지운다
kill $SAMPLER_PID; wait $SAMPLER_PID 2>/dev/null
REQS=$(jq -r '.requests // empty' $TARGET_DIR/k6-test-summary-{n}.json)
if ! [ "$REQS" -gt 0 ] 2>/dev/null; then
  echo "요청 수가 '$REQS'다. 측정이 실패했으므로 가공과 통계 수집을 하지 않는다. 원인을 확인하고 재측정하라."
else
bash .claude/skills/_shared/jvm-sampler.sh summarize \
  --in $TARGET_DIR/jvm-samples-{n}.csv --out $TARGET_DIR/jvm-metrics-{n}.md --requests $REQS \
  && rm $TARGET_DIR/jvm-samples-{n}.csv $TARGET_DIR/jvm-samples-{n}.csv.first.prom $TARGET_DIR/jvm-samples-{n}.csv.last.prom

# 9) 쿼리 통계 수집 (기존 mysqlp -B 블록 그대로)
mysqlp -B -e "..." | tee $TARGET_DIR/query-stats-summary-{n}.md
fi
```

이유 표에 추가:

| 요소 | 이유 |
|---|---|
| 워밍업에 Redis | `major-courses` 같은 캐시는 첫 요청이 채운다. 워밍업 없이 재면 miss 구간이 측정에 섞인다 |
| 샘플러를 리셋 뒤, 측정 앞에 띄움 | 워밍업의 GC와 heap이 섞이지 않는다. 샘플의 `offset_s` 0이 측정 시작이고 `.first.prom`이 증분의 기준점이다 |
| `kill` 뒤 `wait` | 마지막 행과 `.last.prom`을 다 쓴 뒤에 가공한다 |
| `REQS`를 가공 앞으로 | 샘플러 가공본의 "요청당" 칼럼과 쿼리 통계의 `per_req`가 같은 분모를 쓴다 |
| 원본 CSV, prom 삭제 | 산출물 규약대로 1차 출력은 남기지 않는다. 타임라인과 증분이 가공본에 전부 있다 |

### 3. `SKILL.md`

- frontmatter `allowed-tools`: `Agent(query-source-mapper)` 제거.
- **측정 스택** 표에 행 추가: `| JVM, 커넥션 풀, 캐시, Redis | /actuator/prometheus 주기 샘플링 (_shared/jvm-sampler.sh) |`
- 61행 교체: "InnoDB 버퍼 풀은 재기동 없이 비울 수 없고, Redis 캐시(`major-courses`)는 워밍업이 채운다. 측정은 **warm으로 통일**하고 매번 같은 워밍업으로 상태를 맞춘다. 캐시를 비운 상태를 재려면 그 사실과 방법(`redis-cli FLUSHDB`)을 `record.md` 측정 환경에 적는다."
- **산출물 규약** 트리와 표에 `jvm-metrics-{n}.md` 추가 (만드는 Phase 4, 8 / 템플릿 `jvm-sampler.sh summarize` 출력).
- **측정 산출물의 형태** 목록에 추가: "`jvm-metrics-{n}.md`는 스크립트가 만든 완성본이다. 스킬은 읽기만 하고 다시 쓰지 않는다. 캐시, Redis, 리포지토리 구획은 측정 구간에 증분이 있을 때만 나타난다. 구획이 없다는 것은 그 대상이 거기 닿지 않았다는 관측이다."

### 4. `application-perf.yml` 단일화 (결정 4)

- `template/application-perf.yml`을 `git rm`한다. 템플릿은 src에 파일이 없던 시절(스킬 도입 `473508b`)의 "없으면 생성"용이었고, #100(`7ac566b`)에서 src가 커밋된 뒤로는 존재 이유가 없다.
- src 헤더 주석: "성능 측정 전용 프로파일. optimize-performance 스킬의 Phase 2가 이 파일을 확인한다. 이 파일이 유일한 정의다."
- src의 캐시 주석 "Phase 8이 actuator의 `cache_gets_total{result="hit"|"miss"}`로 hit / miss를 읽는다"를 "jvm-sampler.sh가 `cache_gets_total{result="hit"|"miss"}`로 적중률을 만든다"로 바꾼다.
- 그 외 값은 손대지 않는다.
- `perf-env.sh`의 secret-key 경고 문구에서 "Phase 2에서 파일을 만든 뒤"를 "레포 루트에서 source했는지 확인"으로 바꾼다.

### 5. `phase-2-environment.md`

- 참조 파일 목록의 템플릿 경로를 `src/main/resources/application-perf.yml`로 교체.
- 절차 1 교체: "`src/main/resources/application-perf.yml`을 Read해 `maximum-pool-size`, `show_sql`, 드라이버, Redis 접속을 확인한다. 이 파일이 perf 프로파일의 유일한 정의다." 복사와 diff 절차는 두지 않는다.
- 절차 2의 compose 명령: `up -d mysql redis`.
- 절차 3에 항목 추가:
  ```bash
  # 8) Redis (PONG). 없으면 CacheErrorHandler가 DB로 폴백해 캐시 없는 상태를 재게 된다
  docker exec uss-redis redis-cli ping

  # 9) JVM 샘플러가 긁을 지표가 다 있는가 (게이트 7개 모두 "있음")
  bash .claude/skills/_shared/jvm-sampler.sh check
  ```
- 실패 조치 추가: 8) 실패 → `docker-compose ... up -d redis`. 9) `hikaricp`, `http` 없음 → 2)를 메인 포트로 보냈는지 확인. `jvm_gc_pause` 없음 → 2)를 몇 번 더 보내고 재시도. 그 외 → 1)로 돌아가 perf 프로파일로 떴는지 확인.
- 절차 5와 출력의 캐시 상태: "warm 고정, Redis 캐시 워밍업으로 적재".
- 다음 Phase 조건의 "여덟 항목"을 "열 항목"으로.

### 6. `phase-4-baseline.md`

절차 2~4를 교체한다.

```
2. 끝나면 `k6-test-summary-0.json`, `jvm-metrics-0.md`, 1차 출력 `query-stats-summary-0.md`를 Read한다.
   - 파일이 없으면 원인을 확인하고 재실행을 요청한다. 추정으로 채우지 마라.
   - `checks_rate`가 1이 아니면 `checks[]`에서 어떤 항목이 깨졌는지 먼저 본다. 데이터 검증 check가 깨진 측정은 진단에 쓰지 않는다.
   - `jvm-metrics-0.md`의 게이지 표에 `미수집`이 있으면 Phase 2의 9)를 다시 통과시킨 뒤 재측정한다.

3. `query-stats-template.md`의 작성 규칙대로 가공본을 같은 경로에 덮어쓴다.
   출처는 `record.md`의 예상 쿼리 목록과 `jvm-metrics-0.md`의 **리포지토리 호출** 표로 맞춘다.
   목록에 있는 쿼리는 Phase 1에서 이미 확인했으므로 Grep하지 않는다. 리포지토리 호출 표의 메서드별 호출 증분과 digest의 `calls`가 맞아떨어지면 그것이 출처 확인이다.
   목록에도 표에도 없는 쿼리만 테이블명과 컬럼 조합으로 Grep해 확인하고, 못 찾으면 `미상`으로 둔다. 미상이 남는 것은 정상이다.

4. 가공본을 k6 요약, JVM 가공본과 함께 제시하고 **병목 판정을 묻는다** (`SKILL.md`의 **역할 경계**).
   - 제시할 것: 응답시간 분포, 처리량, check 결과, 쿼리별 요청당 호출 수, 총 시간 비중, `examined_per_sent`,
     heap 최대와 heap max, GC 일시정지 합과 최장 정지, GC overhead, blocked 스레드 최대, HikariCP pending 최대와 acquire max, 커넥션 보유 평균,
     처리 중 요청 최대, process CPU 최대. 캐시와 Redis 구획이 있으면 적중률과 Redis 명령별 시간도 함께.
   - 물을 것: "요청당 쿼리 수와 시간이 쏠린 지점, 그리고 JVM과 풀, 캐시의 상태를 보고, 병목의 성격을 어떻게 판단하십니까?"
```

진단 표에 행 추가 (기존 "단건은 빠른데 VU를 올리면 급락" 행 아래):

| 관측 | 진단 | 유력한 기법 |
|---|---|---|
| HikariCP pending 최대 > 0, acquire max가 p95에 근접 | 커넥션 대기 | 풀 크기, 트랜잭션 범위 축소, 쿼리 수 감소 |
| 커넥션 보유 평균이 쿼리 mean_ms 합보다 훨씬 큼 | 트랜잭션이 커넥션을 오래 쥠 | 트랜잭션 범위 축소, 직렬화를 트랜잭션 밖으로 |
| GC overhead가 크거나 최장 정지가 p99에 근접, heap 최대가 heap max에 근접 | 메모리 압박 | 응답 크기 축소, 불필요한 엔티티 로딩 제거, 힙 설정 |
| 할당량 요청당 값이 응답 크기보다 훨씬 큼 | 요청 중 버려지는 객체가 많음 | DTO projection, 컬렉션 가공 축소 |
| blocked 스레드 > 0 | 애플리케이션 락 경합 | 락 범위 축소, 락 없는 구조 |
| 캐시 적용 대상인데 hit 증분 0, miss만 증가 | 캐시 미적중 (키 불일치, TTL, 워밍업 누락) | 키 설계, 워밍업 |
| Redis 명령 시간 합이 쿼리 total_ms에 근접 | 캐시 왕복이 병목 | 직렬화 크기 축소, 로컬 캐시 계층 |
| process CPU가 높고 GC는 조용한데 DB 시간 비중이 낮음 | 애플리케이션 연산 | 컬렉션 가공, 직렬화 경로 |
| DB와 JVM 지표가 모두 여유인데 API가 느림 | 직렬화, 응답 크기 | DTO 축소, 페이징 |

출력에 `jvm-metrics-0.md` 추가.

### 7. `phase-6-snapshot.md`

절차 4의 노드별 표와 칼럼 설명을 교체한다.

```
4. 계획을 노드별 표와 카운터 표로 정리해 대화에 제시한다. 파일에는 쓰지 않는다.
   표 위에 `쿼리 전체: {루트 노드 actual time의 뒤 값} ms` 한 줄을 적는다.

   | 노드 | 접근 방식 / 인덱스 | actual time | loops | 소요 ms | 추정 rows | 실측 rows | 비고 |
   |---|---|---|---|---|---|---|---|
```

칼럼 설명 표에서 `actual time=A..B` 행을 아래로 바꾸고 `소요 ms` 행을 추가:

| 칼럼 | 설명 |
|---|---|
| actual time=A..B | A는 첫 행까지, B는 마지막 행까지(ms). **loops당 평균**이고 자식 노드 시간을 포함한다 |
| 소요 ms | `B × loops`. 그 노드가 자식까지 포함해 실제로 쓴 시간. 부모의 소요를 넘지 않는다. 루트의 소요가 쿼리 전체다 |

`쿼리 전체`에 대한 주의를 한 줄 붙인다: "결과 전송 시간을 뺀 서버 실행 시간이라 digest의 `mean_ms`보다 작은 것이 정상이다. 둘의 차가 크면 전송량(응답 크기)을 본다."

### 8. `phase-8-verify.md`

- 절차 2: "쿼리 통계는 3의 위임이 끝난 뒤 가공본을 읽는다" 삭제. Read 목록에 `jvm-metrics-{n}.md`와 `-{n-1}` 추가.
- 절차 3의 `query-stats-summary-{n}.md` 항목을 교체:
  "Phase 4의 3과 같은 방법으로 메인이 가공본을 쓴다. `n >= 1`이므로 직전 가공본 `query-stats-summary-{n-1}.md`를 Read해 헤더의 **직전 상태 대비**를 채운다."
- 절차 4의 하드웨어 의존 증거에 추가: "쿼리 전체 소요(EXPLAIN ANALYZE 루트), heap 최대, GC 일시정지 합과 최장 정지, HikariCP pending 최대, 커넥션 보유 평균, process CPU 최대."
  하드웨어 독립 증거에 추가: "요청당 리포지토리 호출 수, 요청당 할당량, 캐시 적중률 (구획이 있을 때)."
- 출력에 `jvm-metrics-{n}.md` 추가.

### 9. `PERF-template.md`

- 50행 캐시 상태: "warm 고정. InnoDB 버퍼 풀은 재기동 없이 비울 수 없고, Redis 캐시는 워밍업이 채운다. 매 측정 전 같은 워밍업으로 맞춘다. {비운 상태를 쟀으면 그 방법}".
- **기준선** 표에 행 추가: `heap 최대 / heap max`, `GC 일시정지 합 / 최장`, `GC overhead`, `HikariCP pending 최대 / acquire max`, `커넥션 보유 평균`, `blocked 스레드 최대`, `process CPU 최대`, `캐시 적중률 (구획 없으면 -)`.
- **실행계획** 항목에 추가: `- 쿼리 전체 소요: {ms} (루트 노드)`, `- 비용 상위 노드: {노드} {소요 ms} / {노드} {소요 ms}`.
- **개선 전 지표** 표에 행 추가: `쿼리 전체 소요 (EXPLAIN ANALYZE)`, `GC 일시정지 합 / HikariCP pending 최대`, `요청당 리포지토리 호출 수 / 할당량`.
- **개선 후 지표** 표: 하드웨어 의존 구분에 `쿼리 전체 소요`, `heap 최대`, `GC 일시정지 합 / 최장`, `HikariCP pending 최대`, `커넥션 보유 평균`, `process CPU 최대` 추가.
  하드웨어 독립 구분에 `요청당 리포지토리 호출 수`, `요청당 할당량 (MB)` 추가. 기존 `캐시 hit / miss, 적중률` 행은 유지.
- **최종 요약** 표에 같은 행 추가.

### 10. `k6-script-template.js`

- 50행 `app_cache: '없음'` → `app_cache: '{Redis warm / 없음}'`. 작성 규칙 1의 "고치는 자리" 목록은 이미 CONDITION을 포함하므로 규칙 문장은 그대로.

### 11. `.claude/agents/query-source-mapper.md` 삭제

`git rm`. 삭제 뒤 `grep -rn query-source-mapper .claude/`가 0건이어야 한다.

## 결정 필요 (Decisions needed)
- [x] **1. Tomcat 스레드 지표 포함 여부** — A) 포함: `mbeanregistry.enabled: true`를 perf yml 두 곳에 넣고 앱을 재기동한다 / B) 제외
  → **B 확정 (사용자 선택, 2026-08-30).** 서버 포화는 `http_server_requests_active`(처리 중 요청 수)로 본다. 이 지표는 설정 없이 이미 노출된다.
- [x] **2. 샘플링 간격 기본값** — A) 5초 / B) 2초
  → **A 확정 (사용자 선택, 2026-08-30).** `--interval`로 바꿀 수 있다.
- [x] **3. 캐시, Redis 지표를 언제 넣는가** — A) 스킬이 Phase 1에서 실행 경로를 읽고 캐시 대상일 때만 구획을 켠다 / B) 샘플러는 전부 긁고 summarize가 측정 구간 증분이 0인 구획을 접는다
  → **B 확정 (사용자 요청 "사용하는 상황에 동적으로", 2026-08-30).** 설정이 아니라 관측값이 구획을 정한다. `@CacheEvict`처럼 간접적으로 닿는 경우를 코드 읽기로 놓치지 않고, 캐시 대상인데 hit 증분이 0인 것 자체가 진단 근거가 된다. 리포지토리 호출 표에도 같은 규칙.
- [x] **4. 템플릿과 src `application-perf.yml` 불일치** — A) 이번 이슈에서 함께 맞춘다 / B) 별도 이슈
  → **A 확정 (사용자 선택, 2026-08-30).** 처음엔 "src를 정본으로 템플릿을 동기화하고 Phase 2가 diff로 감시"로 구현했으나, 구현 뒤 사용자가 템플릿의 존재 이유를 물어 이력을 확인한 결과
  템플릿은 src에 파일이 없던 시절의 "없으면 생성"용이었고 #100 이후 그 경우가 없다. 같은 정의를 두 곳에 두고 게이트로 드리프트를 막는 것은 덧대기라서 **템플릿 삭제, src 단일 정본**으로 재확정 (사용자 선택, 2026-08-30).
  이에 따라 스킬의 "애플리케이션 캐시 없음" 전제(SKILL.md, PERF-template, k6 템플릿, phase-2)도 함께 걷는다.

## 검증
- `jvm-sampler.sh check`를 지금 떠 있는 perf 앱에 대해 실행해 게이트 7개 모두 `있음`, exit 0인지 본다. 앱을 내린 상태(또는 잘못된 `--url`)에서는 exit 1과 안내 메시지가 나오는지 본다.
- `sample --out {scratch}/s.csv --interval 1 &` 로 10초 돌리고 `kill`, `wait` 뒤 CSV가 헤더 + 약 10행이고 빈 칼럼이 없는지, `.first.prom`, `.last.prom`이 있는지 확인. 그 사이 `curl localhost:8080/api/v1/courses/major`(토큰 필요, `mint-tokens.sh`)를 몇 번 쳐서 hikari_active, http_active가 0이 아닌 행과 리포지토리, 캐시, Redis 증분이 잡히는지 본다.
- 같은 방식으로 캐시에 닿지 않는 엔드포인트(`/api/v1/courses/terms`)만 친 뒤 summarize해 캐시와 Redis 구획이 "측정 구간 호출 없음" 한 줄로 접히는지 본다 (결정 3의 동작 확인).
- `summarize`의 요약 표 최대, 평균, 최대 시점, 누적 표 증분, 적중률을 CSV와 prom 원문과 손으로 대조한다. 빈 칼럼이 `미수집`으로 나오는지 본다.
- `commands.md` A 블록 6)~9)를 zsh에서 그대로 실행해 `$!`, `kill`, `wait`, `if` 블록이 zsh와 bash 모두에서 동작하는지 확인 (`perf-env.sh`가 둘 다 지원한다고 명시하므로).
- `grep -rn "template/application-perf" .claude/` 0건. `git ls-files | grep application-perf`가 src 한 건만. 그 뒤 Phase 2 게이트 0)~9)가 전부 통과하는지 본다.
- `grep -rn "query-source-mapper" .claude/` 0건. `grep -rn "애플리케이션 캐시" .claude/skills/optimize-performance/` 0건. `SKILL.md` frontmatter의 `allowed-tools`가 한 줄로 유효한지 확인.
- 스킬 텍스트 정합성: phase-2의 "열 항목", phase-4와 8의 출력 목록, SKILL.md 산출물 표와 트리가 서로 같은 파일명(`jvm-metrics-{n}.md`)을 쓰는지 grep으로 대조.
- 실행 가능한 템플릿은 커밋 전 실행한다는 규칙을 따른다. 위 항목 중 스크립트 실행은 전부 커밋 전에 한다.

## Deviation Log
> implement 스킬이 구현 중 계획을 벗어난 지점을 여기에 기록한다. (작성 시점엔 비워둔다)

- 검증 "yml 반영 후 재기동": 생략 — 이유: src `application-perf.yml`은 주석 두 줄만 바뀌어 동작 변화가 없다. Phase 2 게이트 8)(Redis PONG), 9)(`check` 7개 있음)는 떠 있는 앱에서 3회 연속 통과를 확인했다.
- `template/application-perf.yml`: 동기화가 아니라 삭제, `phase-2` 절차 1의 diff 게이트 제거, `perf-env.sh` 경고 문구 정정 — 이유: 결정 4 재확정 (사용자, 2026-08-30). 구현 중 사용자 질문으로 템플릿이 존재 이유를 잃은 사실이 드러났다. 계획서 4, 5절과 영향 범위를 그에 맞게 고쳤다.
- 검증 "`grep -rn query-source-mapper .claude/` 0건": 스킬, 에이전트 디렉토리 기준 0건 — 이유: `resources/plans/PLAN-96.md`(도입 이력)와 이 계획서 자체에는 이름이 남는다. 이력은 지우지 않는다.
- `jvm-sampler.sh check`: 지표 존재 판정을 `grep -q`가 아니라 `grep -c`로 — 이유: 92KB 스크랩을 `printf`로 흘리는데 `-q`가 첫 매치에서 끝나면 `printf`가 SIGPIPE를 받고 `pipefail`이 실패로 판정해 결과가 실행마다 달랐다(cache가 늘 "없음", threads가 간헐 "없음"). 끝까지 읽는 `-c`로 3회 연속 안정 확인.
- `jvm-sampler.sh summarize`: 증분 계산의 키를 `$1`이 아니라 "값 앞까지 전부"로 — 이유: `jvm_gc_pause_seconds_count{action="end of minor GC",...}`처럼 라벨에 공백이 있어 `$1`로 자르면 GC 두 행이 한 키로 뭉개져 472가 1로 나왔다. 원문 합계(472 → 473)와 일치 확인.
