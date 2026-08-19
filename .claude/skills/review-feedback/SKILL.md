---
name: review-feedback
description: |
  PR에 달린 코드래빗 피드백을 수집해 코드베이스 기준으로 타당성을 상중하로 판정하고, 사용자가 고른 항목만 수정한다.
  Trigger: "코드래빗 피드백 봐줘", "리뷰 피드백 검토해줘", "PR 리뷰 확인해줘", "코드래빗 뭐라는지 봐줘"
  Do NOT use for: PR 생성(→ open-pr), 계획 기반 구현(→ implement)
  Boundary: 피드백 판정, 사용자가 선택한 항목의 수정, 반영한 항목의 스레드 resolve까지 수행한다. 판정만으로 코드를 고치지 않는다.
allowed-tools: Read, Grep, Glob, Edit, Bash(gh *), Bash(git *), Bash(jq *), Agent(feedback-judge)
model: opus
effort: xhigh
---

# 리뷰 피드백 검토

현재 PR에 코드리뷰를 남기는 코드래빗은 변경된 파일과 관련된 일부를 기준으로 피드백을 남긴다.
따라서, 피드백을 그대로 수용하지 않고 코드베이스를 이해하고 있는 본 스킬을 통해 코드를 직접 확인한 뒤 타당성을 판정하도록 한다.

대상 PR: $ARGUMENTS (비어있으면 현재 브랜치의 PR)

## Phase 1: 대상 PR 확정

1. $ARGUMENTS에 PR 번호가 있으면 그것을 쓴다.
2. 없으면 현재 브랜치의 PR을 찾아라:
   ```bash
   gh pr view --json number,title,headRefName,url
   ```
   - PR이 없으면 "PR이 없습니다. 먼저 `open-pr`를 실행하세요"를 알리고 중단하라.
3. Phase 7 보고에 PR URL이 필요하다. $ARGUMENTS로 번호를 받은 경우에도
   `gh pr view {번호} --json number,title,headRefName,url`로 URL까지 확보하라.

> 다음 Phase 조건: 대상 PR 번호와 URL을 확보했을 때

> Skip 조건: 없음 (필수 Phase)

## Phase 2: 피드백 수집

아래 세 호출 모두 `--paginate`를 빼지 마라. 코멘트와 리뷰는 컬렉션 엔드포인트라 기본 30건까지만 내려주고,
빠뜨린 페이지는 오류 없이 조용히 사라진다. `--slurp`은 `--jq`와 같이 못 쓰므로(gh가 거부한다)
페이지마다 적용되는 스트리밍 필터로 쓴다.

**피드백 원문(body)은 메인 컨텍스트에 올리지 않는다.** 원문이 포함된 출력은 세션 스크래치패드 파일로
리다이렉트하고, 컨텍스트에는 body를 뺀 인덱스만 남긴다. `{스크래치패드}`는 세션 스크래치패드 디렉토리의
전체 경로다.

1. 판정 대상인 코드래빗 최상위 인라인 피드백의 원문을 파일로 수집하라. 화면에 출력하지 마라:
   ```bash
   gh api --paginate "repos/{owner}/{repo}/pulls/{번호}/comments" \
     --jq '.[] | select(.user.login == "coderabbitai[bot]") | select(.in_reply_to_id == null) | {id, path, line, body}' \
     > {스크래치패드}/rf-{번호}-items.jsonl
   ```
2. 답글이 달린 원본 코멘트 id를 따로 모아라. 1번은 답글과 사람 코멘트를 이미 걸러낸 결과라
   여기서 답글 여부를 판정할 수 없다. 작성자를 가리지 말고 전부 가져와야 사람이 단 답글도 잡힌다:
   ```bash
   gh api --paginate "repos/{owner}/{repo}/pulls/{번호}/comments" \
     --jq '.[] | select(.in_reply_to_id != null) | .in_reply_to_id'
   ```
3. 요약 리뷰 원문을 파일로 수집하라 (Nitpick과 Outside diff range 항목이 여기에 접혀 있다):
   ```bash
   gh api --paginate "repos/{owner}/{repo}/pulls/{번호}/reviews" \
     --jq '.[] | select(.user.login == "coderabbitai[bot]") | .body' \
     > {스크래치패드}/rf-{번호}-reviews.txt
   ```
4. 판정 대상 인덱스만 컨텍스트로 가져와라. 원문 파일을 Read하지 마라:
   ```bash
   jq -c '{id, path, line}' {스크래치패드}/rf-{번호}-items.jsonl
   ```
5. 4번 결과 중 `id`가 2번 목록에 있는 항목은 처리 이력이 있으므로 판정 대상에서 빼고 개수만 보고에 남겨라.
6. 인라인 항목이 0건이고 요약 리뷰 파일도 비어 있으면 코드래빗 피드백이 없는 것이다. 그 사실을 알리고 종료하라.

> 다음 Phase 조건: 판정 대상 항목 목록이 정리되었을 때

> Skip 조건: 없음 (필수 Phase)

## Phase 3: 타당성 판정 (feedback-judge 위임)

