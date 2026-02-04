package uss.code.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.course.domain.*;
import uss.code.course.dto.response.GeneralEducationCourseResponse;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.MajorCourseResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_GENERAL_EDUCATION_AREA;
import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

@IntegrationTest
class CourseServiceTest {

    @Autowired
    private CourseService courseService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CourseRepository courseRepository;

    @Nested
    class 전공_과목_조회_테스트 {

        private static final String TEST_STUDENT_ID = "202012345";
        private static final String TEST_PASSWORD = "password1234";
        private static final String TEST_NAME = "홍길동";
        private static final String TEST_EMAIL = "hong@inu.ac.kr";
        private static final String TEST_COLLEGE = "INFORMATION_TECHNOLOGY";
        private static final String TEST_DEPARTMENT = "COMPUTER_ENGINEERING";
        private static final String TEST_GRADE = "JUNIOR";
        private static final String TEST_ACADEMIC_STATUS = "ENROLLED";
        private static final double TEST_GPA = 3.5;

        private long validMemberId;
        private final long invalidMemberId = 999L;

        @BeforeEach
        void setUp() {
            // 회원 생성 (컴퓨터공학부)
            final SignUpRequest signUpRequest = new SignUpRequest(
                    TEST_STUDENT_ID,
                    TEST_PASSWORD,
                    TEST_NAME,
                    TEST_EMAIL,
                    TEST_COLLEGE,
                    TEST_DEPARTMENT,
                    TEST_GRADE,
                    TEST_ACADEMIC_STATUS,
                    TEST_GPA
            );
            final String encodedPassword = "testPassword1234";
            final Member member = Member.signUp(signUpRequest, encodedPassword);
            memberRepository.save(member);
            validMemberId = member.getId();

            // 전학년 과목 2개
            Course allGrade1 = CourseFixture.createCourseWithDetails(
                    "자료구조", "Data Structure", "COM001",
                    CourseGrade.ALL, "김교수", "공학관101"
            );
            Course allGrade2 = CourseFixture.createCourseWithDetails(
                    "알고리즘", "Algorithm", "COM002",
                    CourseGrade.ALL, null, null
            );

            // 1학년 과목 2개
            Course freshman1 = CourseFixture.createCourseWithDetails(
                    "프로그래밍기초", "Programming Basics", "COM101",
                    CourseGrade.FRESHMAN, "이교수", "공학관201"
            );
            Course freshman2 = CourseFixture.createCourseWithDetails(
                    "컴퓨터개론", "Introduction to Computer", "COM102",
                    CourseGrade.FRESHMAN, "박교수", null
            );

            // 2학년 과목 2개
            Course sophomore1 = CourseFixture.createCourseWithDetails(
                    "객체지향프로그래밍", "OOP", "COM201",
                    CourseGrade.SOPHOMORE, null, "공학관301"
            );
            Course sophomore2 = CourseFixture.createCourseWithDetails(
                    "데이터베이스", "Database", "COM202",
                    CourseGrade.SOPHOMORE, "최교수", "공학관302"
            );

            // 컴공이 아닌 다른 학과 과목
            Course otherDept = CourseFixture.createCourseWithDepartmentAndDetails(
                    "미적분학", "Calculus", "MATH101",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.FRESHMAN, "수학교수", "자연관101"
            );

            // 스케줄 추가 (저장 전에 추가)
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    allGrade1, "월3,4", CourseDay.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    allGrade1, "수3,4", CourseDay.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );
            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
                    freshman1, "화1,2", CourseDay.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );

            allGrade1.addCourseSchedule(schedule1);
            allGrade1.addCourseSchedule(schedule2);
            freshman1.addCourseSchedule(schedule3);

