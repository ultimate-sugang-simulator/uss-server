# [PLAN-57] 이메일 인증과 자체 회원가입 경로 제거

> 이슈: #57
> 브랜치: refactor/57-remove-email-verification

## 목표

학교 포털 로그인 API가 학생 본인 확인을 대신하므로, 이메일 인증 도메인과 메일 발송 인프라, 자체 회원가입 API, 회원의 `email` 필드를 모두 제거한다.
로그인과 JWT 발급, 검증은 손대지 않는다. 회원 식별 기준은 이메일에서 학번으로 옮기되, 학번 유니크 제약은 포털 로그인 연동 이슈로 미룬다.

## 영향 범위

### 신규 파일

- `src/main/resources/database/migration/V0_7__remove_email_verification.sql` - 인증코드 테이블 삭제, `members.email` 컬럼 삭제

### 삭제 파일

이메일 인증 도메인

- `src/main/java/uss/code/auth/controller/EmailVerificationCodeController.java`
- `src/main/java/uss/code/auth/controller/EmailVerificationCodeControllerDocs.java`
- `src/main/java/uss/code/auth/service/EmailVerificationCodeService.java`
- `src/main/java/uss/code/auth/repository/EmailVerificationCodeRepository.java`
- `src/main/java/uss/code/auth/domain/EmailVerificationCode.java`
- `src/main/java/uss/code/auth/dto/request/VerificationCodeSendRequest.java`
- `src/main/java/uss/code/auth/dto/request/VerificationCodeVerifyRequest.java`

메일 발송 인프라

- `src/main/java/uss/code/auth/infra/EmailSender.java`
- `src/main/java/uss/code/auth/infra/EmailTemplateGenerator.java`

자체 회원가입

- `src/main/java/uss/code/auth/dto/request/SignUpRequest.java`

사용처가 사라지는 공용 코드

- `src/main/java/uss/code/global/infra/RandomCodeGenerator.java`

> `AsyncConfig`와 `AsyncExceptionHandler`는 유지한다. `EmailSender.@Async`가 사라져 당분간 `@Async` 사용처가 없지만, 포털 로그인 연동에서 비동기 처리가 다시 필요할 수 있다.
> `@EnableAsync`가 남아 스레드풀 빈은 계속 생성되나 작업이 들어오지 않으므로 동작에 영향은 없다.

### 수정 파일

- `src/main/java/uss/code/member/domain/Member.java` - `email` 필드와 `signUp` 정적 팩토리 제거
- `src/main/java/uss/code/member/repository/MemberRepository.java` - `existsByEmail` 제거
- `src/main/java/uss/code/auth/service/AuthService.java` - `signUp`, `validateUserExists`, `EmailVerificationCodeRepository` 의존 제거
- `src/main/java/uss/code/auth/controller/AuthController.java` - `signUp` 핸들러 제거
- `src/main/java/uss/code/auth/controller/AuthControllerDocs.java` - 회원가입 문서 제거
- `src/main/java/uss/code/global/http/WhitelistEndpoint.java` - 회원가입, 이메일 인증 경로 4건 제거
- `src/main/java/uss/code/global/exception/domain/ExceptionCode.java` - 회원 중복, 이메일 인증 예외 코드 제거
- `build.gradle` - `spring-boot-starter-mail` 제거
- `src/main/resources/application-prod.yml` - `spring.mail` 블록 제거
- `src/test/resources/application.yml` - `spring.mail` 블록 제거
- `.github/workflows/cd-prod.yml` - `spring.mail.username`, `spring.mail.password` 주입 제거
- `src/test/java/uss/code/member/fixture/MemberFixture.java` - `email` 파라미터 제거
- `src/test/java/uss/code/member/service/MemberServiceTest.java` - 회원 생성을 픽스처로 전환
- `src/test/java/uss/code/course/service/CourseServiceTest.java` - 회원 생성을 픽스처로 전환
- `.claude/spec/service-policy/auth.md` - 이메일 인증, 회원가입 섹션 삭제 및 인증 예외 경로 갱신
- `.claude/rules/project-structure.md` - `auth/` 설명에서 "이메일 인증" 문구 제거

## 구현 계획

### 1. Entity / Flyway

**`V0_7__remove_email_verification.sql` 신규 작성**

`migration/`과 `seed/`가 같은 Flyway 버전 네임스페이스를 쓰므로(현재 최대 `V0_6`), 다음 버전은 `V0_7`이다.

```sql
-- 이메일 인증 절차 제거에 따른 인증코드 테이블 삭제
DROP TABLE IF EXISTS email_verification_codes;

-- 회원 식별 기준이 이메일에서 학번으로 옮겨감에 따른 이메일 컬럼 삭제
ALTER TABLE members DROP COLUMN email;
```

