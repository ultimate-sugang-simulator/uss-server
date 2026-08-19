## Phase 8. 재측정과 검증

### 목적
동일 조건으로 재측정해 개선 효과를 수치로 확정하고, 사이클을 계속할지 판정한다.

### 선행 조건
- Phase 7 완료
- 애플리케이션이 변경된 코드로 재기동

### 참조 파일
- `.claude/skills/optimize-performance/template/query-stats-template.md`

### 절차

1. **Phase 4와 완전히 동일한 조건**으로 재측정하도록 아래를 제시한다.
   스크립트, VU, duration, 데이터 규모, 풀 크기를 하나도 바꾸지 마라.

   ```bash
   # 새 터미널이면 먼저:
   #   export PERF_DIR=.claude/resources/perf/{이슈번호}
   #   export TARGET_DIR=$PERF_DIR/{슬러그}
   #   export MYSQL_PWD=root
   #   export MYSQL_PERF="mysql -h 127.0.0.1 -P 3307 -u root uss_db"

   # 1) 토큰 재발급 (digest 리셋 전에 끝낸다)
   seq {STUDENT_ID_START} {STUDENT_ID_END} \
     | while read -r sid; do
         curl -s -X POST localhost:8080/api/v1/auth/login \
           -H 'Content-Type: application/json' \
           -d "{\"studentId\":\"$sid\",\"password\":\"perfPassw0rd\"}" \
         | jq -c 'select(.accessToken != null) | {accessToken, refreshToken}'
       done \
     | jq -s '.' > $PERF_DIR/tokens.json

   jq 'length' $PERF_DIR/tokens.json

   # 2) 워밍업. 이 실행의 결과는 쓰지 않는다.
   k6 run -e PHASE=warmup $TARGET_DIR/test-script.js

   # 3) 되돌리기 - Phase 4와 같은 절차를 같은 자리에서 실행한다
   #    record.md에 적어둔 SQL을 그대로 쓴다

   # 4) 옵티마이저 통계 갱신 - Phase 4와 같아야 한다
   $MYSQL_PERF -e "ANALYZE TABLE members, courses, course_schedules, carts, registrations;"

   # 5) 데이터 규모가 Phase 4의 시작 상태와 같은지 확인
   $MYSQL_PERF -e "
   SELECT 'registrations' AS t, count(*) AS n FROM registrations
   UNION ALL SELECT 'carts', count(*) FROM carts
   UNION ALL SELECT 'enrolled', COALESCE(sum(current_enrollment),0) FROM courses;"

   # 6) 쿼리 통계 리셋
   $MYSQL_PERF -e "
   TRUNCATE TABLE performance_schema.events_statements_summary_by_digest;"

   # 7) 측정 부하 (Phase 3의 스크립트를 그대로 쓴다)
   k6 run -e PHASE=measure \
     -e SUMMARY_OUT=$TARGET_DIR/k6-test-summary-{n}.json \
     $TARGET_DIR/test-script.js

   # 8) 쿼리 통계 수집
   #    수집 단계에서 반올림하지 않는다. 반올림은 대화에서 표로 제시할 때만 한다.
   REQS=$(jq -r '.requests // empty' $TARGET_DIR/k6-test-summary-{n}.json)

   if ! [ "$REQS" -gt 0 ] 2>/dev/null; then
     echo "요청 수가 '$REQS'다. 측정이 실패했으므로 통계를 수집하지 않는다. 원인을 확인하고 재측정하라."
   else
   $MYSQL_PERF -B -e "
   SELECT COUNT_STAR                                                   AS calls,
          COUNT_STAR / $REQS                                           AS per_req,
          AVG_TIMER_WAIT / 1e9                                         AS mean_ms,
          SUM_TIMER_WAIT / 1e9                                         AS total_ms,
          100 * SUM_TIMER_WAIT / SUM(SUM_TIMER_WAIT) OVER ()           AS pct,
          SUM_ROWS_SENT / NULLIF(COUNT_STAR, 0)                        AS rows_per_call,
          SUM_ROWS_EXAMINED / NULLIF(SUM_ROWS_SENT, 0)                 AS examined_per_sent,
          DIGEST_TEXT
   FROM performance_schema.events_statements_summary_by_digest
   WHERE SCHEMA_NAME = 'uss_db'
     AND DIGEST_TEXT NOT LIKE '%performance_schema%'
   ORDER BY SUM_TIMER_WAIT DESC LIMIT 20;" \
   | tee $TARGET_DIR/query-stats-summary-{n}.md
   fi

   # 9) 개선 후 실행계획 (Phase 6과 같은 쿼리, 같은 파라미터 값)
   $MYSQL_PERF -e "EXPLAIN ANALYZE {대상 쿼리}" > /dev/null

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
   } | tee -a $TARGET_DIR/query-plan-{n}.txt
   ```

   - 이 블록도 대상 하나만 잰다. 다른 대상의 스크립트를 이어서 돌리게 하지 마라. (`SKILL.md`의 **대상 진행 규칙**)
   - `{n}`에는 이번 사이클 적용 후의 상태 번호를 넣는다. 앞선 상태의 파일을 덮어쓰지 마라.
   - EXPLAIN에는 Phase 6 **실행계획**에 적어둔 파라미터 값을 그대로 쓴다. 값을 바꾸면 계획이 비교 불가가 된다.
   - `FLUSH STATUS`와 쿼리는 Phase 6과 마찬가지로 같은 세션에서 실행해야 한다.
   - **되돌리기와 `ANALYZE TABLE`을 Phase 4와 같은 자리에서 같은 방식으로 실행한다.** 하나라도 어긋나면
     전후 비교가 아니라 서로 다른 조건의 두 측정을 비교하게 된다.
     특히 되돌리기를 건너뛰면 `uk_member_course` 중복으로 요청이 전부 실패해 측정 자체가 무의미해진다.
   - 쓰기 부하가 포함된 시나리오면 1차 측정이 데이터를 불려놓았을 수 있다. 5)의 결과로 확인한다.
   - 조건이 달라졌으면 그 사실을 기록에 명시하고, 비교 가능한 범위를 좁혀서 해석한다.

