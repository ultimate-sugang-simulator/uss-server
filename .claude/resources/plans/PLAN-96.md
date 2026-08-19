# [PLAN-96] 클로드 도구 서브에이전트 위임과 토큰 낭비 방지

> 이슈: #96
> 브랜치: chore/96-claude-harness-improvement

## 목표
기계적인 사실 조립 작업(코드래빗 피드백 판정, 쿼리 출처 매핑)을 서브에이전트로 위임해 원문과 탐색 흔적이 메인 컨텍스트에 남지 않게 하고, 장기 실행 명령의 실행 규칙(백그라운드, 로그 파일화, 유한 대기, 손절)을 세워 반복 실패로 새는 토큰을 막는다.

## 사전 확인 (2026-08-19, 공식 문서 기준)
- 스킬 frontmatter `allowed-tools`에서 서브에이전트 호출 도구명은 **`Agent`**다 (`Task`는 과거 명칭).
- `Agent(에이전트명)` 표기로 **그 스킬이 띄울 수 있는 에이전트를 특정 이름으로 제한**할 수 있다. 스킬 전용 에이전트에 이 표기를 쓴다.
- `.claude/agents/*.md` frontmatter 지원 필드: `name`(필수), `description`(필수), `tools`(허용 목록), `model`(`sonnet`/`opus`/`haiku`/`inherit`, 기본 inherit), `effort`, `maxTurns` 등.
- #94에서 삭제한 에이전트 3종은 어느 워크플로에도 물리지 않은 독립 리뷰어였다. 이번에 만드는 에이전트는 **스킬 phase에 명시적으로 물리는 전용 워커**이며, description에 전용임을 못 박아 같은 문제의 재발을 막는다.

## 영향 범위
### 신규 파일
- `.claude/agents/feedback-judge.md` — review-feedback 전용. 코드래빗 피드백 항목의 타당성을 코드베이스 기준으로 판정해 정형 행으로 반환
- `.claude/agents/query-source-mapper.md` — optimize-performance 전용. 1차 쿼리 통계 출력을 템플릿 규칙대로 가공본으로 재작성(쿼리와 리포지토리 메서드 매핑)

### 수정 파일
- `.claude/skills/review-feedback/SKILL.md` — Phase 2를 파일 리다이렉트 수집으로, Phase 3을 에이전트 병렬 위임으로 재작성. allowed-tools 갱신
- `.claude/skills/optimize-performance/SKILL.md` — allowed-tools에 `Agent(query-source-mapper)` 추가
- `.claude/skills/optimize-performance/phases/phase-4-baseline.md` — 절차 3(가공본 작성)을 에이전트 위임으로 교체
- `.claude/skills/optimize-performance/phases/phase-8-verify.md` — 절차 3의 query-stats 가공본 항목을 에이전트 위임으로 교체 (k6 `delta_vs_prev`는 메인 유지)
- `.claude/CLAUDE.md` — **명령 실행 규칙** 섹션 신설 (백그라운드, 로그 파일화, 유한 대기, 손절)
- `.claude/skills/write-test/SKILL.md` — Phase 5의 수정, 재실행 반복에 상한 추가

> 애플리케이션 레이어와 DB 마이그레이션, 서비스 정책 변경 없음. `.claude/settings.json`과 훅은 손대지 않는다(훅 강제는 이번 범위 밖).

## 구현 계획

### 1. `.claude/agents/feedback-judge.md` 신규 작성
frontmatter:
```yaml
name: feedback-judge
description: review-feedback 스킬 전용 판정 워커. 코드래빗 피드백 항목 하나(또는 요약 리뷰 묶음)의 타당성을 코드베이스, 컨벤션, 서비스 정책 기준으로 판정해 정형 행만 반환한다. 단독 호출용이 아니다.
tools: Read, Grep, Glob
model: sonnet
```
본문 구성:
- **입력**: 항목 id와 수집 파일 경로(jsonl 또는 요약 리뷰 텍스트). 요약 리뷰 파일이 입력이면 접힌 Nitpick, Outside diff range 항목을 추출해 각각 판정한다
- **판정 절차**: 현행 review-feedback Phase 3의 1~3을 이관 — 지목 파일과 호출부를 Read로 직접 확인, `.claude/rules/code-convention/`과 `.claude/spec/service-policy/`(해당 도메인 파일) 대조, 상/중/하 기준(현행 문구 그대로 이동)
- **반환 형식**: 항목당 한 행. `{항목id} | {판정: 상/중/하} | {대상: 클래스:라인} | {지적 요지 한 줄} | {판정 근거 한 줄} | {수정 지침 한 줄}` — 수정 지침은 메인이 원문 body 없이 Phase 5 수정에 착수할 수 있게 하는 사실 서술로 제한. 표기 규칙은 `template/verdict-table.md`와 동일

