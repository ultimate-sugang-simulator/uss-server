# [PLAN-92] 포털 로그인을 자체 회원 인증으로 전환

> 이슈: #92
> 브랜치: feat/92-member-signup

## 목표

학교 포털 Oracle(`F_LOGIN_CHECK`)에 위임된 로그인을 걷어내고, 이메일과 비밀번호로 인증하는 자체 회원 체계로 바꾼다.
회원가입 경로가 생기면서 프로필을 처음부터 다 채워 받으므로, 미설정 상태를 메우려고 두었던 `DEFAULT("미정")` 열거값도 함께 없앤다.

## 영향 범위

### 신규 파일

- `src/main/resources/database/migration/V1_9__replace_portal_login_with_member_account.sql` - 기존 회원 정리, `email`, `password` 컬럼과 이메일 UNIQUE 추가
- `src/main/java/uss/code/auth/infra/MemberPasswordEncoder.java` - 회원 비밀번호 BCrypt 인코딩, 검증
- `src/main/java/uss/code/auth/dto/request/SignUpRequest.java` - 회원가입 요청 본문
- `src/main/java/uss/code/auth/dto/response/EmailAvailabilityResponse.java` - 이메일 사용 가능 여부 응답

### 수정 파일

**회원**

- `src/main/java/uss/code/member/domain/Member.java` - `email`, `password` 필드 추가, `createDefault` 제거하고 `create(...)` 추가
- `src/main/java/uss/code/member/domain/MemberCollege.java` - `DEFAULT` 제거
- `src/main/java/uss/code/member/domain/MemberDepartment.java` - `DEFAULT` 제거, `fromSelectable` 제거
- `src/main/java/uss/code/member/domain/MemberGrade.java` - `DEFAULT` 제거
- `src/main/java/uss/code/member/domain/AcademicStatus.java` - `DEFAULT` 제거
- `src/main/java/uss/code/member/repository/MemberRepository.java` - `findByStudentId` 제거, `findByEmail`, `existsByEmail` 추가
- `src/main/java/uss/code/member/service/MemberService.java` - `fromSelectable` 호출을 `from`으로 교체
- `src/main/java/uss/code/member/dto/response/MemberProfileResponse.java` - `email` 추가

**인증**

- `src/main/java/uss/code/auth/service/AuthService.java` - `InuMemberRepository` 의존 제거, `login` 교체, `signUp`, `checkEmailAvailability` 추가
- `src/main/java/uss/code/auth/controller/AuthController.java` - 가입, 중복 검사 엔드포인트 추가와 `@Validated` 부착
- `src/main/java/uss/code/auth/controller/AuthControllerDocs.java` - 가입, 중복 검사 문서 추가와 로그인 문서 갱신
- `src/main/java/uss/code/auth/dto/request/LoginRequest.java` - `studentId`를 `email`로 교체, `@Schema` 부착

**전역**

- `src/main/java/uss/code/global/http/WhitelistEndpoint.java` - 가입, 중복 검사 경로를 인증 예외에 추가
- `src/main/java/uss/code/global/exception/domain/ExceptionCode.java` - `PORTAL_LOGIN_FAILED`, `INVALID_DEPARTMENT` 제거, `EMAIL_ALREADY_EXISTS`, `COLLEGE_DEPARTMENT_MISMATCH` 추가
- `src/main/java/uss/code/course/service/CourseService.java` - `fromSelectable` 호출을 `from`으로 교체

**설정, 배포**

- `build.gradle` - `ojdbc11` 의존성 제거
- `src/main/resources/application-prod.yml` - `oracle` 블록 제거
- `src/main/resources/application-conc.yml` - `oracle` 블록 제거, 껐던 DataSource 헬스 프로브 복구
- `.github/workflows/cd-prod.yml` - `ORACLE_URL`, `ORACLE_USERNAME`, `ORACLE_PASSWORD` 주입 제거
- `.claude/skills/fix-concurrency/template/application-conc.yml` - 위 프로파일과 동일하게 정리
- `.claude/skills/fix-concurrency/template/seeds/member.sql` - `email`, `password` 컬럼 반영, `DEFAULT` 열거값을 실제 값으로 교체
- `.claude/skills/fix-concurrency/template/seeds/README.md` - 스키마 불일치 경고의 낡은 사유 문구 갱신
- `.claude/skills/fix-concurrency/template/mint-tokens.sh` - "왜 로그인 API를 쓰지 않는가" 주석을 새 인증 체계 기준으로 갱신

**정책 (SSOT)**

- `.claude/spec/service-policy/auth.md` - **정책 변경** (회원가입, 로그인 절 전면 개정)
- `.claude/spec/service-policy/member.md` - **정책 변경** (미설정 상태 삭제, 이메일 규칙 반영)

