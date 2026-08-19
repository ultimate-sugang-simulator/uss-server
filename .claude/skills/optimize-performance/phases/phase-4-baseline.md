## Phase 4. 기준선 측정

### 목적
기준선을 측정하고, 그 결과를 소비 가능한 형태로 가공해 호출자에게 제시한다.
병목이 어디인지는 호출자가 판정한다.

### 선행 조건
- Phase 3 완료
- `.claude/resources/perf/{이슈번호}/{슬러그}/test-script.js` 존재

### 참조 파일
- `.claude/skills/optimize-performance/template/query-stats-template.md`

### 절차

1. 아래 순서로 실행하도록 제시한다. **측정 실행은 호출자가 한다.** 순서를 바꾸지 마라.

   이 블록은 대상 하나만 잰다. 다른 대상의 스크립트를 이어서 돌리게 하지 마라.
   digest 통계는 인스턴스 전역이라 리셋 없이 다음 대상을 재면 통계가 섞이고,
   `per_req`의 분모(`requests`)가 이 대상의 것이므로 요청당 쿼리 수가 틀린 값이 된다.
   (`SKILL.md`의 **대상 진행 규칙**)

   ```bash
   # 새 터미널이면 먼저:
   #   export PERF_DIR=.claude/resources/perf/{이슈번호}
   #   export TARGET_DIR=$PERF_DIR/{슬러그}
   #   export MYSQL_PWD=root
   #   export MYSQL_PERF="mysql -h 127.0.0.1 -P 3307 -u root uss_db"

   # 1) 토큰 발급 (digest 리셋 전에 끝낸다. 이슈 공용이므로 이미 있으면 건너뛴다)
   #    로그인 쿼리가 측정 통계에 섞이지 않도록 반드시 6)보다 먼저 한다.
   seq {STUDENT_ID_START} {STUDENT_ID_END} \
     | while read -r sid; do
         curl -s -X POST localhost:8080/api/v1/auth/login \
           -H 'Content-Type: application/json' \
           -d "{\"studentId\":\"$sid\",\"password\":\"perfPassw0rd\"}" \
         | jq -c 'select(.accessToken != null) | {accessToken, refreshToken}'
       done \
     | jq -s '.' > $PERF_DIR/tokens.json

   jq 'length' $PERF_DIR/tokens.json   # 시드 회원 수와 같아야 한다

   # 2) 워밍업 (JIT, 커넥션 풀, InnoDB 버퍼 풀). 이 실행의 결과는 쓰지 않는다.
   k6 run -e PHASE=warmup $TARGET_DIR/test-script.js

   # 3) 쓰기 엔드포인트면 워밍업이 남긴 행을 되돌린다
   #    Phase 3-B에서 확정해 record.md에 적어둔 SQL을 그대로 실행한다

   # 4) 옵티마이저 통계 갱신. 3)의 DELETE 이후 분포가 달라졌을 수 있다
   $MYSQL_PERF -e "ANALYZE TABLE members, courses, course_schedules, carts, registrations;"

   # 5) 데이터 규모가 시작 상태와 같은지 확인 (되돌리기가 제대로 됐는지)
   $MYSQL_PERF -e "
   SELECT 'registrations' AS t, count(*) AS n FROM registrations
   UNION ALL SELECT 'carts', count(*) FROM carts
   UNION ALL SELECT 'enrolled', COALESCE(sum(current_enrollment),0) FROM courses;"

   # 6) 쿼리 통계 리셋
   $MYSQL_PERF -e "
   TRUNCATE TABLE performance_schema.events_statements_summary_by_digest;"

   # 7) 측정 부하
   k6 run -e PHASE=measure \
     -e SUMMARY_OUT=$TARGET_DIR/k6-test-summary-0.json \
     $TARGET_DIR/test-script.js

   # 8) 쿼리 통계 수집 (요청 수를 분모로 넘겨 요청당 호출 수까지 뽑는다)
   REQS=$(jq -r '.requests // empty' $TARGET_DIR/k6-test-summary-0.json)

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
   | tee $TARGET_DIR/query-stats-summary-0.md
   fi
   ```

   - **`ANALYZE TABLE`을 빼지 마라.** 되돌리기 `DELETE` 이후 인덱스 통계가 실제 분포와 어긋난 채로 남으면
     옵티마이저가 다른 계획을 고를 수 있고, Phase 8의 전후 비교가 계획 차이로 오염된다.
   - `-B`를 빼지 마라. 기본 박스 출력은 `DIGEST_TEXT`를 잘라 리포지토리 메서드를 식별할 수 없게 만든다.
     `-B`는 탭 구분으로 한 줄에 내보낸다.
   - **`SUM_TIMER_WAIT`는 피코초다.** `/1e9`를 빼면 단위가 ms가 아니게 된다.
   - **수집 단계에서 반올림하지 마라.** `per_req`를 소수 둘째 자리로 자르면 0.005 미만인 쿼리가 `0.00`으로 사라진다.
     반올림은 대화에서 표로 제시할 때만 한다. 파일에는 MySQL이 뽑아준 값을 그대로 둔다.
   - `REQS` 가드를 빼지 마라. 요청 0건이면 `per_req`의 분모가 0이 되어 수집이 무의미해진다.
   - `examined_per_sent`는 **읽은 행 대 돌려준 행의 비율**이다. PostgreSQL의 `Rows Removed by Filter`에 해당하는
     하드웨어 독립 지표이고, 인덱스 필요성을 가장 직접적으로 보여준다. 이 칼럼을 빼지 마라.

