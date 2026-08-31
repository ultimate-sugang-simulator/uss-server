package uss.code.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.cart.domain.Cart;
import uss.code.cart.dto.response.CartedCourseResponse;
import uss.code.cart.dto.response.CartedCoursesResponse;
import uss.code.cart.fixture.CartFixture;
import uss.code.cart.repository.CartRepository;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseClassification;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseGrade;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseType;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.Member;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.*;

@IntegrationTest
class CartServiceTest {

    @Autowired
    private CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CourseRepository courseRepository;

    @Nested
    class 장바구니_조회_테스트 {

        private Long testMemberId;
        private Long otherMemberId;

        @BeforeEach
        void setUp() {
            // 회원 생성
            final Member testMember = MemberFixture.createMember();
            final Member otherMember = MemberFixture.createMember();
            memberRepository.saveAll(List.of(testMember, otherMember));
            testMemberId = testMember.getId();
            otherMemberId = otherMember.getId();

            // 과목 생성
            Course course1 = CourseFixture.createCourseWithDetails(
                    "자료구조", "Data Structure", "CSE101", "CSE101001",
                    CourseGrade.SOPHOMORE
            );
            Course course2 = CourseFixture.createCourseWithDetails(
                    "알고리즘", "Algorithm", "CSE201", "CSE201001",
                    CourseGrade.SOPHOMORE
            );
            Course course3 = CourseFixture.createCourseWithDetails(
                    "데이터베이스", "Database", "CSE301", "CSE301001",
                    CourseGrade.JUNIOR
            );

            // 스케줄 추가
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    course1, CourseDay.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    course1, CourseDay.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
                    course2, CourseDay.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );

            course1.addCourseSchedule(schedule1);
            course1.addCourseSchedule(schedule2);
            course2.addCourseSchedule(schedule3);

            courseRepository.saveAll(List.of(course1, course2, course3));

            // 장바구니 생성
            // testMember: course1, course2, course3
            Cart cart1 = CartFixture.createCart(testMember, course1, LocalDateTime.now().minusDays(3));
            Cart cart2 = CartFixture.createCart(testMember, course2, LocalDateTime.now().minusDays(2));
            Cart cart3 = CartFixture.createCart(testMember, course3, LocalDateTime.now().minusDays(1));

            // otherMember: course1, course2 (같은 과목)
            Cart cart4 = CartFixture.createCart(otherMember, course1);
            Cart cart5 = CartFixture.createCart(otherMember, course2);

            cartRepository.saveAll(List.of(cart1, cart2, cart3, cart4, cart5));
        }