**테스트**

- `src/test/java/uss/code/global/infra/IntegrationTest.java` - `@Import(IntegrationTestConfig.class)` 제거
- `src/test/java/uss/code/UssServerApplicationTests.java` - 같은 `@Import` 제거
- `src/test/java/uss/code/auth/service/AuthServiceTest.java` - 로그인 시나리오 재작성, 가입, 중복 검사 시나리오 추가
- `src/test/java/uss/code/auth/dto/request/LoginRequestTest.java` - 이메일 검증으로 교체
- `src/test/java/uss/code/member/fixture/MemberFixture.java` - `email`, `password` 반영
- `src/test/java/uss/code/member/service/MemberServiceTest.java` - 미정 선택 시나리오 삭제, 미정 회원 픽스처를 실제 값으로 교체
- `src/test/java/uss/code/course/service/CourseServiceTest.java` - 미정 조회 시나리오 삭제, 미정 회원 시나리오를 실제 소속 기준으로 전환
- `src/test/java/uss/code/course/domain/CourseDepartmentTest.java` - `ownedBy(DEFAULT)` 단언 한 줄 삭제

### 삭제 파일

- `src/main/java/uss/code/member/repository/InuMemberRepository.java`
- `src/main/java/uss/code/global/config/OracleConfig.java`
- `src/test/java/uss/code/global/infra/IntegrationTestConfig.java` - 유일한 빈이 `InuMemberRepository` 목이라 남길 내용이 없다

## 구현 계획

### 1. Entity / Flyway

**`V1_9__replace_portal_login_with_member_account.sql`**

기존 회원은 포털 로그인으로 만들어져 비밀번호가 없다. 이메일과 비밀번호를 채울 방법이 없고 앞으로 로그인할 수도 없으므로 삭제한다.

```sql
-- 포털 로그인으로 만들어진 회원은 이메일과 비밀번호가 없어 새 인증 체계에서 로그인할 수 없다.
-- registrations, carts는 member_id에 ON DELETE CASCADE가 걸려 있어 함께 지워진다.
DELETE FROM members;

-- 위 CASCADE는 registrations만 지우고 courses의 수강인원은 건드리지 않아, 신청이 없는데 인원만 남는다.
UPDATE courses SET current_enrollment = 0;

-- 이메일과 비밀번호를 회원 인증 수단으로 추가한다. 남은 행이 없으므로 바로 NOT NULL로 만든다.
ALTER TABLE members
    ADD COLUMN email VARCHAR(255) NOT NULL,
    ADD COLUMN password VARCHAR(255) NOT NULL;

-- 이메일은 로그인 식별자이므로 회원 간 중복될 수 없다. 중복 가입 경합의 최종 방어선이다.
ALTER TABLE members
    ADD CONSTRAINT uk_email UNIQUE (email);
```

- 학번에는 UNIQUE를 걸지 않는다. 같은 학번으로 여러 계정을 만드는 것을 막지 않기로 했다(결정 사항 참고). 기존 `idx_student_id`는 그대로 둔다.
- `DEFAULT` 열거값이 남은 행을 UPDATE로 옮기는 처리는 필요 없다. `DELETE FROM members`가 선행하므로 미정 값을 가진 행 자체가 사라진다.
- 컬럼 순서는 `ALTER TABLE`이 끝에 붙이므로 `created_at` 뒤에 온다. 순서를 맞추려 `AFTER`를 쓰지 않는다.
- 제약 이름은 `uk_email`로 한다. 기존 유일 제약(V1_5의 `uk_login_id`)이 테이블 접두사 없이 `uk_{컬럼}`으로 짓고 있어 이를 따른다.

**`Member`** - 필드 2개 추가, 생성 경로 교체.

```java
@Column(nullable = false, name = "email")
private String email;

@Column(nullable = false, name = "password")
private String password;
```

`@Table`에 UNIQUE 제약을 명시해 스키마와 매핑을 맞춘다. `Admin`이 같은 형태다.

```java
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email"})
        }
)
```

`createDefault(studentId)`를 지우고 프로필 전체를 받는 `create(...)`로 바꾼다. `college`는 파라미터로 받지 않고 `department`에서 파생시킨다. `updateDepartment`가 이미 같은 방식이라 두 경로의 결과가 어긋나지 않는다.