- `V0_0__init_table.sql`은 이미 적용된 파일이므로 수정하지 않는다 (checksum 실패 방지).
- `student_id` 유니크 제약은 이번 범위에서 제외한다 (결정 필요 2 참조). `INDEX idx_student_id`는 그대로 둔다.

**`Member.java` 수정**

- `email` 필드와 `@Column(nullable = false, unique = true)` 제거
- `studentId` 매핑은 `@Column(nullable = false, name = "student_id")` 그대로 둔다
- private `@Builder` 생성자에서 `email` 파라미터 제거
- `public static Member signUp(SignUpRequest, String)` **삭제**, `uss.code.auth.dto.request.SignUpRequest` import 제거
  - 삭제 후 `Member`에는 정적 팩토리가 없다. 포털 로그인 연동 이슈에서 포털 응답 기반 `Member.create(...)`를 새로 추가하는 것을 전제로 한다
  - `@NoArgsConstructor`는 유지한다. 테스트 픽스처가 리플렉션으로 이 생성자를 쓴다

### 2. Repository

- `MemberRepository.existsByEmail(String)` **삭제**. `findByStudentId(String)`만 남긴다
- `EmailVerificationCodeRepository` 인터페이스 파일 **삭제**

### 3. Service

**`AuthService`**

- `signUp(SignUpRequest)` **삭제**
- `validateUserExists(String email)` private 메서드 **삭제**
- 필드 `private final EmailVerificationCodeRepository emailVerificationCodeRepository` **삭제**
- 남는 것: `login(LoginRequest)`, 의존성은 `JwtProvider`, `PasswordEncoder`, `MemberRepository` 3개
- `ExceptionCode` static import는 `MEMBER_NOT_FOUND`, `PASSWORD_NOT_MATCH`만 쓰이도록 정리

**`EmailVerificationCodeService`** 파일 **삭제**

**`PasswordEncoder.encode(String)`는 유지한다.** `signUp` 삭제로 호출부가 사라지지만, `matches`와 같은 인터페이스 계약이고 포털 로그인 연동 시 재사용 가능성이 있다.

### 4. DTO

- `SignUpRequest`, `VerificationCodeSendRequest`, `VerificationCodeVerifyRequest` **삭제**
- `LoginRequest`, `AuthTokenResponse`는 유지
- `MemberProfileResponse`는 `email`을 노출하지 않으므로 수정 없음

### 5. Controller

**`AuthController`**

- `POST /api/v1/auth/sign-up` 핸들러 **삭제**, `SignUpRequest`와 `CREATED` static import 제거
- 남는 것: `POST /api/v1/auth/login`

**`AuthControllerDocs`**

- `signUp` 메서드와 `@Operation`, `@ApiResponses` 블록 **삭제**
- `SignUpRequest` import 제거. 로그인 문서는 그대로 둔다

**`EmailVerificationCodeController`, `EmailVerificationCodeControllerDocs`** 파일 **삭제**

### 6. 전역 설정

**`WhitelistEndpoint`** - 아래 4건 제거. 남는 것은 로그인과 Swagger 경로다.

- `POST /api/v1/auth/sign-up`
- `POST /api/v1/email-verification-codes`
- `POST /api/v1/email-verification-codes/re`
- `PATCH /api/v1/email-verification-codes`

**`ExceptionCode`** - 아래 항목 제거. `// 이메일 인증` 주석 그룹 전체가 사라진다.

- `MEMBER_ALREADY_EXISTS(1011)` - 호출부가 `signUp`과 `EmailVerificationCodeService`뿐이라 함께 사라진다
- `EMAIL_SENDING_FAILED(1013)` ~ `VERIFICATION_FAILED_LIMIT_EXCEEDED(1022)` 전부

> 비어버린 코드 번호(1011, 1013~1022)는 재사용하지 않는다. 클라이언트가 캐싱한 에러 코드와 의미가 어긋나는 것을 막기 위함이다.

**`build.gradle`** - `// Mail` 주석과 `implementation 'org.springframework.boot:spring-boot-starter-mail'` 제거

**`application-prod.yml`, `src/test/resources/application.yml`** - `spring.mail` 블록 전체 제거

**`.github/workflows/cd-prod.yml`** - `spring.mail.username`, `spring.mail.password` 주입 2줄과 위 빈 줄 제거

### 7. 테스트

**`MemberFixture`**