2. 실행이 끝나면 k6 요약(`.claude/resources/perf/{이슈번호}/{슬러그}/k6-test-summary-0.json`)을 Read로 읽는다.
   쿼리 통계 1차 출력(`query-stats-summary-0.md`)은 메인에서 Read하지 않는다. 절차 3의 위임이 끝난 뒤 가공본을 읽는다.

   - 터미널 출력을 붙여넣게 하지 마라. 파일이 없으면 원인을 확인하고 재실행을 요청한다. 추정으로 채우지 마라.
   - k6 요약에는 스크립트가 선별해 내보낸 값만 있다. 담기지 않은 지표가 필요해지면 재측정해야 한다.
   - `checks_rate`가 1이 아니면 `checks[]`에서 어떤 항목이 깨졌는지 먼저 확인한다.
     데이터 검증 check가 깨진 측정은 진단에 쓰지 마라.

3. 가공본 작성을 Agent 도구로 `query-source-mapper`에 위임한다.
   1차 출력을 메인에서 Read하거나 직접 가공하지 마라. Grep 탐색 흔적이 메인 컨텍스트에 남지 않게 하는 위임이다.

   프롬프트에 넘길 것 (전체 경로로):
   - 1차 출력: `.claude/resources/perf/{이슈번호}/{슬러그}/query-stats-summary-0.md`
   - 템플릿: `.claude/skills/optimize-performance/template/query-stats-template.md`
   - `record.md` (예상 쿼리 목록이 출처 매핑의 1차 후보)
   - 상태 번호 `n=0`
   - k6 요약: `.claude/resources/perf/{이슈번호}/{슬러그}/k6-test-summary-0.json`

   반환된 출처 미상 목록과 `DIGEST_TEXT` 잘림 여부를 확인한다. 미상이 남는 것은 정상이다. 채우라고 재호출하지 마라.

4. 가공본(`query-stats-summary-0.md`)을 Read해 k6 요약과 함께 호출자에게 제시하고 **병목 판정을 묻는다.**
   `SKILL.md`의 **분석 주도 규칙**을 따른다.

   - 제시할 것: 응답시간 분포, 처리량, check 결과, 쿼리별 요청당 호출 수, 총 시간 비중, `examined_per_sent`.
   - 물을 것: "요청당 쿼리 수와 시간이 쏠린 지점을 보고, 병목의 성격을 어떻게 판단하십니까?"
   - 결론을 먼저 말하지 마라. 아래 표는 호출자가 막혔을 때 꺼내는 재료다.

   | 관측 | 진단 | 유력한 기법 |
   |---|---|---|
   | 특정 쿼리 1건이 느리고 호출 수는 예상대로 | 쿼리 자체 비효율 | 인덱스, 쿼리 재작성 |
   | `examined_per_sent`가 크다 | 읽고 버리는 행이 많음 | 인덱스, WHERE 조건 선택도 개선 |
   | 쿼리는 빠른데 호출 수가 요청당 N배 | N+1 | fetch join, DTO projection, `@BatchSize` |
   | 쿼리 효율적이고 호출도 적은데 API가 느림 | DB 밖 문제 | 직렬화, 응답 크기, 컬렉션 가공 |
   | 매 요청이 같은 결과를 다시 계산 | 불필요한 재조회 | 캐싱 |
   | 단건은 빠른데 VU를 올리면 급락 | 자원 경합 | 커넥션 풀, 트랜잭션 범위 축소, 락 경합 |
   | 쓰기 엔드포인트에서 VU에 비례해 대기가 늘어남 | 같은 행에 쓰기가 몰림 | 락 범위 축소, 원자적 UPDATE |

5. 호출자의 판정에 대해 타당성을 확인한다.
   - Phase 1의 예상 쿼리 목록과 실제 `per_req`가 어긋난 지점이 있으면 반드시 짚는다.
   - 시간 비중이 낮은 쿼리를 병목으로 지목했으면 `pct` 수치로 반례를 든다.

6. 확정된 판정과 근거를 `.claude/resources/perf/{이슈번호}/{슬러그}/record.md`의 **기준선**에 남긴다.
   근거에는 관측된 수치를 쓴다. 판정의 주체가 호출자였다는 사실은 따로 적지 않는다.

### 출력
- `.claude/resources/perf/{이슈번호}/tokens.json` 생성
- `.claude/resources/perf/{이슈번호}/{슬러그}/k6-test-summary-0.json` 생성
- `.claude/resources/perf/{이슈번호}/{슬러그}/query-stats-summary-0.md` 생성 (가공본)
- `record.md`의 **기준선** 표와 쿼리 통계, 진단이 채워짐
- `record.md`의 진행 상태의 Phase 4가 ✅로 기록

### 실패 처리
- 에러율이 높거나 데이터 검증 check가 깨져 측정이 무의미하면, 원인을 짚어 스크립트나 시드를 수정한 후 재측정하도록 안내한다. 실패한 측정치로 진단하지 않는다.
- 토큰 수가 시드 회원 수와 다르면 로그인에 실패한 학번이 있는 것이다. 비밀번호 해시(`@pw_hash`)가
  Phase 3-A에서 뽑은 값과 같은지 먼저 확인한다.

> 다음 Phase 조건: 병목의 성격이 판정되었고 근거 수치가 기록되었을 때 → Phase 5

> Skip 조건: 없음 (필수 Phase)
