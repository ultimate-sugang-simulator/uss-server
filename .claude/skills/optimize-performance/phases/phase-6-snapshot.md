## Phase 6. 개선 전 스냅샷

### 목적
개선을 적용하기 전 상태를 증거로 남긴다.

### 선행 조건
- Phase 5 완료

### 참조 파일
- 없음

### 절차

1. 기법과 무관하게 공통으로 캡처한다.
   - k6 요약: p95, p99, RPS, 에러율
   - 요청당 쿼리 수, 대상 쿼리의 `calls` / `mean_ms` / `total_ms` / `examined_per_sent`
   - 부하 조건: VU, duration, 데이터 규모, 커넥션 풀 크기

2. EXPLAIN에 넣을 쿼리를 만든다.

   - digest의 쿼리 원문은 리터럴이 `?`로 정규화되어 있다. 그대로 EXPLAIN하면 실패한다.
   - 파라미터마다 리터럴을 대입한다. 대입값은 **부하 스크립트가 실제로 보내는 값의 범위**에서 고른다.
     - memberId 계열: 시드 회원 id 범위 안의 값
     - courseId, 학과, 영역 계열: Phase 3에서 확정한 카디널리티 분포의 중앙에 있는 값
   - 값을 한쪽 끝(최솟값, 최댓값, 행이 0건인 값)으로 잡지 마라.
     특히 **분포가 치우친 컬럼은 값 하나로 계획이 뒤집힌다.** 흔한 값과 드문 값 중 어느 쪽을 기준으로
     삼을지 호출자와 정하고, 정한 이유를 함께 적는다.
   - 대입한 파라미터 값을 `record.md`의 사이클 {n} **실행계획**에 적는다. 이후 모든 사이클에서 같은 값을 쓴다.

3. 대상 쿼리의 실행계획을 `query-plan-{n-1}.txt`로 캡처한다. (사이클 1이면 `query-plan-0.txt`)

   - **`query-plan-{n-1}.txt`가 이미 있고 대상 쿼리가 같으면 다시 뜨지 마라.** Read로 읽고 4번으로 간다.
   - 없거나 대상 쿼리가 직전 사이클과 다르면 아래를 제시하고 결과를 받는다.
     `tee -a`로 덧붙인다. 같은 상태의 다른 쿼리 계획을 덮어쓰지 마라.

   ```bash
   # 새 터미널이면 먼저:
   #   export PERF_DIR=.claude/resources/perf/{이슈번호}
   #   export TARGET_DIR=$PERF_DIR/{슬러그}
   #   export MYSQL_PWD=root
   #   export MYSQL_PERF="mysql -h 127.0.0.1 -P 3307 -u root uss_db"

   # 버퍼 풀을 채우는 1회. 이 출력은 쓰지 않는다.
   $MYSQL_PERF -e "EXPLAIN ANALYZE {대상 쿼리}" > /dev/null

   # 기록용 2회차 - 실측 계획, 추정 계획, 접근 방식별 실제 작업량을 한 파일에 이어 붙인다
   {
     echo "=== EXPLAIN ANALYZE ==="
     $MYSQL_PERF -e "EXPLAIN ANALYZE {대상 쿼리}\G"

     echo "=== EXPLAIN FORMAT=JSON ==="
     $MYSQL_PERF -e "EXPLAIN FORMAT=JSON {대상 쿼리}\G"

     echo "=== HANDLER COUNTERS ==="
     $MYSQL_PERF -e "
     FLUSH STATUS;
     {대상 쿼리};
     SHOW SESSION STATUS WHERE Variable_name LIKE 'Handler_%' AND Value > 0;" \
     | grep -A100 'Handler_'
   } | tee -a $TARGET_DIR/query-plan-{n-1}.txt
   ```

   - `EXPLAIN ANALYZE`는 대상 쿼리를 **실제로 실행한다.** 읽기 쿼리라 되돌릴 것은 없지만,
     대상이 쓰기 쿼리면 상황이 다르다. 아래를 따른다.
     - MySQL 8.0의 `EXPLAIN ANALYZE`는 `SELECT`만 받는다. 쓰기 쿼리면 `EXPLAIN FORMAT=JSON`(실행하지 않음)만 쓴다
     - 쓰기 쿼리의 실제 작업량이 필요하면 `BEGIN; {쿼리}; ROLLBACK;` 안에서 Handler 카운터를 잰다.
       AUTO_INCREMENT 증가는 롤백되지 않는다는 사실을 호출자에게 알린다
   - **`FLUSH STATUS`와 쿼리를 같은 세션에서 실행해야 한다.** 명령을 나눠 실행하면 세션이 갈려 카운터가 0으로 나온다.
     위 블록처럼 하나의 `-e` 안에 세미콜론으로 이어 붙인다.

   파일을 Read로 읽는다. 터미널 출력을 붙여넣게 하지 마라.
   **이 파일은 원본 그대로 둔다.** 가공본으로 덮어쓰지 마라.