            courseRepository.saveAll(List.of(
                    allGrade1, allGrade2,
                    freshman1, freshman2,
                    sophomore1, sophomore2,
                    otherDept
            ));
        }

        @Test
        void 컴퓨터공학부_학생이_전공과목을_조회하면_성공한다() {
            //given

            //when
            final MajorCoursesResponse response = courseService.getMajorCourses(validMemberId);

            //then
            assertThat(response.majorCourses()).hasSize(6);
            assertThat(response.majorCourses())
                    .extracting(MajorCourseResponse::department)
                    .containsOnly("컴퓨터공학부");
        }

        @Test
        void 전학년_1학년_2학년_순서로_정렬되어_조회된다() {
            //given

            //when
            final MajorCoursesResponse response = courseService.getMajorCourses(validMemberId);
            final List<String> grades = response.majorCourses().stream()
                    .map(MajorCourseResponse::courseGrade)
                    .toList();

            //then
            assertThat(grades).containsExactly(
                    "전학년", "전학년",  // COM001, COM002
                    "1학년", "1학년",    // COM101, COM102
                    "2학년", "2학년"     // COM201, COM202
            );
        }

        @Test
        void 스케줄이_있는_과목은_요일순으로_정렬되어_반환된다() {
            //given

            //when
            final MajorCoursesResponse response = courseService.getMajorCourses(validMemberId);

            //then
            // COM001: 월3,4, 수3,4
            final MajorCourseResponse allGrade1 = response.majorCourses().stream()
                    .filter(c -> c.courseCode().equals("COM001"))
                    .findFirst()
                    .orElseThrow();
            assertThat(allGrade1.schedule()).isEqualTo("월3,4, 수3,4");

            // COM101: 화1,2
            final MajorCourseResponse freshman1 = response.majorCourses().stream()
                    .filter(c -> c.courseCode().equals("COM101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(freshman1.schedule()).isEqualTo("화1,2");
        }

        @Test
        void 다른_학과_과목은_조회되지_않는다() {
            //given

            //when
            final MajorCoursesResponse response = courseService.getMajorCourses(validMemberId);

            //then
            assertThat(response.majorCourses())
                    .extracting(MajorCourseResponse::courseCode)
                    .doesNotContain("MATH101");
        }

        @Test
        void 존재하지_않는_회원_아이디로_조회하면_예외가_발생한다() {
            //given

            //when & then
            assertThatThrownBy(() -> courseService.getMajorCourses(invalidMemberId))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND);
        }
    }

    @Nested
    class 교양_과목_조회_테스트 {

        @BeforeEach
        void setUp() {
            // 핵심 인문 과목 2개
            Course coreHumanities1 = CourseFixture.createCourse(
                    "글쓰기", "Writing", "GEN101",
                    CourseCollege.GENERAL_EDUCATION, CourseDepartment.GENERAL_EDUCATION,
                    CourseClassification.CORE_LIBERAL_ARTS, CourseArea.CORE_HUMANITIES,
                    CourseType.LECTURE, CourseGrade.ALL,
                    "이교수", "인문관101",
                    3, false, 50, 30
            );
            Course coreHumanities2 = CourseFixture.createCourse(
                    "철학의이해", "Understanding Philosophy", "GEN102",
                    CourseCollege.GENERAL_EDUCATION, CourseDepartment.GENERAL_EDUCATION,
                    CourseClassification.CORE_LIBERAL_ARTS, CourseArea.CORE_HUMANITIES,
                    CourseType.LECTURE, CourseGrade.ALL,
                    null, null,
                    2, false, 40, 20
            );

            // 핵심 외국어 과목 2개
            Course coreForeignLanguage1 = CourseFixture.createCourse(
                    "영어회화", "English Conversation", "GEN201",
                    CourseCollege.GENERAL_EDUCATION, CourseDepartment.GENERAL_EDUCATION,
                    CourseClassification.CORE_LIBERAL_ARTS, CourseArea.CORE_FOREIGN_LANGUAGE,
                    CourseType.LECTURE, CourseGrade.ALL,
                    "박교수", "인문관201",
                    3, true, 45, 25
            );
            Course coreForeignLanguage2 = CourseFixture.createCourse(
                    "중국어회화", "Chinese Conversation", "GEN202",
                    CourseCollege.GENERAL_EDUCATION, CourseDepartment.GENERAL_EDUCATION,
                    CourseClassification.CORE_LIBERAL_ARTS, CourseArea.CORE_FOREIGN_LANGUAGE,
                    CourseType.LECTURE, CourseGrade.ALL,
                    "최교수", "인문관202",
                    3, false, 40, 15
            );

            // 일반 사회 과목 1개
            Course social = CourseFixture.createCourse(
                    "현대사회와윤리", "Modern Society and Ethics", "GEN301",
                    CourseCollege.GENERAL_EDUCATION, CourseDepartment.GENERAL_EDUCATION,
                    CourseClassification.ADVANCED_LIBERAL_ARTS, CourseArea.SOCIAL,
                    CourseType.LECTURE, CourseGrade.ALL,
                    "김교수", "인문관301",
                    3, false, 35, 20
            );

            // 전공 과목 (교양이 아님)
            Course majorCourse = CourseFixture.createCourse(
                    "데이터구조", "Data Structure", "COM101",
                    CourseCollege.INFORMATION_TECHNOLOGY, CourseDepartment.COMPUTER_ENGINEERING,
                    CourseClassification.MAJOR_CORE, CourseArea.MAJOR_CORE,
                    CourseType.LECTURE, CourseGrade.SOPHOMORE,
                    "홍교수", "공학관101",
                    3, false, 50, 40
            );

            // 스케줄 추가
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    coreHumanities1, "월1,2", CourseDay.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    coreHumanities1, "수1,2", CourseDay.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
                    coreForeignLanguage1, "화3,4", CourseDay.TUESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );

            coreHumanities1.addCourseSchedule(schedule1);
            coreHumanities1.addCourseSchedule(schedule2);
            coreForeignLanguage1.addCourseSchedule(schedule3);

            courseRepository.saveAll(List.of(
                    coreHumanities1, coreHumanities2,
                    coreForeignLanguage1, coreForeignLanguage2,
                    social,
                    majorCourse
            ));
        }

        @Test
        void 핵심_인문_영역으로_조회하면_해당_영역의_과목만_반환된다() {
            //given
            final String courseArea = "CORE_HUMANITIES";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            assertThat(response.generalEducationCourseResponses()).hasSize(2);
            assertThat(response.generalEducationCourseResponses())
                    .extracting(GeneralEducationCourseResponse::courseArea)
                    .containsOnly("(핵심)인문");
        }

        @Test
        void 핵심_외국어_영역으로_조회하면_해당_영역의_과목만_반환된다() {
            //given
            final String courseArea = "CORE_FOREIGN_LANGUAGE";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            assertThat(response.generalEducationCourseResponses()).hasSize(2);
            assertThat(response.generalEducationCourseResponses())
                    .extracting(GeneralEducationCourseResponse::courseArea)
                    .containsOnly("(핵심)외국어");
        }

        @Test
        void 일반_사회_영역으로_조회하면_해당_영역의_과목만_반환된다() {
            //given
            final String courseArea = "SOCIAL";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            assertThat(response.generalEducationCourseResponses()).hasSize(1);
            assertThat(response.generalEducationCourseResponses())
                    .extracting(GeneralEducationCourseResponse::courseArea)
                    .containsOnly("사회");
        }

        @Test
        void 스케줄이_있는_교양_과목은_요일순으로_정렬되어_반환된다() {
            //given
            final String courseArea = "CORE_HUMANITIES";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            final GeneralEducationCourseResponse coreHumanities1 = response.generalEducationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("GEN101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(coreHumanities1.schedule()).isEqualTo("월1,2, 수1,2");
        }

        @Test
        void 스케줄이_없는_교양_과목은_하이픈으로_반환된다() {
            //given
            final String courseArea = "CORE_HUMANITIES";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            final GeneralEducationCourseResponse coreHumanities2 = response.generalEducationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("GEN102"))
                    .findFirst()
                    .orElseThrow();
            assertThat(coreHumanities2.schedule()).isEqualTo("-");
        }

        @Test
        void null값인_교수명과_강의실은_하이픈으로_반환된다() {
            //given
            final String courseArea = "CORE_HUMANITIES";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            final GeneralEducationCourseResponse coreHumanities2 = response.generalEducationCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("GEN102"))
                    .findFirst()
                    .orElseThrow();
            assertThat(coreHumanities2.professor()).isEqualTo("-");
            assertThat(coreHumanities2.classroom()).isEqualTo("-");
        }

        @Test
        void 전공_과목은_교양_조회시_포함되지_않는다() {
            //given
            final String courseArea = "CORE_HUMANITIES";

            //when
            final GeneralEducationCoursesResponse response = courseService.getGeneralEducationCourses(courseArea);

            //then
            assertThat(response.generalEducationCourseResponses())
                    .extracting(GeneralEducationCourseResponse::courseCode)
                    .doesNotContain("COM101");
        }

        @Test
        void 잘못된_교양_영역으로_조회하면_예외가_발생한다() {
            //given
            final String invalidCourseArea = "INVALID_AREA";

            //when & then
            assertThatThrownBy(() -> courseService.getGeneralEducationCourses(invalidCourseArea))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }

        @Test
        void 전공_영역으로_조회하면_예외가_발생한다() {
            //given
            final String majorCourseArea = "MAJOR_CORE";

            //when & then
            assertThatThrownBy(() -> courseService.getGeneralEducationCourses(majorCourseArea))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_GENERAL_EDUCATION_AREA);
        }
    }
}
