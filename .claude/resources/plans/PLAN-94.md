# [PLAN-94] 리뷰 에이전트 제거와 스킬 구조 정리

> 이슈: #94
> 브랜치: chore/94-claude-tooling-cleanup

## 목표
실제 작업 흐름에서 쓰이지 않는 리뷰 에이전트 3종과 `write-api-docs` 스킬을 제거하고, `run-test`를 `write-test`에 병합해 Claude 도구 구성을 슬림화한다. 삭제 대상을 참조하던 다른 스킬 문서의 끊어진 링크를 함께 정리한다.

## 영향 범위
### 삭제 파일
- `.claude/agents/logic-reviewer.md` — 리뷰 에이전트
- `.claude/agents/security-reviewer.md` — 리뷰 에이전트
- `.claude/agents/performance-reviewer.md` — 리뷰 에이전트
- `.claude/agents/` — 위 3개 삭제 후 빈 디렉토리 제거
- `.claude/skills/run-test/SKILL.md` — write-test로 병합
- `.claude/skills/write-api-docs/SKILL.md` — 스킬 폐기
- `.claude/skills/write-api-docs/template/api-docs-template.md` — 스킬 폐기 (코드 골격은 아래 3번대로 스펙에 이관)

> 주의: `write-api-docs` 두 파일은 **이미 index에 삭제로 스테이징**되어 있으나 작업 트리에는 untracked로 남아 있다(`git status --short` → `D`/`??`). 파일 시스템에서 실제로 지워야 정리가 끝난다. `rm -rf`는 settings.json deny 목록이므로 파일 단위 `rm` + `rmdir`로 지운다.

### 수정 파일
- `.claude/spec/api-docs-convention.md` — 폐기되는 템플릿의 인터페이스 골격을 코드 블록으로 흡수 (500 응답 블록 제외)
- `.claude/skills/write-test/SKILL.md` — run-test 병합 (frontmatter description·allowed-tools, Phase 5 확장)
- `.claude/skills/implement/template/output.md` — 19행 `**다음**: write-test → run-test → open-pr` → `write-test → open-pr`
- `.claude/skills/optimize-performance/SKILL.md` — 6행 `Do NOT use for`에서 `코드만 보고 하는 정적 성능 리뷰(→ performance-reviewer)` 제거
- `.claude/skills/fix-concurrency/SKILL.md` — 6행 `Do NOT use for`에서 `코드만 보고 하는 정적 리뷰(→ performance-reviewer)` 제거
- `.claude/skills/review-feedback/SKILL.md` — 6행 `Do NOT use for`의 `자체 코드 리뷰(→ logic-reviewer 등 리뷰 에이전트)` 제거

## 구현 계획
> `.claude/` 도구 정리 작업이라 애플리케이션 레이어(Entity/Repository/Service/DTO/Controller) 변경과 DB 마이그레이션은 없다. 서비스 정책(`.claude/spec/service-policy/`) 변경도 없다.

### 1. 에이전트 삭제
`.claude/agents/` 하위 3개 파일을 삭제하고 디렉토리를 제거한다.
```bash
rm .claude/agents/logic-reviewer.md .claude/agents/security-reviewer.md .claude/agents/performance-reviewer.md
rmdir .claude/agents
```

### 2. write-api-docs 스킬 삭제
```bash
rm .claude/skills/write-api-docs/SKILL.md .claude/skills/write-api-docs/template/api-docs-template.md
rmdir .claude/skills/write-api-docs/template .claude/skills/write-api-docs
```
API 문서 작성 규칙 자체는 `.claude/spec/api-docs-convention.md`에 남으며 CLAUDE.md의 참조도 유지한다. Docs 인터페이스 작성은 계획서에 명시되면 `implement`가 이 스펙을 읽고 수행한다(PLAN-92의 `AuthControllerDocs` 작업이 실제로 그렇게 진행됐다).

