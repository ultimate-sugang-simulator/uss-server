## Phase 4. 기준선 측정

### 목적
기준선을 측정하고 결과를 소비 가능한 형태로 가공해 제시한다. 병목이 어디인지는 호출자가 판정한다.

### 선행 조건
- Phase 3 완료, `test-script.js` 존재

### 참조 파일
- `.claude/skills/optimize-performance/template/commands.md`
- `.claude/skills/optimize-performance/template/query-stats-template.md`

### 절차

1. `commands.md`의 **A. 부하 측정** 블록을 `{n}` = 0으로 채워 제시한다. 실행은 호출자가 한다.
   이 블록은 대상 하나만 잰다. 다른 대상의 스크립트를 이어서 돌리게 하지 마라 (`SKILL.md`의 **대상 진행 규칙**).

2. 끝나면 `k6-test-summary-0.json`을 Read한다. 쿼리 통계 1차 출력은 메인에서 Read하지 않는다.
   - 파일이 없으면 원인을 확인하고 재실행을 요청한다. 추정으로 채우지 마라.
   - `checks_rate`가 1이 아니면 `checks[]`에서 어떤 항목이 깨졌는지 먼저 본다. 데이터 검증 check가 깨진 측정은 진단에 쓰지 않는다.

3. 가공본 작성을 `query-source-mapper`에 위임한다. Grep 흔적이 메인 컨텍스트에 남지 않게 하는 위임이다.
   프롬프트에 넘길 것(전체 경로): 1차 출력 `query-stats-summary-0.md`, 템플릿 `query-stats-template.md`, `record.md`, 상태 번호 `n=0`, `k6-test-summary-0.json`.
   반환된 출처 미상 목록과 잘림 여부만 확인한다. 미상이 남는 것은 정상이다. 채우라고 재호출하지 마라.

4. 가공본을 Read해 k6 요약과 함께 제시하고 **병목 판정을 묻는다** (`SKILL.md`의 **역할 경계**).
   - 제시할 것: 응답시간 분포, 처리량, check 결과, 쿼리별 요청당 호출 수, 총 시간 비중, `examined_per_sent`.
   - 물을 것: "요청당 쿼리 수와 시간이 쏠린 지점을 보고, 병목의 성격을 어떻게 판단하십니까?"
   - 아래 표는 호출자가 막혔을 때 꺼내는 재료다. 먼저 보여주지 마라.

   | 관측 | 진단 | 유력한 기법 |
   |---|---|---|
   | 특정 쿼리 1건이 느리고 호출 수는 예상대로 | 쿼리 자체 비효율 | 인덱스, 쿼리 재작성 |
   | `examined_per_sent`가 크다 | 읽고 버리는 행이 많음 | 인덱스, WHERE 선택도 개선 |
   | 쿼리는 빠른데 호출 수가 요청당 N배 | N+1 | fetch join, DTO projection, `@BatchSize` |
   | 쿼리 효율적이고 호출도 적은데 API가 느림 | DB 밖 문제 | 직렬화, 응답 크기, 컬렉션 가공 |
   | 매 요청이 같은 결과를 다시 계산 | 불필요한 재조회 | 캐싱 |
   | 단건은 빠른데 VU를 올리면 급락 | 자원 경합 | 커넥션 풀, 트랜잭션 범위 축소, 락 경합 |
   | 쓰기에서 VU에 비례해 대기가 늘어남 | 같은 행에 쓰기가 몰림 | 락 범위 축소, 원자적 UPDATE |

5. 판정의 타당성을 확인한다.
   - Phase 1의 예상 쿼리 목록과 실제 `per_req`가 어긋난 지점은 반드시 짚는다.
   - 시간 비중이 낮은 쿼리를 병목으로 지목했으면 `pct`로 반례를 든다.

6. 확정된 판정과 근거 수치를 `record.md`의 **기준선**에 적는다. 판정의 주체가 호출자였다는 사실은 적지 않는다.

### 출력
- `tokens.json`, `k6-test-summary-0.json`, `query-stats-summary-0.md` (가공본)
- `record.md`의 **기준선**과 진단, 진행 상태 Phase 4 ✅

### 실패 처리
- 에러율이 높거나 데이터 검증 check가 깨졌으면 원인을 짚어 스크립트나 시드를 고치고 재측정하게 한다. 실패한 측정치로 진단하지 않는다.
- 토큰 수가 `USER_COUNT`와 다르면 `mint-tokens.sh`의 `--count`가 어긋난 것이다. 첫 토큰으로 401이 나면 `--secret`이 `application-perf.yml`과 다른 것이다.

> 다음 Phase 조건: 병목의 성격이 판정되고 근거 수치가 기록되었을 때 → Phase 5
>
> Skip 조건: 없음
