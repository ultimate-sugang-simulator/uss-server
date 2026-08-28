# USS Server

수강신청 시뮬레이터 백엔드. 학생이 실제 수강신청을 모의로 연습(대용량 조회·신청)할 수 있도록 돕는 플랫폼.

## 기술 스택

- Java 17 / Spring Boot 4.0.1 / Gradle
- JPA + MySQL + Flyway (스키마 마이그레이션 `database/migration/` + 시드 데이터 `database/seed/`)
- 커스텀 JWT 인증 (`@Auth` 파라미터 주입 + `JwtAuthenticationFilter`)
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

## 명령 실행 규칙

- 2분 이상 예상되는 명령(전체 빌드, 전체 테스트, 애플리케이션 기동)은 백그라운드로 실행한다.
  포그라운드 타임아웃 뒤 같은 명령을 그대로 다시 돌리지 마라.
- 빌드와 테스트 출력은 파일로 리다이렉트하고, 실패 시 tail과 grep으로 필요한 부분만 읽는다.
  로그 원문 전체를 컨텍스트에 올리지 마라.
- 무한 대기 금지. 준비 대기(서버 기동 등)는 한 번의 Bash 호출 안에서 유한 루프(시도 횟수 x 간격)로 수행하고,
  시한 초과 시 실패로 종료해 원인을 확인한다.
- 같은 명령이 같은 원인으로 2회 실패하면 재시도를 멈추고, 원인 분석과 다음 선택지를 보고한다.

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
