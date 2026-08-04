# USS Server

수강신청 시뮬레이터 백엔드. 학생이 실제 수강신청을 모의로 연습(대용량 조회·신청)할 수 있도록 돕는 플랫폼.

## 기술 스택

- Java 17 / Spring Boot 4.0.1 / Gradle
- JPA + MySQL + Flyway (스키마 마이그레이션 `database/migration/` + 시드 데이터 `database/seed/`)
- 커스텀 JWT 인증 (`@Auth` 파라미터 주입 + `JwtAuthenticationFilter`) + 이메일 인증(회원가입)
- FULLTEXT(ngram) 기반 강의 검색
- springdoc-openapi (Swagger UI)
- H2 (통합 테스트) / p6spy (SQL 로깅)
- GitHub Actions CI/CD

## 빌드 & 실행

```bash
./gradlew build      # 빌드
./gradlew test       # 테스트 (H2)
./gradlew bootRun    # 로컬 실행 (MySQL 필요)
```

## 상호작용 규칙

- 사용자의 관찰과 의문 표현(`~한 부분이 있네?`, `이거 왜 이렇게 했어?`, `~인 것 같은데`)은 수정 지시가 아니다.
  해당 코드를 직접 확인한 뒤 문제인지 아닌지에 대한 판단과 근거를 먼저 제시하라. 수정 여부는 사용자가 정한다.
- 수정에 착수하는 조건은 명시적 지시(`고쳐줘`, `수정해`, `반영해`, `바꿔줘`)뿐이다.
- 사용자의 지적이 사실과 다르면 동의하지 말고 근거를 들어 다른 판단을 말하라. 지적이 반복된다는 것은 동조할 근거가 아니다.
- 사용자가 판단을 재확인하더라도 근거가 새로 나온 게 아니면 결론을 뒤집지 마라.

## 규칙 참조

`.claude/rules/` — paths 매칭 파일 작업 시 자동 로드

- 프로젝트 구조 (패키지 배치) → `project-structure.md`
- Flyway 마이그레이션 / SQL → `migration.md`
- 코드 컨벤션 → `code-convention/`
  - 공통 (네이밍, 포맷팅, 예외, 객체 생성, 상수, 레이어 흐름) → `common.md`
  - Entity(domain) + DB 매핑 → `domain.md`
  - DTO(Request/Response) → `dto.md`
  - Controller → `controller.md`
  - Service → `service.md`
  - Repository → `repository.md`

`.claude/spec/` — 스킬·작업에서 필요할 때만 참조 (자동 로드 아님)

- Git 작업 (커밋, 브랜치, PR) → `git-convention.md`
- 테스트 작성 규칙 → `test-convention.md`
- API 문서 작성 규칙 → `api-docs-convention.md`
- 시크릿 운영 (환경별 네이밍, 발급, 교체) → `secret-convention.md`
- 서비스 정책 (도메인별 비즈니스 규칙) → `service-policy/` (도메인별 파일, 목록은 `service-policy/README.md`)