### 3. 인터페이스 골격을 api-docs-convention.md로 이관
`.claude/spec/api-docs-convention.md` 맨 끝(`## DTO @Schema` 섹션 뒤)에 `## 인터페이스 골격` 섹션을 추가하고, 폐기되는 템플릿의 코드 블록을 옮긴다. 단 **500 응답 `@ApiResponse` 블록은 옮기지 않는다** — 기존 Docs 인터페이스 9개 전부가 500을 선언하지 않아 템플릿 쪽이 코드와 어긋난 상태였다. 옮길 골격은 다음과 같다.

```java
package uss.code.{domain}.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uss.code.auth.annotation.Auth;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "{Domain} API", description = "{도메인} 관련 API")
public interface {Controller}Docs {

    @Operation(summary = "{기능 요약}", description = "{상세 설명}<br>"
            + "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ {성공 메시지}"),
            @ApiResponse(responseCode = "404", description = "🚨 {에러 설명}",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "{에러명}",
                                            value = "{\"code\" : {코드}, \"message\" : \"에러 메시지\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<{ResponseType}> methodName(@Auth final long memberId);
}
```

### 4. run-test → write-test 병합
`.claude/skills/write-test/SKILL.md`를 다음과 같이 고친다.

**frontmatter**
- `description`의 `Trigger`에 run-test 트리거를 흡수: `"테스트 돌려줘"`, `"테스트 실행해줘"`, `"테스트 결과 확인해줘"` 추가
- `description`의 `Do NOT use for`에서 `테스트 실행/결과 확인(→ run-test)` 제거
- `allowed-tools`의 `Bash`는 그대로 둔다 (`./gradlew test` 실행 필요)

**본문 상단 인자 형식**
- `<클래스명>` 단일 형식에서, 인자가 이미 존재하는 테스트 클래스이거나 비어 있으면 작성 단계를 건너뛰고 실행 단계로 진입한다는 분기를 명시

**Phase 1~4**
- Skip 조건에 "실행만 요청된 경우(대상 테스트 클래스가 이미 존재하거나 인자가 비어 있음) — Phase 5로 직행" 추가

**Phase 5: 테스트 실행 (run-test Phase 1~3 흡수)**
1. 인자가 비어 있으면 `./gradlew test`, 있으면 `./gradlew test --tests "{패키지}.{클래스}"`
2. 전부 통과하면 한 줄 보고 후 종료
3. 실패 시 실패 로그에서 `클래스명#메서드명` 추출, 에러 메시지와 스택 트레이스로 핵심 원인 파악, 필요하면 실패 테스트 소스를 Read
4. 실패 원인이 **이번에 작성한 테스트 코드** 쪽이면 수정 후 재실행을 반복한다
5. 실패 원인이 **테스트 대상(프로덕션) 코드의 버그**면 원인과 수정 방안을 보고만 하고 사용자 확인 없이 고치지 마라 (스킬 Boundary와 동일)
6. 최종 결과 보고: 실행 대상, 통과/실패 수, 실패 시 원인 요약

> 위 5번 항목은 기존 write-test Phase 5의 "실패하면 분석하고 수정하라"와 run-test의 "사용자 확인 없이 수정하지 마라"가 충돌하는 지점을 정리한 것이다. 스킬이 방금 쓴 테스트는 스킬 책임이므로 고치고, 대상 코드 버그는 write-test의 기존 Boundary대로 보고만 한다.

### 5. 끊어진 참조 정리
| 파일 | 현재 | 변경 후 |
|---|---|---|
| `implement/template/output.md:19` | `**다음**: \`write-test\` → \`run-test\` → \`open-pr\`` | `**다음**: \`write-test\` → \`open-pr\`` |
| `optimize-performance/SKILL.md:6` | `Do NOT use for: 코드만 보고 하는 정적 성능 리뷰(→ performance-reviewer), 구현 계획 수립(→ write-plan), 계획 기반 구현(→ implement)` | `Do NOT use for: 구현 계획 수립(→ write-plan), 계획 기반 구현(→ implement)` |
| `fix-concurrency/SKILL.md:6` | `Do NOT use for: 응답시간, 처리량이 목표인 성능 개선(→ optimize-performance), 코드만 보고 하는 정적 리뷰(→ performance-reviewer), 구현 계획 수립(→ write-plan)` | `Do NOT use for: 응답시간, 처리량이 목표인 성능 개선(→ optimize-performance), 구현 계획 수립(→ write-plan)` |
| `review-feedback/SKILL.md:6` | `Do NOT use for: 자체 코드 리뷰(→ logic-reviewer 등 리뷰 에이전트), PR 생성(→ open-pr), 계획 기반 구현(→ implement)` | `Do NOT use for: PR 생성(→ open-pr), 계획 기반 구현(→ implement)` |

