package uss.code.registration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import uss.code.registration.domain.Registration;
import uss.code.registration.dto.response.RegistrationCourseResponse;
import uss.code.registration.dto.response.RegistrationCoursesResponse;
import uss.code.registration.fixture.RegistrationFixture;
import uss.code.registration.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class RegistrationServiceTest {

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CourseRepository courseRepository;

    @Nested
    class 수강신청_과목_조회_테스트 {

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

            // 수강신청 생성
            // testMember: course1, course2, course3
            Registration registration1 = RegistrationFixture.createRegistration(
                    testMember, course1, LocalDateTime.now().minusDays(3)
            );
            Registration registration2 = RegistrationFixture.createRegistration(
                    testMember, course2, LocalDateTime.now().minusDays(2)
            );
            Registration registration3 = RegistrationFixture.createRegistration(
                    testMember, course3, LocalDateTime.now().minusDays(1)
            );

            // otherMember: course1, course2 (같은 과목)
            Registration registration4 = RegistrationFixture.createRegistration(otherMember, course1);
            Registration registration5 = RegistrationFixture.createRegistration(otherMember, course2);

            registrationRepository.saveAll(List.of(
                    registration1, registration2, registration3, registration4, registration5
            ));
        }

        @Test
        void 회원의_수강신청_과목을_조회하면_성공한다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            assertThat(response.registrationCourseResponses()).hasSize(3);
        }

        @Test
        void 수강신청한_과목이_올바르게_조회된다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            assertThat(response.registrationCourseResponses())
                    .extracting(RegistrationCourseResponse::courseCode)
                    .containsExactlyInAnyOrder("CSE101", "CSE201", "CSE301");
        }

        @Test
        void 다른_회원의_수강신청은_조회되지_않는다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            // testMember는 3개의 과목만 조회되어야 함
            assertThat(response.registrationCourseResponses()).hasSize(3);

            // otherMember 조회
            final RegistrationCoursesResponse otherResponse = registrationService.getRegistrationCourse(otherMemberId);
            assertThat(otherResponse.registrationCourseResponses()).hasSize(2);
            assertThat(otherResponse.registrationCourseResponses())
                    .extracting(RegistrationCourseResponse::courseCode)
                    .containsExactlyInAnyOrder("CSE101", "CSE201");
        }

        @Test
        void 스케줄이_있는_과목은_요일순으로_정렬되어_반환된다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            // CSE101: 월3,4 수3,4
            final RegistrationCourseResponse course1 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course1.schedule()).isEqualTo("월3,4 수3,4");

            // CSE201: 화1,2
            final RegistrationCourseResponse course2 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course2.schedule()).isEqualTo("화1,2");
        }

        @Test
        void 스케줄이_없는_과목은_하이픈으로_반환된다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            final RegistrationCourseResponse course3 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE301"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course3.schedule()).isEqualTo("-");
        }

        @Test
        void null값인_강의실은_하이픈으로_반환된다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            final RegistrationCourseResponse course3 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE301"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course3.classroom()).isEqualTo("-");
        }

        @Test
        void 과목_정보가_올바르게_매핑된다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            final RegistrationCourseResponse course1 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();

            assertThat(course1.courseTitleKr()).isEqualTo("자료구조");
            assertThat(course1.courseTitleEn()).isEqualTo("Data Structure");
            assertThat(course1.classroom()).isEqualTo("공학관101");
        }

        @Test
        void 수강신청_내역이_없으면_빈_리스트가_반환된다() {
            //given
            final Member emptyMember = MemberFixture.createMember();
            memberRepository.save(emptyMember);

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(emptyMember.getId());

            //then
            assertThat(response.registrationCourseResponses()).isEmpty();
        }
    }
}