- `createMember(...)` 오버로드에서 `final String email` 파라미터와 `ReflectionTestUtils.setField(member, "email", email)` 제거
- 인자 없는 `createMember()`에서 UUID 기반 이메일 생성 제거. 나머지 기본값은 그대로 둔다 (`studentId`에 유니크 제약이 없으므로 유일성 보장이 필요 없다)

**`MemberServiceTest`, `CourseServiceTest`**

- `SignUpRequest` 생성과 `Member.signUp(...)` 호출을 `MemberFixture.createMember(...)`로 교체
- `TEST_EMAIL` 상수 및 `SignUpRequest` import 제거
- `MemberCollege` 등 enum은 문자열 상수 대신 enum 상수를 직접 넘긴다 (픽스처 시그니처가 enum 타입)

> 테스트는 H2 + `ddl-auto: create-drop`이라 Flyway를 타지 않는다. 마이그레이션 파일은 테스트 결과에 영향을 주지 않으므로 스키마 검증은 별도로 해야 한다.

### 8. 문서

**`.claude/spec/service-policy/auth.md`**

- `## 이메일 인증` 섹션 전체 삭제
- `## 회원가입` 섹션 삭제. 대신 "포털 로그인 연동 전까지 신규 가입 경로가 없다"는 한 줄을 남긴다
- `## 로그인`에 회원 식별 기준이 학번이라는 항목을 추가하되, 학번 유일성은 아직 보장되지 않는다는 사실을 함께 적는다
- `## 인증 예외 경로`에서 회원가입, 이메일 인증 항목 삭제. 로그인과 Swagger만 남긴다
- 파일 frontmatter의 `description`에서 "이메일 인증, 회원가입" 제거

**`.claude/rules/project-structure.md`** - `auth/` 줄의 "이메일 인증" 문구 제거

### 9. 배포 후속 (코드 밖, 사용자 수행)

- GitHub 저장소 시크릿 `MAIL_USERNAME`, `MAIL_PASSWORD` 삭제
- Gmail 앱 비밀번호 폐기

## 결정 필요 (Decisions needed)

- [x] **사용처가 사라지는 공용 코드를 함께 삭제할지** - **`RandomCodeGenerator`만 삭제하고 `AsyncConfig`, `AsyncExceptionHandler`는 유지한다.**
  비동기 설정은 포털 로그인 연동에서 다시 쓰일 여지가 있어 남기고, 코드 생성기는 사용처가 완전히 사라지므로 지운다.

- [x] **`members.student_id` 유니크 제약 추가 여부** - **이번 범위에서 제외한다.**
  신규 가입 경로가 사라져 중복이 새로 생기지 않으므로, 운영 데이터 중복 확인과 제약 추가는 포털 로그인 연동 이슈에서 다룬다.
  그동안 `AuthService.login`의 `findByStudentId`는 학번 중복 시 `NonUniqueResultException`이 날 수 있다. 기존과 동일한 위험 수준이며 이번 작업이 새로 만드는 문제는 아니다.

## 검증

- 대상 테스트: `MemberServiceTest`(프로필 조회 성공, 존재하지 않는 회원 조회 실패), `CourseServiceTest`(학년별 과목 조회 시나리오)
  - 두 클래스 모두 회원 생성 경로만 바뀌고 검증 대상 로직은 그대로다. 기존 단언이 그대로 통과해야 한다
- `./gradlew build`로 삭제한 클래스의 잔여 참조가 없는지 컴파일로 확인한다
- 애플리케이션 컨텍스트가 `spring-boot-starter-mail` 없이 기동되는지 통합 테스트 기동으로 확인한다 (`JavaMailSender` 빈 주입이 사라졌는지)
- 마이그레이션은 테스트에서 실행되지 않으므로, MySQL 로컬 인스턴스에 `V0_7`을 적용해 `DROP TABLE`과 `DROP COLUMN`이 통과하는지 별도 확인이 필요하다
- Swagger UI에서 회원가입, 이메일 인증 API가 사라지고 로그인만 남는지 확인한다

## Deviation Log

- `src/main/java/uss/code/global/config/MailConfig.java`: 계획서에 없던 파일을 추가로 삭제 — 이유: `spring.mail.*` 프로퍼티로 `JavaMailSender` 빈을 만드는 순수 메일 인프라이고 유일한 소비자가 `EmailSender`였다. 계획의 "메일 발송 인프라 전부 제거" 범위에 해당하나 파일 목록에서 누락되어 컴파일이 깨졌다.
- `.claude/spec/service-policy/README.md`: 계획서에 없던 파일을 추가로 수정 — 이유: `auth.md`의 frontmatter `description`을 고치면서 README 도메인 목록의 같은 문구도 함께 맞춰야 SSOT가 어긋나지 않는다.