        @Test
        void 회원의_장바구니를_조회하면_성공한다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            assertThat(response.cartedCourseResponses()).hasSize(3);
        }

        @Test
        void 장바구니_담은_순서대로_정렬되어_조회된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            assertThat(response.cartedCourseResponses())
                    .extracting(CartedCourseResponse::courseCode)
                    .containsExactly("CSE101", "CSE201", "CSE301");
        }

        @Test
        void 과목별_cartCount가_올바르게_조회된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            // CSE101: 2명 (testMember, otherMember)
            final CartedCourseResponse course1 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course1.cartCount()).isEqualTo(2);

            // CSE201: 2명
            final CartedCourseResponse course2 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course2.cartCount()).isEqualTo(2);

            // CSE301: 1명 (testMember만)
            final CartedCourseResponse course3 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE301"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course3.cartCount()).isEqualTo(1);
        }

        @Test
        void 스케줄이_있는_과목은_요일순으로_정렬되어_반환된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            // CSE101: [07-401:월(1-2A),수(1-2A)]
            final CartedCourseResponse course1 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course1.schedule()).isEqualTo("[07-401:월(1-2A),수(1-2A)]");

            // CSE201: [07-401:화(1-2A)]
            final CartedCourseResponse course2 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course2.schedule()).isEqualTo("[07-401:화(1-2A)]");
        }

        @Test
        void 스케줄이_없는_과목은_하이픈으로_반환된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            final CartedCourseResponse course3 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE301"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course3.schedule()).isEqualTo("-");
        }


        @Test
        void 정원에_여유가_있는_과목은_신청_가능으로_조회된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            assertThat(response.cartedCourseResponses())
                    .extracting(CartedCourseResponse::isRegisterable)
                    .containsOnly(true);
        }

        @Test
        void 정원이_마감된_과목은_신청_불가로_조회된다() {
            //given
            final Member testMember = memberRepository.findById(testMemberId).orElseThrow();

            // 정원이 가득 찬 과목 생성 (maxCapacity: 2, currentEnrollment: 2)
            final Course fullCourse = CourseFixture.createCourse(
                    "정원마감과목", "Full Course", "CSE999", "CSE999001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 2, 2
            );
            courseRepository.save(fullCourse);
            cartRepository.save(CartFixture.createCart(testMember, fullCourse));

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            final CartedCourseResponse fullCourseResponse = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE999"))
                    .findFirst()
                    .orElseThrow();
            assertThat(fullCourseResponse.isRegisterable()).isFalse();
        }

        @Test
        void 폐강된_과목은_정원에_여유가_있어도_신청_불가로_조회된다() {
            //given
            final Member testMember = memberRepository.findById(testMemberId).orElseThrow();

            final Course closedCourse = CourseFixture.createCourseWithDetails(
                    "폐강과목", "Closed Course", "CSE888", "CSE888001",
                    CourseGrade.SOPHOMORE
            );
            closedCourse.close();
            courseRepository.save(closedCourse);
            cartRepository.save(CartFixture.createCart(testMember, closedCourse));

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(testMemberId);

            //then
            final CartedCourseResponse closedCourseResponse = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE888"))
                    .findFirst()
                    .orElseThrow();
            assertThat(closedCourseResponse.isRegisterable()).isFalse();
        }

        @Test
        void 빈_장바구니를_조회하면_빈_리스트가_반환된다() {
            //given
            final Member emptyMember = MemberFixture.createMember();
            memberRepository.save(emptyMember);

            //when
            final CartedCoursesResponse response = cartService.getCartedCourse(emptyMember.getId());

            //then
            assertThat(response.cartedCourseResponses()).isEmpty();
        }
    }

    @Nested
    class 장바구니_삭제_테스트 {

        private Long testMemberId;
        private Long otherMemberId;
        private Long course1Id;
        private Long course2Id;
        private Long course3Id;

        @BeforeEach
        void setUp() {
            // 회원 생성
            final Member testMember = MemberFixture.createMember();
            final Member otherMember = MemberFixture.createMember();
            memberRepository.saveAll(List.of(testMember, otherMember));
            testMemberId = testMember.getId();
            otherMemberId = otherMember.getId();

            // 과목 생성
            Course course1 = CourseFixture.createCourseWithDetails(
                    "자료구조", "Data Structure", "CSE101", "CSE101001",
                    CourseGrade.SOPHOMORE
            );
            Course course2 = CourseFixture.createCourseWithDetails(
                    "알고리즘", "Algorithm", "CSE201", "CSE201001",
                    CourseGrade.SOPHOMORE
            );
            Course course3 = CourseFixture.createCourseWithDetails(
                    "데이터베이스", "Database", "CSE301", "CSE301001",
                    CourseGrade.JUNIOR
            );

            courseRepository.saveAll(List.of(course1, course2, course3));
            course1Id = course1.getId();
            course2Id = course2.getId();
            course3Id = course3.getId();

            // 장바구니 생성
            // testMember: course1, course2
            Cart cart1 = CartFixture.createCart(testMember, course1);
            Cart cart2 = CartFixture.createCart(testMember, course2);

            // otherMember: course3 (testMember와 겹치지 않음)
            Cart cart3 = CartFixture.createCart(otherMember, course3);

            cartRepository.saveAll(List.of(cart1, cart2, cart3));
        }

        @Test
        void 장바구니에서_과목을_삭제하면_성공한다() {
            //given

            //when
            cartService.deleteCartedCourse(testMemberId, course1Id);

            //then
            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).hasSize(1);
            assertThat(carts)
                    .extracting(cart -> cart.getCourse().getId())
                    .containsExactly(course2Id);
        }

        @Test
        void 장바구니에서_과목_삭제_후_다른_회원의_장바구니는_영향받지_않는다() {
            //given

            //when
            cartService.deleteCartedCourse(testMemberId, course1Id);

            //then
            // otherMember의 장바구니는 그대로 (course3)
            final List<Cart> otherCarts = cartRepository.findByMemberId(otherMemberId);
            assertThat(otherCarts).hasSize(1);
            assertThat(otherCarts.get(0).getCourse().getId()).isEqualTo(course3Id);
        }

        @Test
        void 존재하지_않는_장바구니_항목을_삭제하면_예외가_발생한다() {
            //given
            final Long nonExistentCourseId = 99999L;

            //when & then
            assertThatThrownBy(() -> cartService.deleteCartedCourse(testMemberId, nonExistentCourseId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", CARTED_COURSE_NOT_FOUND);
        }

        @Test
        void 다른_회원의_장바구니_항목을_삭제하면_예외가_발생한다() {
            //given
            // course3은 otherMember만 장바구니에 담고 있음

            //when & then
            // testMember가 otherMember의 장바구니 항목(course3) 삭제 시도
            assertThatThrownBy(() -> cartService.deleteCartedCourse(testMemberId, course3Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", CARTED_COURSE_NOT_FOUND);

            // otherMember의 장바구니는 그대로
            final List<Cart> otherCarts = cartRepository.findByMemberId(otherMemberId);
            assertThat(otherCarts).hasSize(1);
            assertThat(otherCarts.get(0).getCourse().getId()).isEqualTo(course3Id);
        }

        @Test
        void 모든_장바구니_항목을_삭제할_수_있다() {
            //given

            //when
            cartService.deleteCartedCourse(testMemberId, course1Id);
            cartService.deleteCartedCourse(testMemberId, course2Id);

            //then
            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).isEmpty();
        }
    }

    @Nested
    class 장바구니_추가_테스트 {

        private Long testMemberId;
        private Long course1Id;
        private Long course2Id;

        @BeforeEach
        void setUp() {
            // 회원 생성
            final Member testMember = MemberFixture.createMember();
            memberRepository.save(testMember);
            testMemberId = testMember.getId();

            // 과목 생성
            Course course1 = CourseFixture.createCourseWithDetails(
                    "자료구조", "Data Structure", "CSE101", "CSE101001",
                    CourseGrade.SOPHOMORE
            );
            Course course2 = CourseFixture.createCourseWithDetails(
                    "알고리즘", "Algorithm", "CSE201", "CSE201001",
                    CourseGrade.SOPHOMORE
            );

            // 스케줄 추가
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    course1, CourseDay.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            course1.addCourseSchedule(schedule1);

            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    course2, CourseDay.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            course2.addCourseSchedule(schedule2);

            courseRepository.saveAll(List.of(course1, course2));
            course1Id = course1.getId();
            course2Id = course2.getId();
        }

        @Test
        void 장바구니에_과목을_추가하면_성공한다() {
            //given

            //when
            cartService.addCart(testMemberId, course1Id);

            //then
            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).hasSize(1);
            assertThat(carts.get(0).getCourse().getId()).isEqualTo(course1Id);
        }

        @Test
        void 장바구니가_10개_미만이면_추가할_수_있다() {
            //given
            // 9개의 과목을 미리 추가
            for (int i = 0; i < 9; i++) {
                Course course = CourseFixture.createCourseWithDetails(
                        "과목" + i, "Course" + i, "CSE30" + i, "CSE30" + i + "001",
                        CourseGrade.SOPHOMORE
                );
                courseRepository.save(course);

                Member member = memberRepository.findById(testMemberId).orElseThrow();
                Cart cart = CartFixture.createCart(member, course);
                cartRepository.save(cart);
            }

            //when & then
            // 10번째 추가는 성공해야 함
            cartService.addCart(testMemberId, course1Id);

            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).hasSize(10);
        }

        @Test
        void 장바구니가_10개_이상이면_추가할_수_없다() {
            //given
            // 10개의 과목을 미리 추가
            for (int i = 0; i < 10; i++) {
                Course course = CourseFixture.createCourseWithDetails(
                        "과목" + i, "Course" + i, "CSE30" + i, "CSE30" + i + "001",
                        CourseGrade.SOPHOMORE
                );
                courseRepository.save(course);

                Member member = memberRepository.findById(testMemberId).orElseThrow();
                Cart cart = CartFixture.createCart(member, course);
                cartRepository.save(cart);
            }

            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, course1Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", CARTED_COURSE_LIMIT_EXCEEDED);
        }

        @Test
        void 이미_장바구니에_담긴_과목을_다시_추가하면_예외가_발생한다() {
            //given
            cartService.addCart(testMemberId, course1Id);

            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, course1Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_ALREADY_IN_CART);
        }

        @Test
        void 존재하지_않는_회원이_장바구니에_추가하면_예외가_발생한다() {
            //given
            final Long nonExistentMemberId = 99999L;

            //when & then
            assertThatThrownBy(() -> cartService.addCart(nonExistentMemberId, course1Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }

        @Test
        void 존재하지_않는_과목을_장바구니에_추가하면_예외가_발생한다() {
            //given
            final Long nonExistentCourseId = 99999L;

            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, nonExistentCourseId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_NOT_FOUND);
        }

        @Test
        void 시간표가_겹치는_과목을_추가하면_예외가_발생한다() {
            //given
            // course1을 먼저 추가 (월 13:00-15:00)
            cartService.addCart(testMemberId, course1Id);

            // 겹치는 시간대의 새 과목 생성 (월 14:00-16:00)
            Course conflictCourse = CourseFixture.createCourseWithDetails(
                    "운영체제", "Operating System", "CSE301", "CSE301001",
                    CourseGrade.JUNIOR
            );
            CourseSchedule conflictSchedule = CourseScheduleFixture.createCourseSchedule(
                    conflictCourse, CourseDay.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)
            );
            conflictCourse.addCourseSchedule(conflictSchedule);
            courseRepository.save(conflictCourse);

            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, conflictCourse.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_SCHEDULE_CONFLICT);
        }

        @Test
        void 시간표가_겹치지_않으면_추가할_수_있다() {
            //given
            // course1을 먼저 추가 (월 13:00-15:00)
            cartService.addCart(testMemberId, course1Id);

            // 겹치지 않는 시간대의 새 과목 생성 (월 15:00-17:00)
            Course nonConflictCourse = CourseFixture.createCourseWithDetails(
                    "운영체제", "Operating System", "CSE301", "CSE301001",
                    CourseGrade.JUNIOR
            );
            CourseSchedule nonConflictSchedule = CourseScheduleFixture.createCourseSchedule(
                    nonConflictCourse, CourseDay.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0)
            );
            nonConflictCourse.addCourseSchedule(nonConflictSchedule);
            courseRepository.save(nonConflictCourse);

            //when
            cartService.addCart(testMemberId, nonConflictCourse.getId());

            //then
            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).hasSize(2);
        }

        @Test
        void OCU_과목이_2개_있으면_추가할_수_없다() {
            //given
            // OCU 과목 2개 생성 및 추가
            Course ocu1 = CourseFixture.createCourse(
                    "OCU과목1", "OCU Course 1", "OCU001", "OCU001001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course ocu2 = CourseFixture.createCourse(
                    "OCU과목2", "OCU Course 2", "OCU002", "OCU002001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course ocu3 = CourseFixture.createCourse(
                    "OCU과목3", "OCU Course 3", "OCU003", "OCU003001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(ocu1, ocu2, ocu3));

            Member member = memberRepository.findById(testMemberId).orElseThrow();
            Cart cart1 = CartFixture.createCart(member, ocu1);
            Cart cart2 = CartFixture.createCart(member, ocu2);
            cartRepository.saveAll(List.of(cart1, cart2));

            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, ocu3.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_TYPE_LIMIT_EXCEEDED);
        }

        @Test
        void OCU_과목이_1개_있으면_추가할_수_있다() {
            //given
            // OCU 과목 1개 생성 및 추가
            Course ocu1 = CourseFixture.createCourse(
                    "OCU과목1", "OCU Course 1", "OCU001", "OCU001001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course ocu2 = CourseFixture.createCourse(
                    "OCU과목2", "OCU Course 2", "OCU002", "OCU002001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(ocu1, ocu2));

            Member member = memberRepository.findById(testMemberId).orElseThrow();
            Cart cart1 = CartFixture.createCart(member, ocu1);
            cartRepository.save(cart1);

            //when
            cartService.addCart(testMemberId, ocu2.getId());

            //then
            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).hasSize(2);
        }

        @Test
        void K_MOOC_과목이_1개_있으면_추가할_수_없다() {
            //given
            // K-MOOC 과목 1개 생성 및 추가
            Course kMooc1 = CourseFixture.createCourse(
                    "K-MOOC과목1", "K-MOOC Course 1", "KMOOC001", "KMOOC001001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.K_MOOC,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course kMooc2 = CourseFixture.createCourse(
                    "K-MOOC과목2", "K-MOOC Course 2", "KMOOC002", "KMOOC002001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseClassification.MAJOR_CORE,
                    CourseFixture.createCourse().getArea(),
                    CourseType.K_MOOC,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(kMooc1, kMooc2));

            Member member = memberRepository.findById(testMemberId).orElseThrow();
            Cart cart1 = CartFixture.createCart(member, kMooc1);
            cartRepository.save(cart1);

            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, kMooc2.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_TYPE_LIMIT_EXCEEDED);
        }

        @Test
        void 일반_과목은_타입_제한이_없다() {
            //given
            // 일반 과목 여러 개 추가
            for (int i = 0; i < 5; i++) {
                Course course = CourseFixture.createCourseWithDetails(
                        "과목" + i, "Course" + i, "CSE30" + i, "CSE30" + i + "001",
                        CourseGrade.SOPHOMORE
                );
                courseRepository.save(course);

                Member member = memberRepository.findById(testMemberId).orElseThrow();
                Cart cart = CartFixture.createCart(member, course);
                cartRepository.save(cart);
            }

            //when
            cartService.addCart(testMemberId, course1Id);

            //then
            final List<Cart> carts = cartRepository.findByMemberId(testMemberId);
            assertThat(carts).hasSize(6);
        }
    }

    @Nested
    class 폐강_강의_담기_테스트 {

        private Long testMemberId;
        private Long closedCourseId;

        @BeforeEach
        void setUp() {
            final Member member = MemberFixture.createMember();
            memberRepository.save(member);
            testMemberId = member.getId();

            final Course closedCourse = CourseFixture.createCourse();
            closedCourse.close();
            courseRepository.save(closedCourse);
            closedCourseId = closedCourse.getId();
        }

        @Test
        void 폐강된_강의는_담을_수_없다() {
            //when & then
            assertThatThrownBy(() -> cartService.addCart(testMemberId, closedCourseId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_CLOSED);
        }

        @Test
        void 폐강된_강의는_장바구니에_담기지_않는다() {
            //when
            assertThatThrownBy(() -> cartService.addCart(testMemberId, closedCourseId))
                    .isInstanceOf(RestApiException.class);

            //then
            assertThat(cartRepository.findByMemberId(testMemberId)).isEmpty();
        }

        @Test
        void 이미_담은_강의가_폐강돼도_삭제할_수_있다() {
            //given
            final Course course = CourseFixture.createCourseWithDetails(
                    "운영체제", "Operating System", "CSE3010", "CSE3010001", CourseGrade.JUNIOR
            );
            courseRepository.save(course);
            cartService.addCart(testMemberId, course.getId());

            course.close();
            courseRepository.save(course);

            //when
            cartService.deleteCartedCourse(testMemberId, course.getId());

            //then
            assertThat(cartRepository.findByMemberIdAndCourseId(testMemberId, course.getId())).isEmpty();
        }
    }

}