2. 실행이 끝나면 아래 파일을 Read로 읽는다. 터미널 출력을 붙여넣게 하지 마라.

   | 산출물 | 개선 후 (이번 사이클) | 개선 전 (비교 대상) |
   |---|---|---|
   | k6 요약 | `k6-test-summary-{n}.json` | `k6-test-summary-{n-1}.json` |
   | 실행계획 | `query-plan-{n}.txt` | `query-plan-{n-1}.txt` |

   쿼리 통계는 여기서 읽지 않는다. `{n}` 1차 출력은 절차 3에서 위임으로 가공하고,
   가공이 끝난 뒤 `{n}`과 `{n-1}` 가공본을 읽는다.
   최초 상태와의 누적 변화가 필요하면 `-0` 파일을 함께 읽는다.

   - `checks_rate`가 Phase 4보다 떨어졌으면 응답 내용이 달라진 것이다. 수치 비교보다 이 사실을 먼저 보고한다.

3. 두 산출물을 가공본으로 다시 쓴다.

   - `query-stats-summary-{n}.md`: Agent 도구로 `query-source-mapper`에 위임한다. 1차 출력을 메인에서 Read하지 마라.
     프롬프트에 넘길 것 (전체 경로로): 1차 출력(`query-stats-summary-{n}.md`), 템플릿(`template/query-stats-template.md`),
     `record.md`, 상태 번호 `n`, k6 요약(`k6-test-summary-{n}.json`), 직전 가공본(`query-stats-summary-{n-1}.md`).
     헤더의 **직전 상태 대비**는 에이전트가 `{n-1}` 가공본과 대조해 적는다.
     반환된 출처 미상 목록과 잘림 여부만 확인하고, 위임이 끝난 뒤 가공본을 Read해 절차 4의 제시에 쓴다.
   - `k6-test-summary-{n}.json`: 최상위에 `delta_vs_prev` 객체를 덧붙인다. 다른 필드는 손대지 마라.

     ```json
     "delta_vs_prev": {
       "from": "k6-test-summary-{n-1}.json",
       "rps": { "before": 37.31, "after": 82.44 },
       "duration_p95_ms": { "before": 1734.5, "after": 612.8 },
       "duration_p99_ms": { "before": 2887.8, "after": 941.2 }
     }
     ```

     `before`와 `after`에는 각 파일에 적힌 값을 **그대로** 옮긴다. 자릿수를 줄이지 마라.
   - `query-plan-{n}.txt`는 원본 그대로 둔다.

