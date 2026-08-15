## Phase 2. 측정 환경 점검

### 목적
경합을 왜곡 없이 관측할 수 있는 상태인지 확인한다.
**게이트.** 아래 다섯 항목 중 하나라도 통과하지 못하면 Phase 3으로 넘어가지 마라.
환경이 어긋난 채로 잰 값은 경합이 아니라 환경을 잰 값이다.

### 선행 조건
- Phase 1 완료
- Docker로 MySQL이 떠 있다

### 참조 파일
- `.claude/skills/fix-concurrency/template/application-conc.yml`

### 절차

1. **conc 프로파일을 준비한다.**
   `src/main/resources/application-conc.yml`이 없으면 `template/application-conc.yml`을 Read해 그 내용으로 생성한다.
   이미 있으면 만들지 말고 현재 값을 읽어 아래 점검에 쓴다.

   - 이 파일은 `.gitignore` 대상이 아니다. 측정 조건이 기록으로 남아야 후보 간 비교가 성립한다.
   - **`application-perf.yml`을 복사해 쓰지 마라.** 커넥션 풀 크기 규칙이 정반대이고,
     `oracle` 블록이 없으면 이 프로젝트는 기동 자체가 되지 않는다(4번 참조).

2. **MySQL과 애플리케이션을 띄우도록 제시한다.** 실행은 호출자가 한다.

   ```bash
   docker-compose -f docker/docker-compose-local.yml up -d mysql
   ./gradlew bootRun --args='--spring.profiles.active=conc'
   ```

3. **기동 확인.** 컨텍스트가 올라왔는지부터 본다.

   ```bash
   curl -s localhost:8081/actuator/health
   ```

   - `{"status":"UP"}`이 아니면 기동 로그를 받아 원인을 짚는다. 다음 항목으로 넘어가지 마라.

4. **인증 경로 확인.** 이 프로젝트의 로그인은 학교 포털 Oracle 함수(`F_LOGIN_CHECK`)를 호출하므로
   **로컬에서는 로그인이 되지 않는다.** 측정용 토큰은 Phase 3에서 서명키로 직접 만든다.

   기동에는 Oracle 설정이 필요하다는 점만 여기서 확인한다.

   - `InuMemberRepository`가 `@ConditionalOnProperty(oracle.enabled=true)`이고
     `AuthService`가 이를 생성자 주입으로 요구한다. `oracle.enabled`가 없거나 `false`면
     빈이 없어 컨텍스트가 뜨지 않는다.
   - `template/application-conc.yml`은 이 때문에 도달 불가능한 더미 URL로 `oracle.enabled: true`를 둔다.
     커넥션 풀은 지연 초기화라 실제 접속 없이도 기동된다. 3번이 통과했다면 확인된 것이다.

5. **커넥션 풀 크기를 확인한다.** 이 항목이 동시성 측정에서 가장 자주 틀어진다.

   ```bash
   curl -s localhost:8081/actuator/health | python3 -m json.tool
   grep -A6 'hikari' src/main/resources/application-conc.yml
   ```

   - **풀 크기가 부하 VU보다 작으면 락 경합이 아니라 커넥션 대기를 재게 된다.**
     Phase 3에서 정할 VU 이상으로 잡는다. VU를 아직 안 정했으면 Phase 3에서 정한 뒤 이 항목으로 돌아온다.
   - 값을 바꾸면 `record.md`의 **측정 환경**에 변경 시점과 함께 남긴다.

6. **격리 수준을 확인해 기록한다.** 낙관적 락의 재시도 빈도와 `FOR UPDATE`의 갭 락 범위가 여기에 달려 있다.

   ```bash
   $MYSQL_CONC -e "
   SELECT @@global.transaction_isolation  AS global_iso,
          @@session.transaction_isolation AS session_iso,
          @@innodb_lock_wait_timeout      AS lock_wait_timeout_sec,
          @@autocommit                    AS autocommit;"
   ```

   - 기본값은 `REPEATABLE-READ`, 락 대기 타임아웃은 50초다.
   - **락 대기 타임아웃이 부하 지속시간보다 길면** 비관적 락 후보에서 요청이 타임아웃 없이 끝까지 밀린다.
     그 상태로도 측정은 되지만, 실패가 관측되지 않는 이유가 되므로 값을 기록해둔다.

7. **락 관측 수단을 점검한다.** 여기서 값이 안 나오면 Phase 4, 7에서 2급 지표를 채울 수 없다.

   ```bash
   # 행 락 누적 카운터 (항상 사용 가능)
   $MYSQL_CONC -e "SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';"

   # 데드락, 락 타임아웃 누적 카운터
   $MYSQL_CONC -e "
   SELECT NAME, COUNT, STATUS
   FROM information_schema.INNODB_METRICS
   WHERE NAME IN ('lock_deadlocks','lock_timeouts','lock_row_lock_waits');"

   # 실시간 락 대기 관측 테이블 (부하 중에만 행이 보인다)
   $MYSQL_CONC -e "
   SELECT COUNT(*) AS data_locks_readable FROM performance_schema.data_locks;"
   ```

   - `INNODB_METRICS`의 `STATUS`가 `disabled`인 지표가 있으면 켜도록 제시한다.

     ```bash
     $MYSQL_CONC -e "SET GLOBAL innodb_monitor_enable='lock_deadlocks';"
     ```

   - `data_locks` 조회가 권한 오류로 실패하면 그 사실을 기록하고, 실시간 락 관측 없이 진행한다.
     누적 카운터만으로도 후보 비교는 성립한다.

8. 확인한 값을 `record.md`의 **측정 환경**에 채운다.

### 출력
- `src/main/resources/application-conc.yml` 준비
- `record.md`의 **측정 환경**에 풀 크기, 격리 수준, 락 대기 타임아웃, 관측 가능한 락 지표가 기록
- `record.md`의 진행 상태의 Phase 2가 ✅로 기록

### 실패 처리

| 증상 | 처리 |
|---|---|
| 컨텍스트 기동 실패 | 기동 로그를 받아 원인을 짚는다. `oracle` 블록 누락이 가장 흔하다 |
| `INNODB_METRICS`가 전부 disabled | `innodb_monitor_enable`로 켜고 재확인. 안 되면 `Innodb_row_lock%`만으로 진행 |
| `data_locks` 접근 불가 | 실시간 락 관측을 빼고 진행. `record.md`에 명시 |
| 풀 크기를 VU 이상으로 못 올림 | 그 상한을 VU 상한으로 삼는다. 풀보다 큰 VU로 재지 마라 |

> 다음 Phase 조건: 다섯 항목(기동, 인증 경로, 풀 크기, 격리 수준, 락 관측)이 확인되어 기록되었을 때 → Phase 3

> Skip 조건: 없음 (필수 Phase)