4. 계획에서 아래 수치를 뽑아 노드별 표로 정리해 대화에서 제시한다. 파일에는 쓰지 않는다.

   | 노드 | 접근 방식 / 인덱스 | actual time | 추정 rows | 실측 rows | loops | 비고 |
   |---|---|---|---|---|---|---|

   그리고 Handler 카운터를 별도 표로 함께 낸다.

   | 카운터 | 값 | 뜻 |
   |---|---|---|

   **두 표 바로 아래에 칼럼 설명을 함께 붙인다.** 표만 던지지 마라. 아래 내용을 그 계획의 실제 수치로 예를 들어 적는다.

   | 칼럼 | 설명 |
   |---|---|
   | actual time=A..B | A는 첫 행까지, B는 마지막 행까지 걸린 시간(ms). **loops당 평균**이고 자식 노드의 시간을 포함한다 |
   | 추정 rows (`cost=... rows=N`) | 옵티마이저가 실행 전에 인덱스 통계를 보고 계산한 예상 행 수. 옵티마이저는 이 값만 보고 접근 방식과 조인 순서를 고른다 |
   | 실측 rows | 그 노드가 실제로 내보낸 행 수. **loops당 평균값**이므로 총량을 보려면 loops를 곱한다. 추정과 10배 이상 벌어지면 통계가 낡았거나 계획 자체가 잘못 골라졌다 |
   | loops | 그 노드가 실행된 횟수. Nested Loop 안쪽이면 바깥 행 수만큼 커진다 |

   | Handler 카운터 | 뜻 |
   |---|---|
   | `Handler_read_rnd_next` | 테이블을 순차로 다음 행 읽기. **풀스캔의 직접 증거**다. 반환 행 수보다 훨씬 크면 읽고 버린 행이 그만큼이다 |
   | `Handler_read_key` | 인덱스로 행을 찾은 횟수. 이 값이 크고 `rnd_next`가 작으면 인덱스가 일하고 있다 |
   | `Handler_read_next` | 인덱스 순서로 다음 행 읽기. 범위 스캔의 폭이다 |
   | `Handler_read_rnd` | 인덱스로 찾은 위치에서 행을 다시 읽은 횟수. 커버링이 안 될 때 늘어난다 |
   | `Sort_scan`, `Sort_rows` | `filesort` 발생 여부와 정렬한 행 수. 인덱스 순서로 정렬이 해결되면 0이 된다 |

   `EXPLAIN ANALYZE`에는 PostgreSQL의 `BUFFERS`에 해당하는 항목이 없다.
   **읽은 페이지 대신 읽은 행으로 본다.** 그 근거가 Handler 카운터다. 없는 지표를 있는 것처럼 적지 마라.

   그다음 **어느 노드가 비용을 먹고 있는지 호출자에게 묻는다.** `SKILL.md`의 **분석 주도 규칙**을 따른다.
   - 표는 사실만 옮긴다. 특정 행을 강조하거나 순서를 바꿔 답을 유도하지 마라.
   - 호출자의 답에 대해 타당성을 확인하고, 어긋나면 표의 수치로 반례를 든다.
   - 호출자가 막히면 그때 6번의 위험 신호 표를 꺼내 함께 본다.
   - 확정된 해석을 `record.md`의 사이클 {n} **실행계획**에 적는다.

5. 기법별로 추가 캡처한다.
   - **캐싱**: 동일 입력의 반복 호출 비율, 무효화가 필요한 쓰기 경로 목록
   - **로직 개선**: 변경 전 요청당 쿼리 수와 그 호출 스택
   - **풀, 트랜잭션**: 커넥션 획득 대기 시간, 트랜잭션 유지 구간
   - **락, 원자적 갱신**: 락 대기 현황

     ```bash
     $MYSQL_PERF -e "
     SELECT * FROM performance_schema.data_lock_waits\G
     SHOW ENGINE INNODB STATUS\G" | grep -A20 'LATEST DETECTED DEADLOCK\|TRANSACTIONS'
     ```

6. 판정은 절대 시간이 아니라 비율로 한다. 이 표는 호출자와 함께 본다.

   | 지표 | 위험 신호 |
   |---|---|
   | `examined_per_sent` (읽은 행 / 반환 행) | 100:1 초과 |
   | 옵티마이저 추정 대 실측 행 수 | 10배 이상 괴리 |
   | 단일 쿼리의 `total_ms` 점유율 | 30% 이상 |
   | OLTP 경로의 풀스캔(`Handler_read_rnd_next` 급증) | 1만 행 이상 테이블이면 신호 |
   | 동일 쿼리의 요청당 호출 횟수 | 1회 초과면 N+1 의심 |
   | `Sort_rows`가 반환 행 수보다 큼 | 정렬을 인덱스로 못 풀고 있음 |

### 출력
- `.claude/resources/perf/{이슈번호}/{슬러그}/query-plan-{n-1}.txt` 생성 (원본 유지)
- `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 사이클 {n} **개선 전 지표**와 **실행계획**이 채워짐
  (EXPLAIN에 대입한 파라미터 값 포함)
- `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 진행 상태의 사이클 {n} Phase 6이 ✅으로 기록

### 실패 처리
- 없음

> 다음 Phase 조건: 개선 전 지표와 실행계획이 기록되었을 때 → Phase 7

> Skip 조건: 없음 (필수 Phase)
