# [PLAN-114] 장바구니 조회 응답 신청 가능 여부 추가

> 이슈: #114
> 브랜치: fix/114-cart-registerable

## 목표

장바구니 조회 응답(`CartedCourseResponse`)에 `isRegisterable`을 추가해, 수강신청 화면의 장바구니 탭에서
신청 불가한 강의를 신청 전에 구분할 수 있게 한다. 조회 엔드포인트는 나누지 않고 필드 하나만 늘린다.

## 영향 범위

### 신규 파일

- 없음

### 수정 파일

- `src/main/java/uss/code/cart/dto/response/CartedCourseResponse.java` - `isRegisterable` 컴포넌트 추가와 매핑
- `src/main/java/uss/code/cart/controller/CartControllerDocs.java` - 장바구니 조회 `@Operation` description 보강
- `src/test/java/uss/code/cart/service/CartServiceTest.java` - 조회 응답의 `isRegisterable` 검증 추가
- `.claude/spec/service-policy/cart.md` - 조회 섹션에 신청 가능 여부를 함께 제공한다는 항목 추가 (정책 추가)

## 구현 계획

1. **Entity / Flyway**: 변경 없음. 판정에 쓰는 `maxCapacity`, `currentEnrollment`, `status`는 이미 `course` 테이블에 있다.

2. **Repository**: 변경 없음. `CartRepository.findByMemberId(long memberId)`가 이미 `JOIN FETCH c.course`로
   `Course`를 함께 읽으므로 추가 쿼리가 생기지 않는다.

3. **Service**: 변경 없음. `CartService.getCartedCourse(final long memberId)`는 지금처럼
   `CartedCourseResponse.of(cart.getCourse(), count)`를 호출한다. 판정은 DTO 매핑에서 끝난다.

4. **DTO**: `CartedCourseResponse`
   - record 컴포넌트 맨 끝에 `boolean isRegisterable` 추가.
     `SearchedCourseResponse`, `MajorCourseResponse` 등과 이름과 위치를 맞춘다.
   - `of(final Course course, final Long cartCount)`의 빌더 체인에
     `.isRegisterable(course.isActive() && course.isRegisterable())` 한 줄 추가.
   - 기존 파일은 컴포넌트 사이에 빈 줄이 없고 `@Schema`도 붙어 있지 않다. 신규 필드도 그 서식을 그대로 따른다.
     `dto.md`의 빈 줄 규칙과 `api-docs-convention.md`의 `@Schema` 규칙과는 어긋나지만,
     이번 diff에서 파일 전체 서식을 손대면 한 줄짜리 변경이 서식 변경에 묻힌다. 서식 정리는 별도 작업으로 둔다.

5. **Controller**: 경로와 시그니처 변경 없음 (`GET /api/v1/carts` → `CartController.getCartedCourse`).
   - `CartControllerDocs.getCartedCourse`의 `@Operation` description에
     "각 과목의 신청 가능 여부(`isRegisterable`)를 함께 내려줍니다" 한 문장을 추가한다.

6. **서비스 정책**: `.claude/spec/service-policy/cart.md`의 "조회" 항목에 한 줄 추가.
   - `- 강의마다 신청 가능 여부를 함께 제공한다. 폐강되지 않았고 현재 수강인원이 정원에 도달하지 않았을 때만 신청 가능이다`
   - 담기는 정원을 보지 않는다는 기존 문장은 그대로 둔다. 조회에 필드가 생겨도 담기 규칙은 바뀌지 않는다.

## 결정 필요 (Decisions needed)

- [x] `isRegisterable` 산정 기준 - **B: 폐강까지 반영 (`course.isActive() && course.isRegisterable()`) 채택**

  배경: 장바구니에는 담은 뒤 폐강된 강의가 그대로 남는다. 폐강은 물리 삭제가 아니라 상태 변경이고
  (`course.md` - 장바구니와 수강신청이 강의를 참조하므로), `CartRepository.findByMemberId`에는 상태 필터가 없다.
  반면 전공, 교양, 타학과, 연계전공, 검색 조회에는 폐강 강의가 애초에 나오지 않아 두 식의 결과가 같다.

  - A(정원만)는 다른 응답과 계산식이 완전히 같다. 대신 폐강된 강의라도 정원에 여유가 있으면 `true`로 내려가고,
    사용자는 신청을 눌러 `COURSE_CLOSED`를 받아야 신청 불가를 안다. 이슈가 없애려던 상황이 폐강 건에 그대로 남는다.
  - B는 "신청하면 성공하는가"라는 의미를 응답 사이에서 일치시킨다. 신청 검증 순서가 폐강 다음 정원인 점
    (`registration.md`)과도 맞는다. 계산식은 장바구니에서만 다르지만, 폐강이 걸러진 목록에서는 A와 결과가 같다.

## 검증

- 대상 테스트: `CartServiceTest`의 `장바구니_조회_테스트`
  - `정원에_여유가_있는_과목은_신청_가능으로_조회된다` - 기존 setUp 과목(정원 50, 현재 30)으로 `isRegisterable`이 true
  - `정원이_마감된_과목은_신청_불가로_조회된다` - `CourseFixture.createCourse(...)` 전체 오버로드로
    maxCapacity 2, currentEnrollment 2 강의를 만들어 장바구니에 담고 false 검증
    (`RegistrationServiceTest.수강_정원이_마감된_과목은_신청할_수_없다`와 같은 생성 방식)
  - `폐강된_과목은_신청_불가로_조회된다` - 정원에 여유가 있는 강의를 `close()` 후 저장해 false 검증
- 회귀 확인: `CourseResponseTest`가 `CartedCourseResponse.of(course, 3L)`를 쓰지만 시그니처가 그대로라 영향 없다
- 실행: `./gradlew test --tests '*CartServiceTest' --tests '*CourseResponseTest'`

## Deviation Log

> implement 스킬이 구현 중 계획을 벗어난 지점을 여기에 기록한다. (작성 시점엔 비워둔다)
