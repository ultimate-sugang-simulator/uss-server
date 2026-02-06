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
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseGrade;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.Member;
import uss.code.member.fixture.MemberFixture;
import uss.code.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
                    "자료구조", "Data Structure", "CSE101",
                    CourseGrade.SOPHOMORE, "김교수", "공학관101"
            );
            Course course2 = CourseFixture.createCourseWithDetails(
                    "알고리즘", "Algorithm", "CSE201",
                    CourseGrade.SOPHOMORE, "이교수", "공학관201"
            );
            Course course3 = CourseFixture.createCourseWithDetails(
                    "데이터베이스", "Database", "CSE301",
                    CourseGrade.JUNIOR, null, null
            );

            // 스케줄 추가
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    course1, "월3,4", CourseDay.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    course1, "수3,4", CourseDay.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
                    course2, "화1,2", CourseDay.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
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
            final CartedCoursesResponse response = cartService.getCart(testMemberId);

            //then
            assertThat(response.cartedCourseResponses()).hasSize(3);
        }

        @Test
        void 장바구니_담은_순서대로_정렬되어_조회된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCart(testMemberId);

            //then
            assertThat(response.cartedCourseResponses())
                    .extracting(CartedCourseResponse::courseCode)
                    .containsExactly("CSE101", "CSE201", "CSE301");
        }

        @Test
        void 과목별_cartCount가_올바르게_조회된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCart(testMemberId);

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
            final CartedCoursesResponse response = cartService.getCart(testMemberId);

            //then
            // CSE101: 월3,4 수3,4
            final CartedCourseResponse course1 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course1.schedule()).isEqualTo("월3,4 수3,4");

            // CSE201: 화1,2
            final CartedCourseResponse course2 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course2.schedule()).isEqualTo("화1,2");
        }

        @Test
        void 스케줄이_없는_과목은_하이픈으로_반환된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCart(testMemberId);

            //then
            final CartedCourseResponse course3 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE301"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course3.schedule()).isEqualTo("-");
        }

        @Test
        void null값인_교수명과_강의실은_하이픈으로_반환된다() {
            //given

            //when
            final CartedCoursesResponse response = cartService.getCart(testMemberId);

            //then
            final CartedCourseResponse course3 = response.cartedCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE301"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course3.professor()).isEqualTo("-");
            assertThat(course3.classroom()).isEqualTo("-");
        }

        @Test
        void 빈_장바구니를_조회하면_빈_리스트가_반환된다() {
            //given
            final Member emptyMember = MemberFixture.createMember();
            memberRepository.save(emptyMember);

            //when
            final CartedCoursesResponse response = cartService.getCart(emptyMember.getId());

            //then
            assertThat(response.cartedCourseResponses()).isEmpty();
        }
    }
}
