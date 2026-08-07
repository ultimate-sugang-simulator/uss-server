# [PLAN-76] 회원 학과 정보 수정 API 추가

> 이슈: #76
> 브랜치: feat/76-member-department-update

## 목표

포털 로그인으로 가입한 회원은 학과가 `DEFAULT("미정")`로 남아 전공 과목 조회를 쓸 수 없다.
회원이 본인의 학과를 직접 수정하는 `PATCH /api/v1/members/department`를 추가한다.

## 영향 범위

### 신규 파일

- `src/main/java/uss/code/member/dto/request/DepartmentUpdateRequest.java` - 학과 수정 요청 본문
- `src/test/java/uss/code/member/service/MemberServiceTest.java`의 신규 `@Nested` (파일은 기존, 아래 수정 파일 참고)

### 수정 파일

- `src/main/java/uss/code/member/domain/Member.java` - `updateDepartment` 메서드 추가
- `src/main/java/uss/code/member/service/MemberService.java` - `updateDepartment` 추가
- `src/main/java/uss/code/member/controller/MemberController.java` - `PATCH /department` 추가
- `src/main/java/uss/code/member/controller/MemberControllerDocs.java` - Swagger 문서 추가
- `src/test/java/uss/code/member/service/MemberServiceTest.java` - 학과 수정 시나리오 `@Nested` 추가
- `.claude/spec/service-policy/member.md` - 학과가 가입 이후에도 변경 가능하다는 정책 반영 (아래 "정책 변경" 참고)

## 구현 계획

1. **Entity / Flyway**: DB 스키마 변경 없음. `members.department`는 이미 `VARCHAR` + `@Enumerated(STRING)`이며 컬럼 추가/변경이 없으므로 마이그레이션 파일을 만들지 않는다.

   `Member`에 상태 변경 메서드를 추가한다. 필드 직접 노출(setter) 대신 도메인 메서드로 감싼다.
   ```java
   public void updateDepartment(final MemberDepartment department) {
       this.department = department;
       this.updatedAt = LocalDateTime.now();
   }
   ```
   `college`는 갱신하지 않는다 (이슈 #76의 제약사항).

2. **Repository**: 변경 없음. 기존 `MemberRepository.findById`를 그대로 쓴다.

3. **Service**: `MemberService.updateDepartment(final long memberId, final DepartmentUpdateRequest request)` - 반환값 `void`
   ```java
   @Transactional
   public void updateDepartment(
           final long memberId,
           final DepartmentUpdateRequest request
   ) {
       final Member member = memberRepository.findById(memberId)
               .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

       member.updateDepartment(MemberDepartment.from(request.department()));
   }
   ```
   - 조회 메서드가 아니므로 `@Transactional`(readOnly 아님)을 붙인다. 더티 체킹으로 반영되므로 `save` 호출은 하지 않는다.
   - 유효하지 않은 학과 문자열은 기존 `MemberDepartment.from`이 `INVALID_ENUM_TYPE`(8888, 400)을 던진다. 별도 검증 로직을 추가하지 않는다.
   - 회원 조회 실패는 `MEMBER_NOT_FOUND`(1010, 404).
   - 검증 순서: 회원 조회 → 학과 변환. 존재하지 않는 회원이 잘못된 학과를 보내면 `MEMBER_NOT_FOUND`가 먼저 난다.

4. **DTO**: `DepartmentUpdateRequest` - `record`, 컴포넌트 1개
   ```java
   public record DepartmentUpdateRequest(
           @Schema(description = "변경할 학과", example = "COMPUTER_ENGINEERING")
           @NotBlank(message = "학과가 비어있습니다.")
           String department
   ) {}
   ```
   - `MemberDepartment` enum이 아닌 `String`으로 받는다. enum으로 직접 바인딩하면 Spring이 `HttpMessageNotReadableException`을 던져 프로젝트의 `INVALID_ENUM_TYPE` 응답 규격을 벗어난다.
   - 응답 DTO는 만들지 않는다. 갱신 결과가 필요하면 클라이언트가 기존 `GET /api/v1/members/profile`을 호출한다.

5. **Controller**: `PATCH /api/v1/members/department` → `MemberController.updateDepartment`
   ```java
   @PatchMapping("/department")
   public ResponseEntity<Void> updateDepartment(
           @Auth final long memberId,
           @Valid @RequestBody final DepartmentUpdateRequest request
   ){
       memberService.updateDepartment(memberId, request);
       return ResponseEntity.ok().build();
   }
   ```
   - 성공 응답은 `ResponseEntity.ok().build()`. 본문 없는 변경 성공에 `ok().build()`를 쓰는 `CartController.addCart`, `RegistrationController.registerCourse`와 맞춘다.
   - `MemberControllerDocs`에 동일 시그니처와 `@Operation`, `@ApiResponses`(200 / 400 `INVALID_ENUM_TYPE` / 404 `MEMBER_NOT_FOUND`)를 추가하고, `MemberController`는 이미 `implements MemberControllerDocs`이므로 연결은 추가 작업이 없다.

## 정책 변경

`.claude/spec/service-policy/member.md`의 "회원 정보" 항목은 현재 "모두 가입 시점에 확정되며 값이 없을 수 없다"고 적혀 있으나,
포털 로그인 가입 회원은 학과가 `미정`으로 생성된다(`Member.createDefault`). 문서가 코드보다 뒤처져 있다.
이번 작업으로 학과는 가입 이후 회원이 직접 바꿀 수 있게 되므로 "프로필" 절에 아래 정책을 추가한다.

- 본인의 학과만 수정할 수 있다
- 학과는 정해진 학과 목록 중 하나여야 하며, 목록에 없는 값이면 수정에 실패한다
- 학과를 수정해도 단과대학은 함께 바뀌지 않는다

## 결정 필요 (Decisions needed)

- [x] 수정 범위는 `department`만 - `college`는 건드리지 않는다 (이슈 발의 시 확정)
- [x] 요청 형식은 `PATCH` + `@RequestBody` (이슈 발의 시 확정)

## 검증

- 대상 테스트: `MemberServiceTest`에 `@Nested class 사용자의_학과를_수정할_때` 추가
  - `유효한_학과가_들어오면_수정에_성공한다` - `DEFAULT` 회원의 학과를 `COMPUTER_ENGINEERING`으로 바꾸고, `memberRepository.findById`로 다시 읽어 `getDepartment()`가 바뀌었는지 검증
  - `학과를_수정해도_단과대학은_바뀌지_않는다` - 제약사항이 회귀하지 않도록 `getCollege()`가 그대로인지 검증
  - `유효하지_않은_학과가_들어오면_예외를_반환한다` - `INVALID_ENUM_TYPE` 검증
  - `사용자_아이디가_유효하지_않으면_예외를_반환한다` - `MEMBER_NOT_FOUND` 검증
- `MemberFixture`는 기존 `createMember(...)` 오버로드로 `MemberDepartment.DEFAULT` 회원을 만들 수 있어 픽스처 추가가 필요 없다.
- 실행: `./gradlew test`

## Deviation Log