### 2. `.claude/agents/query-source-mapper.md` 신규 작성
frontmatter:
```yaml
name: query-source-mapper
description: optimize-performance 스킬 전용 가공 워커. mysql 1차 쿼리 통계 출력을 읽고 각 쿼리의 출처(리포지토리 메서드)를 Grep으로 매핑해, 템플릿 작성 규칙대로 같은 경로에 가공본을 덮어쓴다. 판정과 해석은 쓰지 않는다. 단독 호출용이 아니다.
tools: Read, Grep, Glob, Write
model: sonnet
```
본문 구성:
- **입력**: 1차 출력 파일 경로, `template/query-stats-template.md` 경로, `record.md` 경로(예상 쿼리 목록이 출처 매핑의 1차 후보), 상태 번호 `n`, k6 요약 경로(측정 조건 헤더용), `n>=1`이면 직전 가공본(`{n-1}`) 경로
- **절차**: 템플릿 상단 작성 규칙을 그대로 따른다(반올림 금지, 재정렬 금지, DIGEST_TEXT 전문 유지, 미상 처리, `n>=1`이면 직전 상태 대비 작성). 출처는 Grep으로 확인된 것만 적는다
- **반환**: 행 수, 요청당 쿼리 수 합, 출처 미상 목록, DIGEST 잘림 발생 여부 — 사실만. 병목 판정이나 개선 제안을 반환하지 마라

### 3. `.claude/skills/review-feedback/SKILL.md` 수정
- frontmatter `allowed-tools`: `Read, Grep, Glob, Edit, Bash(gh *), Bash(git *), Bash(jq *), Agent(feedback-judge)` — `jq`는 Phase 5에서 선택 항목의 원문만 추출하는 용도
- **Phase 2 (수집) 재작성** — 원문을 컨텍스트에 올리지 않는다:
  1. 인라인 피드백 원문을 세션 스크래치패드 파일로 리다이렉트: 기존 1번 명령 뒤에 `> {스크래치패드}/rf-{PR번호}-items.jsonl` (jq 필터에 `| @json` 등 jsonl 형태 유지)
  2. 답글 원본 id 목록: 기존 2번 명령 유지 (출력이 작아 컨텍스트 허용)
  3. 요약 리뷰 원문 리다이렉트: 기존 3번 명령 뒤에 `> {스크래치패드}/rf-{PR번호}-reviews.txt`
  4. 판정 대상 인덱스만 컨텍스트로: 1번과 같은 필터에서 `{id, path, line}`만 뽑는 gh 호출 1회 (body 제외)
  5. 답글 이력 제외와 0건 종료는 현행 5, 6번 유지
- **Phase 3 (판정) 재작성** — 메인은 코드를 읽지 않는다:
  1. 판정 대상 인라인 항목마다 `feedback-judge`를 호출한다. 프롬프트에 항목 id와 jsonl 경로를 넘긴다. **독립 항목이므로 한 메시지에 병렬 호출하되 한 번에 최대 10개**, 초과분은 다음 배치로
  2. 요약 리뷰 파일은 `feedback-judge` 1회 호출로 추출과 판정을 함께 위임
  3. 반환 행을 모은다. 현행 1~4번(직접 Read, 판정 기준)은 에이전트 정의로 이관하고 본문에서 제거
- Phase 4(보고)는 현행 유지 — 반환 행을 `template/verdict-table.md` 형식으로 조립
- Phase 5(수정) 보강: 수정에 원문이 필요하면 `jq`로 해당 id의 body만 추출해 읽는다. 전체 jsonl을 Read하지 마라

### 4. `.claude/skills/optimize-performance/SKILL.md` 수정
- frontmatter `allowed-tools` 끝에 `Agent(query-source-mapper)` 추가. 그 외 본문 변경 없음

### 5. `.claude/skills/optimize-performance/phases/phase-4-baseline.md` 수정
- 절차 3을 교체: 직접 가공(템플릿 Read, Grep 매핑) 지시를 삭제하고 `query-source-mapper` 호출로 대체. 프롬프트에 넘길 것: 1차 출력 경로, 템플릿 경로, `record.md` 경로, `n=0`, k6 요약 경로. 반환된 미상 목록과 잘림 여부를 확인하고, **가공본은 절차 4의 제시를 위해 메인이 Read한다**(호출자에게 제시할 근거는 메인 컨텍스트에 있어야 한다)