4. 전후를 비교해 제시하고 **개선 여부 판정을 호출자에게 묻는다.** `SKILL.md`의 **분석 주도 규칙**을 따른다.
   제시할 때 **두 축을 모두 남긴다.**

   - **하드웨어 의존 증거**: p95, p99, RPS. 로컬 절대값은 신뢰하지 말고 상대 변화만 쓴다.
   - **하드웨어 독립 증거**: 요청당 쿼리 수, `examined_per_sent`, Handler 카운터, 접근 방식과 사용 인덱스 변화.
   - 실행계획을 노드별 표로 제시할 때는 **Phase 6의 칼럼 설명 표를 함께 붙인다.** 표만 던지지 마라.
   - 물을 것: "이 변화가 기법의 효과라고 보십니까, 아니면 측정 편차라고 보십니까?"
   - 개선이 없거나 오히려 나빠졌으면 그대로 제시한다. 수치를 유리하게 해석하지 마라.

5. Phase 5-B에 적힌 **호출자가 예상한 효과**와 실측을 대조해 보고한다.
   - 예상대로면 어떤 근거가 맞았는지 짚는다.
   - 어긋났으면 어느 가정이 틀렸는지 관측값으로 짚는다.
   - 실측 없이 "예상대로 개선되었다"고 쓰지 마라.

6. 종료를 판정한다. **개선 여부는 하드웨어 독립 증거로 판정한다.** p95나 RPS의 변화만으로 개선을 주장하거나 종료를 판정하지 마라.

   | 조건 | 판정 |
   |---|---|
   | 하드웨어 독립 증거에 변화가 없음 | 종료 |
   | Phase 6 위험 신호 표의 항목이 모두 해소됨 | 종료 |
   | 호출자가 종료를 선택 | 종료 |
   | 그 외 | 계속 |

   **판정만 하고, 계속할지는 호출자에게 확인한다.**

### 출력
- `.claude/resources/perf/{이슈번호}/{슬러그}/k6-test-summary-{n}.json` 생성 (`delta_vs_prev` 포함)
- `.claude/resources/perf/{이슈번호}/{슬러그}/query-stats-summary-{n}.md` 생성 (가공본)
- `.claude/resources/perf/{이슈번호}/{슬러그}/query-plan-{n}.txt` 생성 (원본 유지)
- `record.md`의 사이클 {n} **개선 후 지표**와 **판정**이 채워짐
- `record.md`의 진행 상태의 사이클 {n} Phase 8이 ✅으로 기록

### 실패 처리
- 재측정 조건이 1차와 달라졌는데 되돌릴 수 없으면, 비교 가능한 지표만 골라 해석하고 나머지는 "조건 변경으로 비교 불가"로 명시한다.
- 개선 후 에러율이 올랐으면 수치 비교보다 원인을 먼저 보고한다.

> 다음 Phase 조건: 종료 판정이거나 호출자가 종료를 선택한 경우 → Phase 9

> 계속하는 경우 → Phase 5 (`record.md`의 진행 상태에 사이클 행을 추가하고 번호를 +1)

> Skip 조건: 없음 (필수 Phase)
