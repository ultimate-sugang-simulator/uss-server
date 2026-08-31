# [PLAN-112] 스킬 다음 단계 안내 체인 정비

> 이슈: #112
> 브랜치: chore/112-skill-next-step-chain

## 목표
스킬 보고 템플릿의 다음 단계 안내를 실제 작업 흐름
(`open-issue` → 작업 스킬 → `write-test` → `commit-push` → `open-pr`)과 일치시킨다.
현재 `implement`는 `commit-push`를, `fix-concurrency`/`optimize-performance`는 `write-test`를 건너뛰고,
`write-test`는 다음 단계 안내 자체가 없어 체인이 끊긴다.

## 영향 범위
### 신규 파일
- `.claude/skills/write-test/template/output.md` - write-test 보고 템플릿 (다음 단계 안내 포함)

### 수정 파일
- `.claude/skills/write-test/SKILL.md` - Phase 5의 보고 지시를 템플릿 참조로 바꾸고 Phase 6(결과 보고) 신설
- `.claude/skills/implement/template/output.md` - 다음 단계에 `commit-push` 추가
- `.claude/skills/fix-concurrency/template/output.md` - 다음 단계에 `write-test` 추가
- `.claude/skills/optimize-performance/template/output.md` - 다음 단계에 `write-test` 추가
- `.claude/skills/optimize-performance/phases/phase-9-report.md` - 종료 안내 문장에 `write-test` 추가 (output.md와 정의 일치)

## 구현 계획
> 이번 작업은 Java 코드가 아닌 스킬 마크다운 자산 정비다. 레이어 구분 대신 파일 단위로 적는다.

1. **`implement/template/output.md`** (19행)
   - `**다음**: \`write-test\` → \`open-pr\`` → `**다음**: \`write-test\` → \`commit-push\` → \`open-pr\``

2. **`write-test/template/output.md`** (신설)
   - 기존 output.md 패턴을 따른다: 상단 작성 가이드 주석 + 항목만 채우는 본문.
   - 본문 구성 (SKILL.md Phase 5의 보고 요구사항을 그대로 옮긴다):
     - 제목: `## 테스트 완료 - {대상}`
     - **실행**: 실행 대상(전체 / 클래스)과 통과/실패 수
     - **작성**: 작성한 테스트 파일 경로와 메서드 수 (작성 없이 실행만 한 경우 "실행만")
     - **실패**: 남은 실패 목록 `클래스명#메서드명` - 원인 요약, 없으면 "없음"
     - **다음**: `commit-push` → `open-pr`
   - 가이드 주석에 명시: 실패가 남았으면 다음 항목을 `실패 해소 후 commit-push`로 적는다 (실패 상태에서 커밋 유도 금지).

3. **`write-test/SKILL.md`**
   - Phase 5 항목 2 `모두 통과하면 결과를 한 줄로 보고하고 종료하라` → `모두 통과하면 Phase 6으로 이동하라`
   - Phase 5 항목 6 `최종 결과를 사용자에게 보고하라: ...` (하위 불릿 포함) → 삭제하고 Phase 6으로 이관
   - Phase 5 말미 `> Skip 조건: 없음 (필수 Phase)` 앞에 `> 다음 Phase 조건: 실행 결과가 확정되었을 때(전부 통과 또는 반복 상한 도달)` 추가
   - Phase 6 신설 (다른 스킬의 결과 보고 Phase와 동일 형식):
     ```
     ## Phase 6: 결과 보고

     1. 보고 템플릿을 Read로 읽어라: `.claude/skills/write-test/template/output.md`
     2. 템플릿 상단 작성 가이드에 따라 항목을 채워 보고하라. (가이드 주석은 출력에 포함하지 않는다.)

     > Skip 조건: 없음 (필수 Phase)
     ```

4. **`fix-concurrency/template/output.md`** (24행)
   - `**다음**: \`commit-push\` → \`open-pr\`` → `**다음**: \`write-test\` → \`commit-push\` → \`open-pr\``

5. **`optimize-performance/template/output.md`** (19행)
   - `**다음**: \`commit-push\` → \`open-pr\`` → `**다음**: \`write-test\` → \`commit-push\` → \`open-pr\``

6. **`optimize-performance/phases/phase-9-report.md`** (48행)
   - `없으면 스킬 종료. 커밋은 \`commit-push\`, PR은 \`open-pr\`로 이어간다. 이 스킬은 커밋하지 않는다.`
     → `없으면 스킬 종료. 테스트는 \`write-test\`, 커밋은 \`commit-push\`, PR은 \`open-pr\`로 이어간다. 이 스킬은 커밋하지 않는다.`
   - output.md의 다음 안내와 문구가 어긋나지 않게 유지한다. (fix-concurrency phases에는 대응 문장 없음 - grep으로 확인 완료)

## 결정 필요 (Decisions needed)
- [x] write-test 다음 단계 안내 방식 - 옵션 A: output.md 신설 + Phase 6 / 옵션 B: Phase 5에 한 줄 추가
  → **옵션 A 채택.** 다른 모든 스킬이 보고 템플릿 + 결과 보고 Phase 구조를 쓰고 있어 형식 일관성이 유지되고,
  보고 형식 정의가 템플릿 한 곳에 모인다.

## 검증
- 대상 테스트: 해당 없음 (Java 코드 변경 없음)
- 수정 후 `grep -rn "다음" .claude/skills/*/template/output.md`로 전 스킬의 다음 안내가
  `open-issue → (write-plan → implement | fix-concurrency | optimize-performance) → write-test → commit-push → open-pr → review-feedback` 체인과 일치하는지 대조한다.

## Deviation Log
> implement 스킬이 구현 중 계획을 벗어난 지점을 여기에 기록한다. (작성 시점엔 비워둔다)