```java
public static Member create(
        final String email,
        final String encodedPassword,
        final String studentId,
        final String name,
        final MemberDepartment department,
        final MemberGrade grade,
        final AcademicStatus academicStatus,
        final double lastSemesterGpa
) {
    return Member.builder()
            .email(email)
            .password(encodedPassword)
            .studentId(studentId)
            .name(name)
            .college(department.getMemberCollege())
            .department(department)
            .grade(grade)
            .academicStatus(academicStatus)
            .lastSemesterGpa(lastSemesterGpa)
            .build();
}
```

`Admin.create`가 `encodedPassword`를 받는 것과 같이, 인코딩은 호출부(Service)가 끝내고 넘긴다. 엔티티가 인코더를 알지 않는다.

**열거 4종** - `DEFAULT` 상수 제거.

| 파일 | 지우는 상수 |
|---|---|
| `MemberCollege` | `DEFAULT("미정")` |
| `MemberDepartment` | `DEFAULT(MemberCollege.DEFAULT, "미정")` 과 위 주석 |
| `MemberGrade` | `DEFAULT("미정", 0)` |
| `AcademicStatus` | `DEFAULT("미정")` |

`MemberDepartment.fromSelectable`도 지운다. `from()` 결과에서 `DEFAULT`만 걸러내려고 만든 메서드라 `DEFAULT`가 없으면 `from()`과 동작이 같다.

```java
// 제거
public static MemberDepartment fromSelectable(final String value) {
    final MemberDepartment memberDepartment = from(value);
    if (memberDepartment == DEFAULT) {
        throw new RestApiException(INVALID_DEPARTMENT);
    }
    return memberDepartment;
}
```

`INVALID_DEPARTMENT`의 static import도 함께 지운다. 호출부인 `CourseService:79`와 `MemberService:37`은 `MemberDepartment.from(...)`으로 바꾼다.
`"DEFAULT"` 문자열이 들어오면 이제 알 수 없는 상수라 `from`이 `INVALID_ENUM_TYPE`(8888, 400)을 던진다. 400이라는 결과는 같고 코드만 바뀐다.

### 2. Repository

`MemberRepository` - 조회 기준을 학번에서 이메일로 옮긴다.

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(final String email);

    boolean existsByEmail(final String email);
}
```

- `findByStudentId`는 지운다. 유일한 호출부가 `AuthService.login`이고, 학번에 UNIQUE를 걸지 않기로 해 `Optional` 반환이 중복 행에서 깨진다.
- 파생 쿼리로 충분하므로 `@Query`를 쓰지 않는다.

### 3. Service

**`MemberPasswordEncoder`** (`auth/infra/`) - `AdminPasswordEncoder`와 같은 구조다. `project-structure.md`가 비밀번호 인코딩을 `auth/`에 두라고 정하고 있어 회원용도 여기에 둔다.

```java
@Component
public class MemberPasswordEncoder {

    private static final int LOG_ROUNDS = 10;

    public String encode(final String rawPassword) { ... }

    public boolean matches(
            final String rawPassword,
            final String encodedPassword
    ) { ... }
}
```

`AdminPasswordEncoder`와 본문이 같지만 합치지 않는다. 관리자와 회원의 해싱 강도를 따로 조정할 수 있게 남긴다.

**`AuthService`** - 의존을 갈아끼운다.

```java
private final JwtProvider jwtProvider;
private final MemberPasswordEncoder passwordEncoder;

private final MemberRepository memberRepository;
```

`InuMemberRepository` 필드와 import를 지운다. 이 제거로 `@ConditionalOnProperty(oracle.enabled=true)` 빈을 요구하는 곳이 사라져, `oracle` 설정 없이도 컨텍스트가 뜬다.

`signUp` - 신규.

```java
@Transactional
public AuthTokenResponse signUp(final SignUpRequest request) {
    final MemberDepartment department = MemberDepartment.from(request.department());

    validateCollegeMatchesDepartment(MemberCollege.from(request.college()), department);

    final Member member = Member.create(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.studentId(),
            request.name(),
            department,
            MemberGrade.from(request.grade()),
            AcademicStatus.from(request.academicStatus()),
            request.lastSemesterGpa()
    );

    return jwtProvider.generateAuthToken(saveUniqueEmail(member).getId());
}
```

```java
private void validateCollegeMatchesDepartment(
        final MemberCollege college,
        final MemberDepartment department
) {
    if (department.getMemberCollege() != college) {
        throw new RestApiException(COLLEGE_DEPARTMENT_MISMATCH);
    }
}