판정은 전용 에이전트 `feedback-judge`가 한다. 메인 컨텍스트에서 피드백 원문과 대상 코드를 읽지 마라.
판정 기준과 반환 형식은 `.claude/agents/feedback-judge.md`에 있다. 여기에 중복해 적지 마라.

1. 판정 대상 인라인 항목마다 Agent 도구로 `feedback-judge`를 호출하라.
   - 프롬프트에 넘길 것: 항목 id, `rf-{번호}-items.jsonl`의 전체 경로
   - 항목들은 서로 독립이므로 한 메시지에 병렬로 호출하되, 한 번에 최대 10개까지만 띄운다.
     초과분은 앞 배치가 끝난 뒤 다음 배치로 호출하라.
2. 요약 리뷰 파일(`rf-{번호}-reviews.txt`)이 비어 있지 않으면 `feedback-judge`를 1회 더 호출해
   접힌 항목의 추출과 판정을 함께 위임하라. 프롬프트에 파일 전체 경로를 넘긴다.
3. 반환된 행을 모아라. 행이 형식(`항목id | 판정 | 대상 | 요지 | 근거 | 수정 지침`)에 맞지 않으면
   해당 항목만 재호출하라.

> 다음 Phase 조건: 모든 판정 대상 항목에 판정 행이 확보되었을 때

> Skip 조건: 없음 (필수 Phase)

## Phase 4: 판정 보고

1. [template/verdict-table.md](template/verdict-table.md)를 읽어 그 형식대로 **전체 항목**을 표로 보고하라.
2. **이 시점에 코드를 수정하지 마라.** 상으로 판정한 항목도 예외가 아니다.

> 다음 Phase 조건: 표를 보고하고 사용자 선택을 기다릴 때

> Skip 조건: 없음 (필수 Phase)

## Phase 5: 선택 항목 수정

1. 사용자가 고른 번호만 수정하라. 고르지 않은 항목은 건드리지 마라.
2. 판정 행의 수정 지침으로 부족해 피드백 원문이 필요하면 해당 항목의 body만 추출해 읽어라.
   원문 파일 전체를 Read하지 마라:
   ```bash
   jq -r 'select(.id == {항목id}) | .body' {스크래치패드}/rf-{번호}-items.jsonl
   ```
3. 수정은 `.claude/rules/code-convention/`을 따른다. 코드래빗이 제안한 diff를 그대로 붙여넣지 말고, 이 코드베이스의 패턴에 맞춰 다시 써라.
4. 수정으로 서비스 정책이 바뀌면 `.claude/spec/service-policy/`의 해당 도메인 파일도 함께 고쳐라.
5. 수정 범위가 계획서 한 건 수준으로 커지면 여기서 멈추고 `write-plan`을 권하라.

> 다음 Phase 조건: 선택된 항목이 모두 반영되었을 때

> Skip 조건: 사용자가 아무 항목도 고르지 않았을 때

## Phase 6: 반영 항목 resolve 처리

수정해서 반영한 항목만 스레드를 resolve로 바꾼다. 판정만 하고 넘어간 항목은 그대로 둔다.
사용자가 나중에 무엇이 미처리로 남았는지 스레드 상태만 보고 알 수 있어야 하기 때문이다.

1. 스레드 id를 가져와라. 인라인 코멘트 id(`databaseId`)로 스레드를 찾는다:
   ```bash
   gh api graphql -f query='
     query($owner:String!, $repo:String!, $pr:Int!) {
       repository(owner:$owner, name:$repo) {
         pullRequest(number:$pr) {
           reviewThreads(first:100) {
             nodes { id isResolved comments(first:1) { nodes { databaseId } } }
           }
         }
       }
     }' -F owner={owner} -F repo={repo} -F pr={번호} \
     --jq '.data.repository.pullRequest.reviewThreads.nodes[]
           | select(.isResolved | not)
           | {threadId: .id, commentId: .comments.nodes[0].databaseId}'
   ```
2. Phase 5에서 반영한 항목의 코멘트 id에 해당하는 스레드만 resolve 하라:
   ```bash
   gh api graphql -f query='
     mutation($threadId:ID!) {
       resolveReviewThread(input: {threadId: $threadId}) { thread { id isResolved } }
     }' -F threadId={스레드 id}
   ```
3. 요약 리뷰에 접혀 있던 Nitpick과 Outside diff range 항목은 인라인 스레드가 없어 resolve 대상이 아니다.
   반영했더라도 스레드 상태는 바꿀 수 없으니 보고에만 남겨라.

> 다음 Phase 조건: 반영한 인라인 항목의 스레드가 모두 resolve 되었을 때

> Skip 조건: Phase 5를 스킵했을 때(반영한 항목이 없을 때)

## Phase 7: 결과 보고

1. 보고 템플릿을 Read로 읽어라: `.claude/skills/review-feedback/template/output.md`
2. 템플릿 상단 작성 가이드에 따라 항목을 채워 보고하라. (가이드 주석은 출력에 포함하지 않는다.)

> Skip 조건: 없음 (필수 Phase)
