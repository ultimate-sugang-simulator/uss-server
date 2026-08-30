## Phase 8. 재측정과 검증

### 목적
동일 조건으로 재측정해 개선 효과를 수치로 확정하고, 사이클을 계속할지 판정한다.

### 선행 조건
- Phase 7 완료 (변경된 코드로 재기동 확인됨)

### 참조 파일
- `.claude/skills/optimize-performance/template/commands.md`
- `.claude/skills/optimize-performance/template/query-stats-template.md`

### 절차

1. `commands.md`의 **A. 부하 측정**과 **B. 실행계획 캡처** 블록을 `{n}` = 이번 사이클 번호로 채워 순서대로 제시한다.
   - **Phase 4와 완전히 같은 조건이다.** 스크립트, VU, duration, 데이터 규모, 풀 크기, 되돌리기, `ANALYZE` 자리를 하나도 바꾸지 마라.
     하나라도 어긋나면 전후 비교가 아니라 서로 다른 조건의 두 측정이 된다.
   - EXPLAIN에는 Phase 6 **실행계획**에 적어둔 파라미터 값을 그대로 쓴다.
   - 앞선 상태의 파일을 덮어쓰지 마라.
   - 조건이 달라졌으면 기록에 명시하고 비교 가능한 범위를 좁혀 해석한다.

2. 끝나면 `k6-test-summary-{n}.json`, `jvm-metrics-{n}.md`, `query-plan-{n}.txt`, 1차 출력 `query-stats-summary-{n}.md`를 개선 전 파일(`-{n-1}`)과 함께 Read한다.
   최초 상태와의 누적 변화가 필요하면 `-0`도 읽는다.
   - `checks_rate`가 떨어졌으면 응답 내용이 달라진 것이다. 수치 비교보다 이 사실을 먼저 보고한다.

3. 가공본을 만든다.
   - `query-stats-summary-{n}.md`: Phase 4의 3과 같은 방법으로 메인이 쓴다. `n >= 1`이므로 직전 가공본 `query-stats-summary-{n-1}.md`를 Read해
     헤더의 **직전 상태 대비**를 채운다.
   - `k6-test-summary-{n}.json`: 최상위에 `delta_vs_prev`를 덧붙인다. 다른 필드는 손대지 마라. 값은 각 파일의 것을 자릿수 그대로 옮긴다.

     ```json
     "delta_vs_prev": {
       "from": "k6-test-summary-{n-1}.json",
       "rps": { "before": 37.31, "after": 82.44 },
       "duration_p95_ms": { "before": 1734.5, "after": 612.8 },
       "duration_p99_ms": { "before": 2887.8, "after": 941.2 }
     }
     ```

4. 전후를 두 축으로 제시하고 **개선 여부 판정을 묻는다** (`SKILL.md`의 **역할 경계**).
   - 하드웨어 의존 증거: p95, p99, RPS, 쿼리 전체 소요(EXPLAIN ANALYZE 루트), heap 최대, GC 일시정지 합과 최장 정지, HikariCP pending 최대, 커넥션 보유 평균, process CPU 최대.
     로컬 절대값은 믿지 말고 상대 변화만 쓴다.
   - 하드웨어 독립 증거: 요청당 쿼리 수, `examined_per_sent`, Handler / Sort 카운터, 접근 방식과 사용 인덱스, 요청당 리포지토리 호출 수, 요청당 할당량,
     캐시 적중률 (구획이 있을 때).
   - 실행계획은 Phase 6과 같은 노드별 표로, 칼럼 설명을 함께 붙인다.
   - 물을 것: "이 변화가 기법의 효과라고 보십니까, 측정 편차라고 보십니까?"
   - 개선이 없거나 나빠졌으면 그대로 제시한다. 유리하게 해석하지 마라.

5. 5-B의 **호출자가 예상한 효과**와 실측을 대조한다. 맞았으면 어떤 근거가 맞았는지, 어긋났으면 어느 가정이 틀렸는지 관측값으로 짚는다.
   실측 없이 "예상대로 개선되었다"고 쓰지 마라.

6. 종료를 판정한다. **개선 여부는 하드웨어 독립 증거로 판정한다.** p95나 RPS만으로 개선을 주장하거나 종료를 판정하지 마라.

   | 조건 | 판정 |
   |---|---|
   | 하드웨어 독립 증거에 변화가 없음 | 종료 |
   | Phase 6 위험 신호가 모두 해소됨 | 종료 |
   | 호출자가 종료를 선택 | 종료 |
   | 그 외 | 계속 |

   판정만 하고, 계속할지는 호출자에게 확인한다.

### 출력
- `k6-test-summary-{n}.json` (`delta_vs_prev` 포함), `jvm-metrics-{n}.md`, `query-stats-summary-{n}.md` (가공본), `query-plan-{n}.txt` (원본)
- `record.md`의 사이클 {n} **개선 후 지표**와 **판정**, 진행 상태 Phase 8 ✅

### 실패 처리
- 조건이 1차와 달라졌는데 되돌릴 수 없으면 비교 가능한 지표만 해석하고 나머지는 "조건 변경으로 비교 불가"로 명시한다.
- 에러율이 올랐으면 수치 비교보다 원인을 먼저 보고한다.

> 다음 Phase 조건: 종료 판정이거나 호출자가 종료를 선택 → Phase 9. 계속 → Phase 5 (진행 상태에 사이클 행 추가, 번호 +1)
>
> Skip 조건: 없음
