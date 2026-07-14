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