`.claude/CLAUDE.md`는 에이전트나 삭제 대상 스킬을 참조하지 않으므로 수정하지 않는다. `.claude/settings.json`과 `.claude/hooks/`도 참조가 없다.
`.claude/resources/plans/PLAN-92.md`의 `api-docs-convention.md` 언급은 과거 작업 기록이므로 건드리지 않는다.

## 결정 필요 (Decisions needed)
- [x] **`write-api-docs/template/api-docs-template.md`의 코드 골격 처리** — A) 그대로 폐기 / B) `.claude/spec/api-docs-convention.md`로 이관
  → **B 확정.** 위 3번대로 이관한다. 단 템플릿의 500 응답 블록은 실제 Docs 인터페이스 9개 중 어디에도 없어 코드와 어긋나므로 옮기지 않는다.
- [x] **`write-test`의 실행 전용 모드 지원 범위** — A) 인자 비었거나 기존 테스트 클래스면 작성 건너뛰고 실행만 / B) 작성 전용 유지하고 "테스트 돌려줘"는 스킬 없이 처리
  → **A 확정.** 위 4번 구현 계획이 A 기준으로 작성되어 있다.

## 검증
- 실행할 애플리케이션 테스트 없음 (`src/` 변경 없음). 필요 시 `./gradlew test`로 회귀만 확인.
- 문서 정합성 확인:
  - `grep -rn "run-test\|write-api-docs\|logic-reviewer\|security-reviewer\|performance-reviewer" .claude --include="*.md"` 결과가 `PLAN-*.md`(과거 기록)와 `PLAN-94.md` 외에 남지 않을 것
  - `git status --short`에 `.claude/skills/write-api-docs/` untracked 항목이 사라질 것
  - `.claude/agents/`, `.claude/skills/run-test/`, `.claude/skills/write-api-docs/` 디렉토리가 존재하지 않을 것

## Deviation Log
- `.claude/spec/api-docs-convention.md`: 이관한 골격 코드 블록 아래에 `- 선언할 @ApiResponse는 해당 엔드포인트에서 실제로 발생하는 응답만이다. 500은 선언하지 마라` 한 줄을 추가 — 이유: 계획은 500 블록을 "옮기지 않는다"까지만 지시했다. 제외 이유를 남기지 않으면 다음 작성자가 관성으로 500을 다시 넣는다.
- `.claude/skills/write-test/SKILL.md`: H1을 `테스트 코드 작성` → `테스트 코드 작성과 실행`, description 첫 줄을 `테스트 코드를 작성한다` → `테스트 코드를 작성하고 실행한다`로 변경 — 이유: 계획은 Trigger 목록과 Phase만 명시했으나, 스킬이 실행까지 담당하게 된 이상 제목과 한 줄 요약이 범위를 그대로 드러내야 한다.
- `.claude/skills/write-test/SKILL.md`: `Do NOT use for`에 run-test에 있던 `빌드 설정 변경`을 흡수 — 이유: 계획은 `테스트 실행/결과 확인(→ run-test)` 제거만 지시했다. run-test가 사라지면서 이 경계 문구를 받아줄 곳이 write-test뿐이다.
- `.claude/skills/write-test/SKILL.md`: Phase 4의 `최종 결과를 사용자에게 보고하라` → `작성 결과를 정리하라`로 변경하고 `다음 Phase 조건`을 추가 — 이유: 실행(Phase 5) 앞에서 "최종 보고"를 하면 최종 보고가 두 번 나온다.
