# 측정 명령 블록

Phase 4, 6, 8이 호출자에게 제시하는 명령이다. `{n}`과 `{…}` 자리를 채워 **블록 그대로** 제시한다.
전제: `perf-env.sh`를 source한 터미널 (`$PERF_DIR`, `$TARGET_DIR`, `$PERF_JWT_SECRET`, `mysqlp`).

## A. 부하 측정

Phase 4는 `{n}` = 0, Phase 8은 `{n}` = 이번 사이클 번호.

```bash
# 1) 토큰. 이슈 공용이라 이미 있고 만료 전이면 건너뛴다. 로그인 API를 태우지 않는다
bash .claude/skills/_shared/mint-tokens.sh \
  --secret "$PERF_JWT_SECRET" --start {회원 id 시작값} --count {USER_COUNT} \
  --out $PERF_DIR/tokens.json
python3 -c "import json;print(len(json.load(open('$PERF_DIR/tokens.json'))))"   # USER_COUNT와 같아야 한다

# 2) 워밍업 (JIT, 커넥션 풀, InnoDB 버퍼 풀). 이 실행의 결과는 쓰지 않는다
k6 run -e PHASE=warmup $TARGET_DIR/test-script.js

# 3) 되돌리기. 쓰기 엔드포인트면 record.md의 되돌리기 SQL을 여기서 실행한다. 읽기면 없음

# 4) 옵티마이저 통계 갱신, 시작 상태 확인
mysqlp -e "
ANALYZE TABLE members, courses, course_schedules, carts, registrations;
SELECT 'registrations' AS t, count(*) AS n FROM registrations
UNION ALL SELECT 'carts', count(*) FROM carts
UNION ALL SELECT 'enrolled', COALESCE(sum(current_enrollment), 0) FROM courses;"

# 5) 쿼리 통계 리셋
mysqlp -e "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest;"

# 6) 측정
k6 run -e PHASE=measure -e SUMMARY_OUT=$TARGET_DIR/k6-test-summary-{n}.json $TARGET_DIR/test-script.js

# 7) 쿼리 통계 수집. 요청 수를 분모로 넘겨 요청당 호출 수까지 뽑는다
REQS=$(jq -r '.requests // empty' $TARGET_DIR/k6-test-summary-{n}.json)
if ! [ "$REQS" -gt 0 ] 2>/dev/null; then
  echo "요청 수가 '$REQS'다. 측정이 실패했으므로 통계를 수집하지 않는다. 원인을 확인하고 재측정하라."
else
mysqlp -B -e "
SELECT COUNT_STAR                                          AS calls,
       COUNT_STAR / $REQS                                  AS per_req,
       AVG_TIMER_WAIT / 1e9                                AS mean_ms,
       SUM_TIMER_WAIT / 1e9                                AS total_ms,
       100 * SUM_TIMER_WAIT / SUM(SUM_TIMER_WAIT) OVER ()  AS pct,
       SUM_ROWS_SENT / NULLIF(COUNT_STAR, 0)               AS rows_per_call,
       SUM_ROWS_EXAMINED / NULLIF(SUM_ROWS_SENT, 0)        AS examined_per_sent,
       DIGEST_TEXT
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = 'uss_db'
  AND DIGEST_TEXT NOT LIKE '%performance_schema%'
  AND DIGEST_TEXT NOT LIKE 'SET NAMES%'
ORDER BY SUM_TIMER_WAIT DESC LIMIT 20;" \
| tee $TARGET_DIR/query-stats-summary-{n}.md
fi
```

블록의 순서와 각 요소는 아래 이유로 고정이다. 바꾸거나 빼지 마라.

| 요소 | 이유 |
|---|---|
| 워밍업 → 되돌리기 → `ANALYZE` 순서 | 되돌리기 `DELETE` 뒤에 통계를 갱신해야 옵티마이저가 같은 계획을 고른다. Phase 4와 8의 계획이 달라지면 전후 비교가 아니다 |
| 리셋 뒤에 곧바로 측정 | digest는 인스턴스 전역이다. 사이에 다른 부하가 끼면 통계가 섞이고 `per_req`가 틀린다 |
| `-B` | 기본 박스 출력은 `DIGEST_TEXT`를 잘라 출처를 매핑할 수 없게 한다 |
| `/1e9` | `TIMER_WAIT`는 피코초다 |
| 반올림 없음 | `per_req`를 둘째 자리에서 자르면 0.005 미만 쿼리가 `0.00`으로 사라진다. 반올림은 대화의 표에서만 한다 |
| `REQS` 가드 | 요청 0건이면 `per_req` 분모가 0이다 |
| `examined_per_sent` | 읽은 행 대 돌려준 행. 하드웨어 독립 지표이고 인덱스 필요성을 가장 직접 보여준다 |
| `SET NAMES` 제외 | `mysqlp`의 init-command가 남기는 행이다. 측정 대상이 아니다 |

## B. 실행계획 캡처

Phase 6은 `{n}` = 사이클 번호 - 1, Phase 8은 `{n}` = 사이클 번호.
`{대상 쿼리}`는 digest의 `?`에 record.md **실행계획**에 적어둔 파라미터 값을 대입한 원문이다.

```bash
# 버퍼 풀을 채우는 1회. 이 출력은 쓰지 않는다
mysqlp -e "EXPLAIN ANALYZE {대상 쿼리}" > /dev/null

{
  echo "=== EXPLAIN ANALYZE ==="
  mysqlp -e "EXPLAIN ANALYZE {대상 쿼리}\G"

  echo "=== EXPLAIN FORMAT=JSON ==="
  mysqlp -e "EXPLAIN FORMAT=JSON {대상 쿼리}\G"

  echo "=== STATUS COUNTERS ==="
  mysqlp -e "
  FLUSH STATUS;
  {대상 쿼리};
  SHOW SESSION STATUS
  WHERE (Variable_name LIKE 'Handler_%' OR Variable_name LIKE 'Sort_%') AND Value > 0;" \
  | grep -E '^(Handler_|Sort_)'
} | tee -a $TARGET_DIR/query-plan-{n}.txt
```

| 요소 | 이유 |
|---|---|
| `FLUSH STATUS`와 쿼리를 한 `-e` 안에 | 세션 카운터다. 명령을 나누면 세션이 갈려 0이 나온다 |
| `Sort_%` 포함 | `Sort_rows`, `Sort_scan`이 filesort 판정 근거다. `Handler_%`만 뽑으면 기록 항목이 빈다 |
| `tee -a` | 같은 상태의 다른 쿼리 계획을 덮어쓰지 않는다 |
| `EXPLAIN ANALYZE`는 SELECT만 | MySQL 8.0은 쓰기 문을 받지 않는다. 쓰기 쿼리면 `EXPLAIN FORMAT=JSON`만 뜨고, 작업량은 `BEGIN; {쿼리}; ROLLBACK;` 안에서 카운터를 잰다. AUTO_INCREMENT 증가는 롤백되지 않는다 |
