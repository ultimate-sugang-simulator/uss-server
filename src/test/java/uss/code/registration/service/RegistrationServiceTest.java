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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.*;

import uss.code.course.domain.CourseType;
import uss.code.global.exception.domain.RestApiException;

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
            // CSE101: [07-401:월(1-2A),수(1-2A)]
            final RegistrationCourseResponse course1 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course1.schedule()).isEqualTo("[07-401:월(1-2A),수(1-2A)]");

            // CSE201: [07-401:화(1-2A)]
            final RegistrationCourseResponse course2 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(course2.schedule()).isEqualTo("[07-401:화(1-2A)]");
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
        void 과목_정보가_올바르게_매핑된다() {
            //given

            //when
            final RegistrationCoursesResponse response = registrationService.getRegistrationCourse(testMemberId);

            //then
            final RegistrationCourseResponse course1 = response.registrationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("CSE101"))
                    .findFirst()
                    .orElseThrow();

            assertThat(course1.titleKr()).isEqualTo("자료구조");
            assertThat(course1.titleEn()).isEqualTo("Data Structure");
            assertThat(course1.haksuCode()).isEqualTo("CSE101001");
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

    @Nested
    class 수강신청_테스트 {

        private Long testMemberId;
        private Long course1Id;

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
        }

        @Test
        void 수강신청에_성공한다() {
            //given

            //when
            registrationService.registerCourse(testMemberId, course1Id);

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).hasSize(1);
            assertThat(registrations.get(0).getCourse().getId()).isEqualTo(course1Id);
        }

        @Test
        void 존재하지_않는_회원이_수강신청하면_예외가_발생한다() {
            //given
            final Long nonExistentMemberId = 99999L;

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(nonExistentMemberId, course1Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }

        @Test
        void 존재하지_않는_과목을_수강신청하면_예외가_발생한다() {
            //given
            final Long nonExistentCourseId = 99999L;

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, nonExistentCourseId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_NOT_FOUND);
        }

        @Test
        void 이미_신청한_과목을_다시_신청하면_예외가_발생한다() {
            //given
            registrationService.registerCourse(testMemberId, course1Id);

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, course1Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_ALREADY_REGISTERED);
        }

        @Test
        void 시간표가_겹치는_과목을_신청하면_예외가_발생한다() {
            //given
            // course1을 먼저 신청 (월 13:00-15:00)
            registrationService.registerCourse(testMemberId, course1Id);

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
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, conflictCourse.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_SCHEDULE_CONFLICT);
        }

        @Test
        void 시간표가_겹치지_않으면_신청할_수_있다() {
            //given
            // course1을 먼저 신청 (월 13:00-15:00)
            registrationService.registerCourse(testMemberId, course1Id);

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
            registrationService.registerCourse(testMemberId, nonConflictCourse.getId());

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).hasSize(2);
        }

        @Test
        void OCU_과목이_2개_있으면_신청할_수_없다() {
            //given
            // OCU 과목 2개 생성 및 신청
            Course ocu1 = CourseFixture.createCourse(
                    "OCU과목1", "OCU Course 1", "OCU001", "OCU001001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course ocu2 = CourseFixture.createCourse(
                    "OCU과목2", "OCU Course 2", "OCU002", "OCU002001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course ocu3 = CourseFixture.createCourse(
                    "OCU과목3", "OCU Course 3", "OCU003", "OCU003001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(ocu1, ocu2, ocu3));

            Member member = memberRepository.findById(testMemberId).orElseThrow();
            Registration registration1 = RegistrationFixture.createRegistration(member, ocu1);
            Registration registration2 = RegistrationFixture.createRegistration(member, ocu2);
            registrationRepository.saveAll(List.of(registration1, registration2));

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, ocu3.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_TYPE_LIMIT_EXCEEDED);
        }

        @Test
        void OCU_과목이_1개_있으면_신청할_수_있다() {
            //given
            // OCU 과목 1개 생성 및 신청
            Course ocu1 = CourseFixture.createCourse(
                    "OCU과목1", "OCU Course 1", "OCU001", "OCU001001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course ocu2 = CourseFixture.createCourse(
                    "OCU과목2", "OCU Course 2", "OCU002", "OCU002001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.OCU,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(ocu1, ocu2));

            Member member = memberRepository.findById(testMemberId).orElseThrow();
            Registration registration1 = RegistrationFixture.createRegistration(member, ocu1);
            registrationRepository.save(registration1);

            //when
            registrationService.registerCourse(testMemberId, ocu2.getId());

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).hasSize(2);
        }

        @Test
        void K_MOOC_과목이_1개_있으면_신청할_수_없다() {
            //given
            // K-MOOC 과목 1개 생성 및 신청
            Course kMooc1 = CourseFixture.createCourse(
                    "K-MOOC과목1", "K-MOOC Course 1", "KMOOC001", "KMOOC001001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.K_MOOC,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course kMooc2 = CourseFixture.createCourse(
                    "K-MOOC과목2", "K-MOOC Course 2", "KMOOC002", "KMOOC002001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.K_MOOC,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(kMooc1, kMooc2));

            Member member = memberRepository.findById(testMemberId).orElseThrow();
            Registration registration1 = RegistrationFixture.createRegistration(member, kMooc1);
            registrationRepository.save(registration1);

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, kMooc2.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_TYPE_LIMIT_EXCEEDED);
        }

        @Test
        void 일반_과목은_타입_제한이_없다() {
            //given
            // 일반 과목 여러 개 신청
            for (int i = 0; i < 5; i++) {
                Course course = CourseFixture.createCourseWithDetails(
                        "과목" + i, "Course" + i, "CSE30" + i, "CSE30" + i + "001",
                        CourseGrade.SOPHOMORE
                );
                courseRepository.save(course);

                Member member = memberRepository.findById(testMemberId).orElseThrow();
                Registration registration = RegistrationFixture.createRegistration(member, course);
                registrationRepository.save(registration);
            }

            //when
            registrationService.registerCourse(testMemberId, course1Id);

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).hasSize(6);
        }

        @Test
        void 수강_정원이_마감된_과목은_신청할_수_없다() {
            //given
            // 정원이 가득 찬 과목 생성 (maxCapacity: 2, currentEnrollment: 2)
            Course fullCourse = CourseFixture.createCourse(
                    "정원마감과목", "Full Course", "CSE999", "CSE999001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 2, 2
            );
            courseRepository.save(fullCourse);

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, fullCourse.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", COURSE_MAX_CAPACITY_EXCEEDED);
        }

        @Test
        void 학점_제한을_초과하면_신청할_수_없다() {
            //given
            final Member member = memberRepository.findById(testMemberId).orElseThrow();
            final int maxCredit = member.getMaxCredit(); // 기본 GPA 3.5 = 21학점

            // 이미 21학점 신청한 상태로 만들기 (7과목 * 3학점)
            Course course3Credit1 = CourseFixture.createCourse(
                    "과목1", "Course1", "CSE301", "CSE301001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course course3Credit2 = CourseFixture.createCourse(
                    "과목2", "Course2", "CSE302", "CSE302001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course course3Credit3 = CourseFixture.createCourse(
                    "과목3", "Course3", "CSE303", "CSE303001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course course3Credit4 = CourseFixture.createCourse(
                    "과목4", "Course4", "CSE304", "CSE304001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course course3Credit5 = CourseFixture.createCourse(
                    "과목5", "Course5", "CSE305", "CSE305001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course course3Credit6 = CourseFixture.createCourse(
                    "과목6", "Course6", "CSE306", "CSE306001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            Course course3Credit7 = CourseFixture.createCourse(
                    "과목7", "Course7", "CSE307", "CSE307001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );

            courseRepository.saveAll(List.of(
                    course3Credit1, course3Credit2, course3Credit3,
                    course3Credit4, course3Credit5, course3Credit6, course3Credit7
            ));

            Registration reg1 = RegistrationFixture.createRegistration(member, course3Credit1);
            Registration reg2 = RegistrationFixture.createRegistration(member, course3Credit2);
            Registration reg3 = RegistrationFixture.createRegistration(member, course3Credit3);
            Registration reg4 = RegistrationFixture.createRegistration(member, course3Credit4);
            Registration reg5 = RegistrationFixture.createRegistration(member, course3Credit5);
            Registration reg6 = RegistrationFixture.createRegistration(member, course3Credit6);
            Registration reg7 = RegistrationFixture.createRegistration(member, course3Credit7);
            registrationRepository.saveAll(List.of(reg1, reg2, reg3, reg4, reg5, reg6, reg7)); // 총 21학점

            // 3학점 과목 추가 시도 (21 + 3 = 24 > 21)
            Course extraCourse = CourseFixture.createCourse(
                    "추가과목", "Extra Course", "CSE308", "CSE308001",
                    CourseFixture.createCourse().getCollege(),
                    CourseFixture.createCourse().getDepartment(),
                    CourseFixture.createCourse().getClassification(),
                    CourseFixture.createCourse().getArea(),
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 30
            );
            courseRepository.save(extraCourse);

            //when & then
            assertThatThrownBy(() -> registrationService.registerCourse(testMemberId, extraCourse.getId()))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", CREDIT_LIMIT_EXCEEDED);
        }

        @Test
        void 학점_제한_내에서는_신청할_수_있다() {
            //given
            final Member member = memberRepository.findById(testMemberId).orElseThrow();
            final int maxCredit = member.getMaxCredit(); // 기본 GPA 3.5 = 21학점

            // 이미 15학점 신청한 상태로 만들기 (5과목 * 3학점)
            for (int i = 0; i < 5; i++) {
                Course course = CourseFixture.createCourse(
                        "과목" + i, "Course" + i, "CSE30" + i, "CSE30" + i + "001",
                        CourseFixture.createCourse().getCollege(),
                        CourseFixture.createCourse().getDepartment(),
                        CourseFixture.createCourse().getClassification(),
                        CourseFixture.createCourse().getArea(),
                        CourseType.LECTURE,
                        CourseGrade.SOPHOMORE,
                        3, false, 50, 30
                );
                courseRepository.save(course);
                Registration registration = RegistrationFixture.createRegistration(member, course);
                registrationRepository.save(registration);
            }
            // 현재 15학점, 3학점 과목 추가 시 18학점 (21학점 이하)

            //when
            registrationService.registerCourse(testMemberId, course1Id); // 3학점

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).hasSize(6);

            int totalCredits = registrations.stream()
                    .mapToInt(reg -> reg.getCourse().getCredits())
                    .sum();
            assertThat(totalCredits).isEqualTo(18);
            assertThat(totalCredits).isLessThanOrEqualTo(maxCredit);
        }
    }

    @Nested
    class 수강신청_삭제_테스트 {

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

            // 수강신청 생성
            // testMember: course1, course2
            Registration registration1 = RegistrationFixture.createRegistration(testMember, course1);
            Registration registration2 = RegistrationFixture.createRegistration(testMember, course2);

            // otherMember: course3 (testMember와 겹치지 않음)
            Registration registration3 = RegistrationFixture.createRegistration(otherMember, course3);

            registrationRepository.saveAll(List.of(registration1, registration2, registration3));
        }

        @Test
        void 수강신청_내역에서_과목을_삭제하면_성공한다() {
            //given

            //when
            registrationService.deleteRegisteredCourse(testMemberId, course1Id);

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).hasSize(1);
            assertThat(registrations)
                    .extracting(registration -> registration.getCourse().getId())
                    .containsExactly(course2Id);
        }

        @Test
        void 수강신청_삭제_후_다른_회원의_수강신청은_영향받지_않는다() {
            //given

            //when
            registrationService.deleteRegisteredCourse(testMemberId, course1Id);

            //then
            // otherMember의 수강신청은 그대로 (course3)
            final List<Registration> otherRegistrations = registrationRepository.findByMemberId(otherMemberId);
            assertThat(otherRegistrations).hasSize(1);
            assertThat(otherRegistrations.get(0).getCourse().getId()).isEqualTo(course3Id);
        }

        @Test
        void 존재하지_않는_수강신청을_삭제하면_예외가_발생한다() {
            //given
            final Long nonExistentCourseId = 99999L;

            //when & then
            assertThatThrownBy(() -> registrationService.deleteRegisteredCourse(testMemberId, nonExistentCourseId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", REGISTERED_COURSE_NOT_FOUND);
        }

        @Test
        void 다른_회원의_수강신청을_삭제하면_예외가_발생한다() {
            //given
            // course3은 otherMember만 수강신청 함

            //when & then
            // testMember가 otherMember의 수강신청(course3) 삭제 시도
            assertThatThrownBy(() -> registrationService.deleteRegisteredCourse(testMemberId, course3Id))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", REGISTERED_COURSE_NOT_FOUND);

            // otherMember의 수강신청은 그대로
            final List<Registration> otherRegistrations = registrationRepository.findByMemberId(otherMemberId);
            assertThat(otherRegistrations).hasSize(1);
            assertThat(otherRegistrations.get(0).getCourse().getId()).isEqualTo(course3Id);
        }

        @Test
        void 모든_수강신청을_삭제할_수_있다() {
            //given

            //when
            registrationService.deleteRegisteredCourse(testMemberId, course1Id);
            registrationService.deleteRegisteredCourse(testMemberId, course2Id);

            //then
            final List<Registration> registrations = registrationRepository.findByMemberId(testMemberId);
            assertThat(registrations).isEmpty();
        }
    }
}