private Member saveUniqueEmail(final Member member) {
    if (memberRepository.existsByEmail(member.getEmail())) {
        throw new RestApiException(EMAIL_ALREADY_EXISTS);
    }

    try {
        return memberRepository.saveAndFlush(member);
    } catch (final DataIntegrityViolationException e) {
        throw new RestApiException(EMAIL_ALREADY_EXISTS);
    }
}
```

- 검증 순서: 학과 변환 → 단과대학 정합성 → 이메일 선점 확인 → 저장. 잘못된 열거값이 이메일 중복보다 먼저 걸린다.
- `existsByEmail` 선확인은 경합에 열려 있다. 확인과 저장 사이에 다른 요청이 끼어들 수 있으므로 `uk_email` 위반을 잡아 같은 `EMAIL_ALREADY_EXISTS`로 바꾼다. 선확인만으로는 500이 나가고, 제약만으로는 정상 경로에서도 예외 비용을 내므로 둘 다 둔다.
- `saveAndFlush`를 쓴다. `save`만 하면 제약 위반이 트랜잭션 커밋 시점(서비스 밖)에 터져 `DataIntegrityViolationException`을 여기서 잡을 수 없다.
- `DataIntegrityViolationException` 처리를 `GlobalExceptionHandler`에 두지 않는다. 전역에서는 어느 제약이 깨졌는지 구분할 수 없어 다른 제약 위반까지 이메일 중복으로 응답하게 된다.
- 가입 직후 토큰을 발급해 곧바로 로그인 상태가 되게 한다. 가입하고 다시 로그인을 부르게 하지 않는다.

`checkEmailAvailability` - 신규.

```java
@Transactional(readOnly = true)
public EmailAvailabilityResponse checkEmailAvailability(final String email) {
    return EmailAvailabilityResponse.of(!memberRepository.existsByEmail(email));
}
```

이메일이 이미 있어도 예외를 던지지 않는다. 입력 도중 호출하는 검사이므로 사용 가능 여부를 200 본문으로 돌려준다.

`login` - 교체.

```java
@Transactional(readOnly = true)
public AuthTokenResponse login(final LoginRequest request) {
    final Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

    if (!passwordEncoder.matches(request.password(), member.getPassword()))
        throw new RestApiException(PASSWORD_NOT_MATCH);

    return jwtProvider.generateAuthToken(member.getId());
}
```

- `@Transactional`에서 `@Transactional(readOnly = true)`로 바꾼다. 회원을 자동 생성하던 쓰기가 사라져 조회만 남는다.
- 실패를 `MEMBER_NOT_FOUND`(1010, 404)와 `PASSWORD_NOT_MATCH`(1012, 401)로 나눈 것은 기존 `AuthControllerDocs`와 `auth.md`가 그렇게 적고 있어 유지한다. 관리자 로그인처럼 하나로 합치는 방식(`ADMIN_LOGIN_FAILED`)은 계정 열거를 막으려는 것인데, 이메일 중복 검사 API가 이미 존재 여부를 알려주므로 여기서만 감춰봐야 소용이 없다.
- `reIssue`는 그대로 둔다.

`MemberService.updateDepartment` - `MemberDepartment.fromSelectable(...)`을 `MemberDepartment.from(...)`으로 바꾼다. 다른 로직은 건드리지 않는다.

`CourseService:79` - 같은 교체.

### 4. DTO

**`SignUpRequest`** (`auth/dto/request/`)

```java
public record SignUpRequest(
        @Schema(
                description = "이메일",
                example = "student@inu.ac.kr"
        )
        @NotBlank(message = "이메일이 비어있습니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(
                description = "비밀번호",
                example = "password1234"
        )
        @NotBlank(message = "비밀번호가 비어있습니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @Schema(
                description = "학번",
                example = "202012345"
        )
        @NotBlank(message = "학번이 비어있습니다.")
        @Pattern(regexp = "^[A-Za-z0-9]{1,20}$", message = "학번은 20자 이하의 영문자 또는 숫자여야 합니다.")
        String studentId,

        @Schema(
                description = "이름",
                example = "홍길동"
        )
        @NotBlank(message = "이름이 비어있습니다.")
        String name,

        @Schema(
                description = "단과대학",
                example = "INFORMATION_TECHNOLOGY"
        )
        @NotBlank(message = "단과대학이 비어있습니다.")
        String college,

        @Schema(
                description = "학과(부)",
                example = "COMPUTER_ENGINEERING"
        )
        @NotBlank(message = "학과가 비어있습니다.")
        String department,

        @Schema(
                description = "학년",
                example = "JUNIOR"
        )
        @NotBlank(message = "학년이 비어있습니다.")
        String grade,

        @Schema(
                description = "학적 상태",
                example = "ENROLLED"
        )
        @NotBlank(message = "학적 상태가 비어있습니다.")
        String academicStatus,

        @Schema(
                description = "직전 학기 성적",
                example = "3.5"
        )
        @NotNull(message = "직전 학기 성적이 비어있습니다.")
        @DecimalMin(value = "0.0", message = "직전 학기 성적은 0.0 이상이어야 합니다.")
        @DecimalMax(value = "4.5", message = "직전 학기 성적은 4.5 이하여야 합니다.")
        Double lastSemesterGpa
) {}
```

- 열거값은 enum이 아니라 `String`으로 받는다. enum 직접 바인딩은 `HttpMessageNotReadableException`을 거쳐 응답 규격이 갈리므로, `DepartmentUpdateRequest`가 이미 `String`을 쓰는 것과 맞춘다.
- 비밀번호 제약은 기존 `LoginRequest`의 8~20자를 그대로 가져온다. 가입과 로그인의 규칙이 어긋나면 가입은 되는데 로그인이 막히는 계정이 생긴다.
- `lastSemesterGpa`는 `double`이 아니라 `Double`로 받는다. 기본형이면 값을 빼먹어도 0.0으로 채워져 `@NotNull`이 걸리지 않는다.
- 상한 4.5는 인천대 학점 체계 기준이다. 최대 이수 학점 산정(`Member.getMaxCredit`)이 이 값에 걸려 있어 범위를 넘긴 값이 들어오면 학점 상한이 왜곡된다.

**`EmailAvailabilityResponse`** (`auth/dto/response/`)

```java
@Builder(access = PRIVATE)
public record EmailAvailabilityResponse(
        boolean available
) {
    public static EmailAvailabilityResponse of(final boolean available) {
        return EmailAvailabilityResponse.builder()
                .available(available)
                .build();
    }
}
```

**`LoginRequest`** - `studentId`를 `email`로 교체하고 `@Schema`를 붙인다. `dto.md`는 Request에 `@Schema` + validation을 요구하는데 기존 파일에는 validation만 있었다. 전면 수정하는 김에 컨벤션에 맞춘다.

```java
public record LoginRequest(
        @Schema(
                description = "이메일",
                example = "student@inu.ac.kr"
        )
        @NotBlank(message = "이메일이 비어있습니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(
                description = "비밀번호",
                example = "password1234"
        )
        @NotBlank(message = "비밀번호가 비어있습니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password
) {}
```

**`MemberProfileResponse`** - `email`을 추가한다. 이메일이 회원 정보의 일부가 됐는데 프로필에 없으면 본인 이메일을 확인할 방법이 없다. 비밀번호는 넣지 않는다(`member.md` "응답에 비밀번호는 포함하지 않는다").

```java
public record MemberProfileResponse(
        String email,
        String department,
        String studentId,
        String name,
        String grade,
        String academicStatus
) { ... }
```

**`ExceptionCode`** - 회원 그룹만 손댄다.

```java
// 회원
MEMBER_NOT_FOUND(NOT_FOUND, 1010, "사용자를 찾을 수 없습니다."),
PASSWORD_NOT_MATCH(UNAUTHORIZED, 1012, "비밀번호가 일치하지 않습니다."),
EMAIL_ALREADY_EXISTS(CONFLICT, 1015, "이미 사용 중인 이메일입니다."),
COLLEGE_DEPARTMENT_MISMATCH(BAD_REQUEST, 1016, "학과의 소속 단과대학과 일치하지 않습니다."),
```

- `PORTAL_LOGIN_FAILED`(1013)와 `INVALID_DEPARTMENT`(1014)를 지운다. 둘 다 마지막 호출부가 이번에 사라진다.
- 지운 번호를 재사용하지 않고 1015부터 이어 붙인다. 클라이언트에 이미 나간 코드가 다른 뜻으로 되살아나는 것을 막는다.

### 5. Controller

**`AuthController`** - 엔드포인트 2개 추가.

```java
@PostMapping("/sign-up")
public ResponseEntity<AuthTokenResponse> signUp(@Valid @RequestBody final SignUpRequest request){
    return ResponseEntity.status(CREATED).body(authService.signUp(request));
}

@GetMapping("/email-availability")
public ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(
        @ParamValidation(maxLength = 255)
        @RequestParam("email") final String email
){
    return ResponseEntity.ok(authService.checkEmailAvailability(email));
}
```

- 가입은 회원을 새로 만드므로 201 CREATED로 응답한다.
- 중복 검사는 상태를 바꾸지 않으므로 `GET` + 쿼리 파라미터다.
- **클래스에 `@Validated`를 추가해야 한다.** `@ParamValidation`은 `@Constraint`라 메서드 파라미터 검증이 `@Validated` 없이는 동작하지 않는다. `AuthController`에는 아직 없고, `CourseController`가 이미 붙이고 있다.
- `maxLength`는 `members.email` 컬럼 길이와 같은 255로 맞춘다. 검증 실패는 `ConstraintViolationException`을 거쳐 `INVALID_REQUEST_PARAMETER`(7777, 400)로 나간다.

**`AuthControllerDocs`** - 위 두 메서드의 `@Operation`, `@ApiResponses`를 추가하고, 로그인 문서의 "학번과 비밀번호로 로그인합니다"를 이메일 기준으로 고친다. 가입 문서에는 400(`INVALID_ENUM_TYPE`, `COLLEGE_DEPARTMENT_MISMATCH`)과 409(`EMAIL_ALREADY_EXISTS`)를 넣는다. 작성 규칙은 `.claude/spec/api-docs-convention.md`를 따른다.

**`WhitelistEndpoint`** - 가입과 중복 검사는 토큰 없이 불러야 한다.

```java
new EndPoint("/api/v1/auth/login", HttpMethod.POST),
new EndPoint("/api/v1/auth/sign-up", HttpMethod.POST),
new EndPoint("/api/v1/auth/email-availability", HttpMethod.GET),
new EndPoint("/api/v1/auth/re-issue", HttpMethod.POST),
```

### 6. Oracle 제거

| 대상 | 조치 |
|---|---|
| `member/repository/InuMemberRepository.java` | 파일 삭제 |
| `global/config/OracleConfig.java` | 파일 삭제 |
| `build.gradle:35` | `runtimeOnly 'com.oracle.database.jdbc:ojdbc11'` 줄 삭제 |
| `application-prod.yml:54-60` | `oracle:` 블록 삭제 |
| `application-conc.yml:71-85` | `oracle:` 블록과 위 주석 삭제 |
| `application-conc.yml`의 `management.health.db.enabled: false` | 삭제하고 관련 주석도 제거 |
| `.github/workflows/cd-prod.yml:34-36` | `oracle.datasource.*` 주입 3줄 삭제 |
| `.claude/skills/fix-concurrency/template/application-conc.yml` | 실제 프로파일과 같게 정리 |
| `.claude/skills/fix-concurrency/template/seeds/member.sql` | 새 스키마 반영 (아래 상세) |
| `.claude/skills/fix-concurrency/template/seeds/README.md` | 경고 사유 문구 갱신 |
| `.claude/skills/fix-concurrency/template/mint-tokens.sh` | 헤더 주석 갱신 |

`management.health.db.enabled: false`는 도달 불가능한 더미 Oracle DataSource 때문에 껐던 설정이다. DataSource가 하나만 남으면 헬스 프로브가 정상 동작하므로 되돌린다. 저장소 시크릿(`ORACLE_URL` 등) 폐기는 레포 설정이라 코드 작업 밖이다. 구현 후 사용자에게 보고만 한다.

**fix-concurrency 시드·스크립트** - 스킬 자산이 옛 스키마를 전제하고 있어 그대로 두면 V1_9 이후 동작하지 않는다.

- `seeds/member.sql` - INSERT 컬럼에 `email`, `password`를 추가한다. `email`은 UNIQUE 제약 때문에 행마다 달라야 하므로 `CONCAT('conc', n, '@uss.local')`로 만들고, `password`는 로그인 경로를 쓰지 않으므로 BCrypt가 아닌 고정 더미 문자열을 넣는다. `college`, `department`의 `'DEFAULT'`는 열거값이 사라져 JPA 매핑이 깨지므로 실제 값(`INFORMATION_TECHNOLOGY`, `COMPUTER_ENGINEERING`)으로 바꾸고, "비밀번호 컬럼이 없다(인증을 학교 포털에 위임한다)"와 "DEFAULT로 고정한다" 주석도 함께 고친다.
- `mint-tokens.sh` - 헤더의 "왜 로그인 API를 쓰지 않는가" 사유(포털 위임, 비밀번호 컬럼 없음)가 거짓이 된다. 새 사유로 바꾼다. 시드 비밀번호가 BCrypt 해시가 아니라 로그인 API를 탈 수 없고, 수백 개 계정을 가입 API로 만드는 것보다 시드 id에 맞춰 직접 서명하는 편이 측정 준비에 맞다.
- `seeds/README.md` - "optimize-performance 시드를 가져다 쓰지 마라" 경고는 유지하되, "`email`, `password`, `member_college` 컬럼이 있던 시절"이라는 사유가 낡는다(이번에 `email`, `password`는 되살아난다). 현행 스키마와 컬럼 구성이 다르다는 사유로 다시 쓴다.

### 7. 정책 문서 갱신

**`auth.md`**

- "회원가입" 절: "신규 회원을 만드는 경로가 없다"를 지우고 아래로 바꾼다.
  - 이메일, 비밀번호, 학번, 이름, 단과대학, 학과, 학년, 학적 상태, 직전 학기 성적을 모두 받아 가입한다
  - 이메일이 이미 쓰이고 있으면 가입에 실패한다
  - 학과의 소속 단과대학과 보낸 단과대학이 다르면 가입에 실패한다
  - 가입에 성공하면 액세스 토큰을 함께 발급한다
  - 이메일 사용 가능 여부는 가입 전에 따로 확인할 수 있다
- "로그인" 절: 식별 기준을 학번에서 이메일로 바꾸고, 학번 유일성에 관한 마지막 문단을 아래로 교체한다.
  - 학번은 회원 간 중복될 수 있다. 한 학번으로 여러 계정을 만드는 것을 막지 않는다
- "인증 예외 경로" 절: 회원가입, 이메일 중복 검사를 목록에 추가한다.

**`member.md`**

- "회원 정보" 절: "학교 포털 로그인으로 새로 가입한 회원은..." 문단을 통째로 지운다. 미설정 상태가 없어진다.
- "학과 수정" 절: "`미정`은 선택할 수 없다" 줄을 지운다. 선택지에서 사라진 게 아니라 값 자체가 없어진다.
- "회원 정보" 절에 비밀번호를 추가하고, 이메일이 로그인 식별자임을 밝힌다.

`member.md`는 이미 "학번, 이름, 이메일"과 "이메일은 회원 간 중복될 수 없다"를 적고 있다. 문서가 코드보다 앞서 있던 부분이며, 이번 작업으로 코드가 문서를 따라잡는다.

## 결정 필요 (Decisions needed)

- [x] 기존 `members` 행은 전부 삭제한다 - 포털로 만들어져 비밀번호가 없어 어차피 로그인할 수 없다. `registrations`, `carts`는 CASCADE로 함께 지우고 `courses.current_enrollment`를 0으로 되돌린다
- [x] UNIQUE는 이메일에만 건다 - 학번 중복은 허용한다. 시뮬레이터라 같은 학번으로 여러 번 연습하는 것을 막지 않는다
- [x] 회원가입, 이메일 중복 검사는 auth 도메인에 둔다 - `POST /api/v1/auth/sign-up`, `GET /api/v1/auth/email-availability`
- [x] 단과대학은 요청에서 받고 학과와의 정합성을 검증한다 - 어긋나면 `COLLEGE_DEPARTMENT_MISMATCH`(400)
- [x] 로그인 실패는 `MEMBER_NOT_FOUND`와 `PASSWORD_NOT_MATCH`로 계속 나눈다 - 기존 문서와 정책이 그렇게 적혀 있고, 중복 검사 API가 이미 이메일 존재 여부를 알려주므로 합쳐도 감춰지는 게 없다

## 검증

- `AuthServiceTest` - `InuMemberRepository` 목 의존을 걷어내고 다시 짠다
  - `@Nested class 회원가입할_때`
    - `유효한_요청이면_가입에_성공하고_토큰을_반환한다` - 저장된 회원의 `college`가 학과에서 파생됐는지 함께 검증
    - `비밀번호는_평문으로_저장되지_않는다` - `getPassword()`가 원문과 다르고 `matches`로는 통과하는지 검증
    - `이미_사용_중인_이메일이면_예외를_반환한다` - `EMAIL_ALREADY_EXISTS`
    - `학과와_단과대학이_어긋나면_예외를_반환한다` - `COLLEGE_DEPARTMENT_MISMATCH`
    - `유효하지_않은_학과면_예외를_반환한다` - `INVALID_ENUM_TYPE`
  - `@Nested class 이메일_중복을_검사할_때`
    - `쓰이지_않은_이메일이면_사용_가능으로_응답한다`
    - `이미_쓰이는_이메일이면_사용_불가로_응답한다`
  - `@Nested class 로그인할_때`
    - `이메일과_비밀번호가_맞으면_토큰을_반환한다`
    - `없는_이메일이면_예외를_반환한다` - `MEMBER_NOT_FOUND`
    - `비밀번호가_틀리면_예외를_반환한다` - `PASSWORD_NOT_MATCH`
    - 기존 `로그인하면_회원이_자동_생성된다` 계열 시나리오는 삭제한다. 자동 생성 경로가 없어진다
  - `reIssue` 시나리오는 그대로 둔다
- `LoginRequestTest` - 학번 패턴 검증을 이메일 형식 검증으로 교체
- `MemberFixture.createMember` - `email`, `password` 파라미터를 추가한다. 인자 없는 오버로드는 **호출마다 유일한 이메일**(정적 카운터 기반)을 생성해야 한다. `@Table`의 email UNIQUE가 H2 `ddl-auto: create-drop`으로 테스트 스키마에 실제로 생기는데, `RegistrationServiceTest:60-61, 695-696`과 `CartServiceTest:56-57, 225-226`이 한 트랜잭션 안에서 `createMember()` 회원 2명을 `saveAll`하므로 고정 이메일이면 두 번째 flush에서 제약 위반으로 깨진다. 유일 이메일을 생성하면 두 테스트 파일은 수정 없이 통과한다. 비밀번호는 인코딩된 고정 값으로 충분하다
- `MemberServiceTest` - `미정은_학과로_선택할_수_없다`(166)는 기대 코드를 바꿔 남기지 않고 **삭제한다**. `DEFAULT` 상수가 사라지면 `"DEFAULT"`는 그냥 알 수 없는 문자열이라, 바로 아래 `유효하지_않은_학과가_들어오면_예외를_반환한다`(177)와 같은 경로(`INVALID_ENUM_TYPE`)를 검증하는 중복이 된다. `사용자의_학과를_수정할_때`의 setUp(99-100, 114-115)이 만드는 미정 회원은 실제 값으로 바꾸되, 수정 전 학과는 `VALID_DEPARTMENT`(COMPUTER_ENGINEERING)와 다른 학과로 둬 수정이 실제로 일어나게 한다
- `CourseServiceTest` - `미정으로_조회하면_예외가_발생한다`(741)도 같은 이유로 삭제한다. `잘못된_학과_코드로_조회하면_예외가_발생한다`(719)와 `학과가_아닌_값으로_조회하면_예외가_발생한다`(730)가 이미 `INVALID_ENUM_TYPE` 경로를 덮는다. `학과가_미정인_회원은_예외없이_빈_목록을_받는다`(338)는 미정 회원 픽스처(251-254)를 대응 강의 학과가 없는 실제 소속(예: `INTERNATIONAL_LIBERAL_ARTS`)으로 바꾸고 테스트명도 그에 맞게 고쳐, 빈 목록 분기의 커버리지를 유지한다
- `CourseDepartmentTest:160` - `ownedBy(MemberDepartment.DEFAULT)` 단언 한 줄만 지운다. 나머지 단언(`INTERNATIONAL_LIBERAL_ARTS`, `CONVERGENCE`)이 남아 테스트는 유지된다
- 이메일 UNIQUE 경합은 통합 테스트로 재현하지 않는다. `@IntegrationTest`가 `@Transactional`이라 동시 트랜잭션을 만들 수 없다. `existsByEmail` 선확인 경로만 테스트로 덮고, 제약 위반 경로는 수동으로 확인한다
- 기동 검증: `oracle` 설정이 없는 상태에서 `conc` 프로파일이 뜨는지 확인한다. `InuMemberRepository` 의존이 남아 있으면 여기서 걸린다
- 실행: `./gradlew test`

## Deviation Log

- `src/main/java/uss/code/course/controller/CourseControllerDocs.java`, `src/main/java/uss/code/member/controller/MemberControllerDocs.java`: `INVALID_DEPARTMENT`(1014) `@ExampleObject` 제거 — 이유: 예외 코드가 삭제되어 실제로 내려가지 않는 응답 예시가 된다. `api-docs-convention.md`의 "실제 응답 본문과 같은 형태" 규칙을 지키기 위한 파생 정리로, 계획 영향 범위에 없던 파일이다.
- `src/main/java/uss/code/auth/dto/response/EmailAvailabilityResponse.java`: `available` 필드에 `@Schema` 추가 — 이유: `api-docs-convention.md`가 Response record 필드에도 `@Schema`를 요구한다. 계획 코드 블록에는 없었다.
- `src/test/java/uss/code/auth/service/AuthServiceTest.java`: `아직_만료되지_않은_토큰으로도_재발급에_성공한다`의 유효 토큰을 로그인 경유 대신 `jwtProvider.generateAuthToken`으로 직접 발급 — 이유: 시나리오의 검증 대상은 재발급이고, 기존의 포털 로그인 경유가 사라졌다.
- `src/test/java/uss/code/member/fixture/MemberFixture.java`: 기존 프로필 7개 인자 오버로드를 유지하고 `email`, `password`를 받는 오버로드를 별도로 추가. 7개 인자 오버로드도 호출마다 유일한 이메일을 생성한다 — 이유: `CourseServiceTest`가 한 setUp에서 이 오버로드로 회원 2명을 저장하므로 여기서도 유일 이메일이 필요하고, 기존 호출부 변경을 최소화한다.