### 6. `.claude/skills/optimize-performance/phases/phase-8-verify.md` 수정
- 절차 3의 첫 불릿(query-stats 가공)을 5번과 같은 방식으로 교체하되 `n`과 직전 가공본(`{n-1}`) 경로를 함께 넘긴다 (직전 상태 대비 작성용)
- 둘째 불릿(k6 요약에 `delta_vs_prev` 덧붙이기)은 메인 유지 — k6 요약은 선별된 소형 JSON이라 위임 이득이 없다

### 7. `.claude/CLAUDE.md` 수정 — **명령 실행 규칙** 섹션 신설
위치: **상호작용 규칙** 섹션 뒤. 내용 4개 항목:
- 2분 이상 예상되는 명령(전체 빌드, 전체 테스트, 애플리케이션 기동)은 백그라운드로 실행한다. 포그라운드 타임아웃 뒤 같은 명령을 그대로 다시 돌리지 마라
- 빌드와 테스트 출력은 파일로 리다이렉트하고, 실패 시 tail과 grep으로 필요한 부분만 읽는다. 로그 원문 전체를 컨텍스트에 올리지 마라
- 무한 대기 금지. 준비 대기(서버 기동 등)는 한 번의 Bash 호출 안에서 유한 루프(시도 횟수 x 간격)로 수행하고, 시한 초과 시 실패로 종료해 원인을 확인한다
- 같은 명령이 같은 원인으로 2회 실패하면 재시도를 멈추고, 원인 분석과 다음 선택지를 보고한다

### 8. `.claude/skills/write-test/SKILL.md` 수정 — Phase 5 상한
- Phase 5의 4번을 교체: "실패 원인이 이번에 작성한 테스트 코드 쪽이면 수정하고 재실행하라. **수정, 재실행 반복은 최대 3회**다. 상한에 도달하면 중단하고 남은 실패 목록(`클래스명#메서드명`), 각 원인 분석, 시도한 수정 내용을 보고하라"

## 결정 필요 (Decisions needed)
- [x] **1. `feedback-judge`의 model** — A) `inherit` (세션 모델 그대로, 판정 품질 우선) / B) `sonnet` (비용 우선, 오판 시 사용자가 걸러야 함)
  → **B 확정 (사용자 선택).** `model: sonnet`으로 작성한다. 항목 수만큼 병렬 실행되므로 비용을 우선하고, 오판은 Phase 4 판정 표에서 사용자가 근거를 보고 거른다.
- [x] **2. 손절 상한 횟수** — A) CLAUDE.md 손절 2회 + write-test 수정, 재실행 상한 3회 (같은 명령 반복은 이르게 끊고, 테스트 수정은 수정이 개입되므로 한 번 더 허용) / B) 둘 다 3회로 통일
  → **A 확정 (사용자 선택).** CLAUDE.md 손절 2회, write-test 상한 3회로 작성한다.

## 검증
- 실행할 애플리케이션 테스트 없음 (`src/` 변경 없음)
- 문서 정합성: `grep -rn "feedback-judge\|query-source-mapper" .claude --include="*.md"` 결과가 에이전트 정의 2개, 두 스킬 문서, PLAN-96에만 나타날 것
- review-feedback의 Phase 3 판정 기준 문구가 SKILL.md에서 제거되고 feedback-judge에만 존재할 것 (중복 금지)
- 동작 확인: 코드래빗 피드백이 있는 PR에서 review-feedback을 1회 실행해 (1) 원문 jsonl이 스크래치패드에 생기고 (2) Agent 호출이 권한 프롬프트 없이 통과하며 (3) 판정 표가 현행 형식과 동일하게 나오는지 확인
- optimize-performance는 다음 성능 이슈에서 Phase 4 도달 시 확인 (이번 브랜치에서 측정 실행은 하지 않음)

## Deviation Log
- `.claude/skills/review-feedback/SKILL.md`: Phase 2의 인덱스 추출을 gh 재호출 대신 수집 파일에 대한 `jq -c '{id, path, line}'`로 변경 — 이유: 두 API 호출 사이에 코멘트가 추가되면 인덱스와 원문 파일이 어긋난다. 수집 파일에서 파생하면 항상 일치하고 API 호출도 준다.
- `.claude/skills/optimize-performance/phases/phase-4-baseline.md`, `phase-8-verify.md`: 절차 3 교체에 더해 절차 2(Read 대상에서 쿼리 통계 1차 출력 제거)와 절차 4(가공본 Read 시점 명시)도 수정 — 이유: 절차 2가 1차 출력을 메인에서 Read하게 두면 위임의 목적(원문 비적재)이 무효가 된다.
