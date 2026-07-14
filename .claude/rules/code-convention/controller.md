---
description: Controller 레이어 작성 패턴
paths:
  - "src/main/java/**/controller/**/*.java"
---

# Controller Convention

- `@RestController` + `@RequiredArgsConstructor`를 사용하라
- 기본 경로는 `@RequestMapping("/api/v1/{도메인복수형}")`으로 설정하라
- 반드시 `{Controller}Docs` 인터페이스를 implements 하라
- Controller에 비즈니스 로직을 넣지 마라. Service에 위임만 하라
- 인증된 사용자는 커스텀 `@Auth` 어노테이션으로 회원 식별자를 주입받아라 (`@Auth final long memberId`). `AuthArgumentResolver`가 JWT에서 값을 채운다
- RequestParam 검증이 필요하면 `@ParamValidation`(길이 등) / `@EnumValidation`을 사용하라
- 성공 응답은 `ResponseEntity`로 반환하라. 단순 200 조회는 `ResponseEntity.ok(...)`, 그 외 상태코드는 `ResponseEntity.status(HttpStatus.XXX).body(...)`(body 없으면 `.build()`)를 사용하라
