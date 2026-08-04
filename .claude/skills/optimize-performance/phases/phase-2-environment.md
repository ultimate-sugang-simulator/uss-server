## Phase 2. 측정 환경 검증

### 목적
측정값을 왜곡하는 설정은 없는지, 쿼리 단위 관측 도구가 유효한지 확인한다.

**게이트.** 하나라도 충족하지 못하면 다음 Phase로 넘어가지 마라. 호출자에게 조치를 요청한다.

### 선행 조건
- Phase 1 완료

### 참조 파일
- `src/main/resources/application-perf.yml`
- `.claude/skills/optimize-performance/template/application-perf.yml`

### 절차

1. `src/main/resources/application-perf.yml`이 있는지 Glob으로 확인한다.
   - 없으면 `template/application-perf.yml`을 Read해 그 내용으로 생성한다.
     이 파일은 측정 전용이다. 프로파일을 새로 만드는 것이므로 만들었다는 사실을 호출자에게 알린다.
   - 있으면 Read해 `maximum-pool-size`, `show_sql`, p6spy 설정을 확인한다.
     **SQL 로깅이 켜져 있으면 끄도록 요청한다.** 요청당 수십 줄을 찍는 로깅은 측정값을 통째로 바꾼다.

2. MySQL과 애플리케이션을 기동하도록 호출자에게 제시한다. 별도 터미널에서 실행하게 하고, 기동 완료를 확인받은 뒤 3번으로 간다.

   ```bash
   docker-compose -f docker/docker-compose-local.yml up -d mysql
   ./gradlew bootRun --args='--spring.profiles.active=perf'
   ```

3. 아래를 **적힌 순서대로** 실행하도록 제시하고 결과를 받는다.
   3)은 요청이 한 번 들어온 뒤에만 값이 잡히므로 2)를 건너뛰지 마라.

   ```bash
   # 1) perf 프로파일로 떠 있는가 (application 태그가 uss-server-perf여야 한다)
   curl -s localhost:8081/actuator/prometheus | grep -m1 'application='

   # 2) 미터 등록용 1회 요청
   curl -s -o /dev/null -w '%{http_code}\n' localhost:8081/actuator/health

   # 3) 응답시간 히스토그램이 노출되는가 (0이면 SLO 버킷 설정이 안 붙은 것)
   curl -s localhost:8081/actuator/prometheus | grep -c http_server_requests_seconds_bucket

   # 4) performance_schema와 statements_digest 소비자가 켜져 있는가 (셋 다 ON이어야 한다)
   $MYSQL_PERF -e "
   SELECT @@performance_schema AS ps;
   SELECT NAME, ENABLED FROM performance_schema.setup_consumers
   WHERE NAME IN ('statements_digest','events_statements_current');"

   # 5) digest 테이블이 실제로 채워지는가 (0보다 커야 한다)
   $MYSQL_PERF -e "
   SELECT count(*) FROM performance_schema.events_statements_summary_by_digest
   WHERE SCHEMA_NAME = 'uss_db';"

   # 6) digest 원문이 잘리지 않는가 (기본 1024. 대상 쿼리가 이보다 길면 원문이 잘린다)
   $MYSQL_PERF -e "SELECT @@performance_schema_max_digest_length;"
   ```

4. 4)의 `ps`가 0이거나 소비자가 `NO`면 아래로 조치하도록 안내한다.

   - `@@performance_schema`가 0이면 재기동이 필요하다. `docker/docker-compose-local.yml`의 mysql `command`에
     `--performance-schema=ON`을 추가한 뒤 컨테이너를 재기동한다.
   - 소비자가 꺼져 있으면 재기동 없이 켤 수 있다.

     ```bash
     $MYSQL_PERF -e "
     UPDATE performance_schema.setup_consumers SET ENABLED = 'YES'
     WHERE NAME IN ('statements_digest','events_statements_current');"
     ```

5. 5)가 0이면 아직 아무 쿼리도 안 지나간 것이다. 대상 API를 한 번 호출하게 한 뒤 다시 센다.
   그래도 0이면 4)로 돌아가 소비자 상태를 다시 확인한다.

6. 6)의 값이 대상 쿼리 길이보다 짧으면 그 사실을 호출자에게 알린다.
   digest 원문이 잘리면 Phase 4에서 쿼리를 리포지토리 메서드에 매핑할 수 없다.
   늘리려면 `--performance-schema-max-digest-length=4096`을 mysql `command`에 추가하고 재기동한다.
   **`performance-schema-max-sql-text-length`도 함께 올려야** 원문 전체가 남는다.

7. 확인 결과를 `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 **측정 환경**에 기록한다.
   - 커넥션 풀 크기는 `application-perf.yml`의 `maximum-pool-size`를 Read로 읽어 적는다.
   - **캐시 상태는 warm으로 고정해 적는다.** InnoDB 버퍼 풀은 재기동 없이 비울 수 없고,
     이 프로젝트는 애플리케이션 캐시를 쓰지 않는다. cold 측정을 설계하지 마라.
     대신 Phase 4와 8에서 워밍업을 같은 조건으로 돌려 버퍼 풀 상태를 맞춘다.
   - 버퍼 풀 크기(`SELECT @@innodb_buffer_pool_size;`)를 함께 적는다.
     데이터가 버퍼 풀보다 크면 디스크 I/O가 측정에 섞이므로, 그 사실이 해석의 전제가 된다.

### 출력
- `src/main/resources/application-perf.yml` 존재 확인 또는 생성
- `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 측정 환경에 프로파일, 커넥션 풀 크기, 버퍼 풀 크기, 캐시 상태가 기록
- `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 진행 상태의 Phase 2가 ✅로 기록

### 실패 처리
- 없음

> 다음 Phase 조건: 3번의 여섯 항목이 모두 통과했을 때 → Phase 3

> Skip 조건: 같은 이슈의 다른 대상에서 이미 통과했고 그 사이에 애플리케이션과 컨테이너를 재기동하지 않았으면,
> 앞선 대상의 `record.md` **측정 환경**을 그대로 옮겨 적고 건너뛴다. 진행 상태에는 ⏭️로 표기한다.
