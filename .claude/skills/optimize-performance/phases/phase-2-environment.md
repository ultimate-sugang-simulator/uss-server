## Phase 2. 측정 환경 검증

### 목적
측정값을 왜곡하는 설정이 없는지, 쿼리 단위 관측 도구가 유효한지 확인한다.

**게이트.** 하나라도 통과하지 못하면 넘어가지 마라. 호출자에게 조치를 요청한다.

### 선행 조건
- Phase 1 완료

### 참조 파일
- `src/main/resources/application-perf.yml`
- `.claude/skills/optimize-performance/template/perf-env.sh`

### 절차

1. `src/main/resources/application-perf.yml`을 Read해 `maximum-pool-size`, `show_sql`, 드라이버, Redis 접속을 확인한다.
   이 파일이 perf 프로파일의 유일한 정의다. SQL 로깅이나 p6spy가 켜져 있으면 끄도록 요청한다.
   요청당 수십 줄을 찍는 로깅은 측정값을 통째로 바꾼다.

2. 셸 환경과 서버 기동을 제시한다. 애플리케이션은 별도 터미널에서 띄우게 하고, 기동 완료를 확인받은 뒤 3으로 간다.

   ```bash
   # 측정 터미널 (레포 루트)
   source .claude/skills/optimize-performance/template/perf-env.sh {이슈번호} {슬러그}
   docker-compose -f docker/docker-compose-local.yml up -d mysql redis

   # 애플리케이션 터미널
   ./gradlew bootRun --args='--spring.profiles.active=perf'
   ```

3. 아래를 **순서대로** 실행하게 하고 결과를 받는다. 3)은 2)가 만든 미터를 세므로 2)를 건너뛰지 마라.

   ```bash
   # 0) DB 접속과 charset. 한글이 ?로 보이면 perf-env.sh가 source되지 않은 것이다
   mysqlp -e "SELECT title_kr FROM courses LIMIT 1;"

   # 1) perf 프로파일로 떠 있는가 (application="uss-server-perf")
   curl -s localhost:8081/actuator/prometheus | grep -m1 'application='

   # 2) 미터 등록용 1회 요청. 메인 포트(8080)여야 한다. 관리 포트(8081) 요청은 메인 서버 미터를 만들지 않는다. 401이어도 기록된다
   curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/v1/courses/terms

   # 3) 응답시간 히스토그램 (0이면 SLO 버킷 설정이 안 붙은 것)
   curl -s localhost:8081/actuator/prometheus | grep -c http_server_requests_seconds_bucket

   # 4) performance_schema와 digest 소비자 (셋 다 ON / YES)
   mysqlp -e "
   SELECT @@performance_schema AS ps;
   SELECT NAME, ENABLED FROM performance_schema.setup_consumers
   WHERE NAME IN ('statements_digest', 'events_statements_current');"

   # 5) digest 테이블이 채워지는가 (0보다 커야 한다)
   mysqlp -e "
   SELECT count(*) FROM performance_schema.events_statements_summary_by_digest
   WHERE SCHEMA_NAME = 'uss_db';"

   # 6) digest 원문이 잘리는가. 가장 긴 digest가 max_digest_length에 닿으면 잘리는 것이다
   mysqlp -e "
   SELECT @@performance_schema_max_digest_length AS max_len;
   SELECT LENGTH(DIGEST_TEXT) AS len, LEFT(DIGEST_TEXT, 80) AS head
   FROM performance_schema.events_statements_summary_by_digest
   WHERE SCHEMA_NAME = 'uss_db' ORDER BY len DESC LIMIT 3;"

   # 7) 버퍼 풀 크기. 데이터가 이보다 크면 디스크 I/O가 측정에 섞인다
   mysqlp -e "SELECT @@innodb_buffer_pool_size / 1024 / 1024 AS buffer_pool_mib;"

   # 8) Redis (PONG). 없으면 CacheErrorHandler가 DB로 폴백해 캐시 없는 상태를 재게 된다
   docker exec uss-redis redis-cli ping

   # 9) JVM 샘플러가 긁을 지표가 다 있는가 (게이트 7개 모두 "있음")
   bash .claude/skills/_shared/jvm-sampler.sh check
   ```

4. 실패 항목의 조치:
   - 3)이 0 → 설정을 의심하기 전에 2)를 메인 포트로 보냈는지 확인한다.
   - 8) 실패 → `docker-compose -f docker/docker-compose-local.yml up -d redis`.
   - 9)에서 `hikari`, `http_active` 없음 → 2)를 메인 포트로 보냈는지 확인한다. `gc` 없음 → 아직 GC가 없던 것이니 2)를 몇 번 더 보내고 재시도한다.
     그 외가 없음 → 1)로 돌아가 perf 프로파일로 떴는지 확인한다.
   - 4)의 `ps`가 0 → `docker/docker-compose-local.yml`의 mysql `command`에 `--performance-schema=ON`을 추가하고 컨테이너를 재기동한다.
   - 소비자가 `NO` → 재기동 없이 켠다.

     ```bash
     mysqlp -e "
     UPDATE performance_schema.setup_consumers SET ENABLED = 'YES'
     WHERE NAME IN ('statements_digest', 'events_statements_current');"
     ```

   - 5)가 0 → 대상 API를 한 번 호출하게 한 뒤 다시 센다. 그래도 0이면 4)로 돌아간다.
   - 6)에서 잘림 → `--performance-schema-max-digest-length=4096`과 `--performance-schema-max-sql-text-length=4096`을 mysql `command`에 넣고 재기동한다.
     MySQL 8.0.28+는 digest에서 `IN` 목록을 `IN (...)`로 접으므로 `@BatchSize`의 자리표시자 1000개도 100자 미만이다.
     실측 길이가 닿지 않으면 올리지 마라.

5. 결과를 `record.md`의 **측정 환경**에 적는다. 프로파일, 커넥션 풀 크기(`maximum-pool-size`), 버퍼 풀 크기, 캐시 상태(warm 고정, Redis 캐시는 워밍업으로 적재).
   cold 측정을 설계하지 마라. Phase 4와 8이 같은 워밍업으로 버퍼 풀과 Redis 상태를 맞춘다.

### 출력
- `record.md`의 **측정 환경**에 프로파일, 풀 크기, 버퍼 풀 크기, 캐시 상태, 진행 상태 Phase 2 ✅

> 다음 Phase 조건: 3의 열 항목이 모두 통과했을 때 → Phase 3
>
> Skip 조건: 같은 이슈의 다른 대상에서 통과했고 그 사이에 애플리케이션과 컨테이너를 재기동하지 않았으면,
> 앞선 대상의 **측정 환경**을 옮겨 적고 ⏭️로 표기한다.
