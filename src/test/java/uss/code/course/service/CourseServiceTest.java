package uss.code.course.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseClassification;
import uss.code.course.domain.CourseCollege;
import uss.code.course.domain.CourseDay;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.domain.CourseGrade;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseType;
import uss.code.course.dto.response.GeneralEducationCourseResponse;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCourseResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCoursesResponse;
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
import static uss.code.global.exception.domain.BusinessExceptionCode.*;

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
            // COM001: 월3,4 수3,4
            final MajorCourseResponse allGrade1 = response.majorCourses().stream()
                    .filter(c -> c.courseCode().equals("COM001"))
                    .findFirst()
                    .orElseThrow();
            assertThat(allGrade1.schedule()).isEqualTo("월3,4 수3,4");

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
            assertThat(coreHumanities1.schedule()).isEqualTo("월1,2 수1,2");
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

    @Nested
    class 타학과_전공과목_조회_테스트 {

        @BeforeEach
        void setUp() {
            // 수학과 과목들
            // 전학년 과목 2개
            Course mathAllGrade1 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "미적분학", "Calculus", "MATH101",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.ALL, "김교수", "자연관101"
            );
            Course mathAllGrade2 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "선형대수", "Linear Algebra", "MATH102",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.ALL, null, null
            );

            // 1학년 과목 2개
            Course mathFreshman1 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "수학의이해", "Understanding Mathematics", "MATH201",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.FRESHMAN, "이교수", "자연관201"
            );
            Course mathFreshman2 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "기초수학", "Basic Mathematics", "MATH202",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.FRESHMAN, "박교수", null
            );

            // 2학년 과목 2개
            Course mathSophomore1 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "해석학", "Analysis", "MATH301",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.SOPHOMORE, null, "자연관301"
            );
            Course mathSophomore2 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "정수론", "Number Theory", "MATH302",
                    CourseDepartment.MATHEMATICS,
                    CourseGrade.SOPHOMORE, "최교수", "자연관302"
            );

            // 컴퓨터공학부 과목 (다른 학과)
            Course cseCourse = CourseFixture.createCourseWithDetails(
                    "자료구조", "Data Structure", "CSE101",
                    CourseGrade.SOPHOMORE, "정교수", "공학관101"
            );

            // 스케줄 추가
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    mathAllGrade1, "월1,2", CourseDay.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    mathAllGrade1, "수1,2", CourseDay.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
                    mathFreshman1, "화3,4", CourseDay.TUESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );

            mathAllGrade1.addCourseSchedule(schedule1);
            mathAllGrade1.addCourseSchedule(schedule2);
            mathFreshman1.addCourseSchedule(schedule3);

            courseRepository.saveAll(List.of(
                    mathAllGrade1, mathAllGrade2,
                    mathFreshman1, mathFreshman2,
                    mathSophomore1, mathSophomore2,
                    cseCourse
            ));
        }

        @Test
        void 수학과_학과코드로_조회하면_수학과_과목만_반환된다() {
            //given
            final String department = "MATHEMATICS";

            //when
            final MajorCoursesResponse response = courseService.getOtherDepartmentCourses(department);

            //then
            assertThat(response.majorCourses()).hasSize(6);
            assertThat(response.majorCourses())
                    .extracting(MajorCourseResponse::department)
                    .containsOnly("수학과");
        }

        @Test
        void 전학년_1학년_2학년_순서로_정렬되어_조회된다() {
            //given
            final String department = "MATHEMATICS";

            //when
            final MajorCoursesResponse response = courseService.getOtherDepartmentCourses(department);
            final List<String> grades = response.majorCourses().stream()
                    .map(MajorCourseResponse::courseGrade)
                    .toList();

            //then
            assertThat(grades).containsExactly(
                    "전학년", "전학년",  // MATH101, MATH102
                    "1학년", "1학년",    // MATH201, MATH202
                    "2학년", "2학년"     // MATH301, MATH302
            );
        }

        @Test
        void 스케줄이_있는_과목은_요일순으로_정렬되어_반환된다() {
            //given
            final String department = "MATHEMATICS";

            //when
            final MajorCoursesResponse response = courseService.getOtherDepartmentCourses(department);

            //then
            // MATH101: 월1,2 수1,2
            final MajorCourseResponse mathAllGrade1 = response.majorCourses().stream()
                    .filter(c -> c.courseCode().equals("MATH101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(mathAllGrade1.schedule()).isEqualTo("월1,2 수1,2");

            // MATH201: 화3,4
            final MajorCourseResponse mathFreshman1 = response.majorCourses().stream()
                    .filter(c -> c.courseCode().equals("MATH201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(mathFreshman1.schedule()).isEqualTo("화3,4");
        }

        @Test
        void 다른_학과_과목은_조회되지_않는다() {
            //given
            final String department = "MATHEMATICS";

            //when
            final MajorCoursesResponse response = courseService.getOtherDepartmentCourses(department);

            //then
            assertThat(response.majorCourses())
                    .extracting(MajorCourseResponse::courseCode)
                    .doesNotContain("CSE101");
        }

        @Test
        void 잘못된_학과_코드로_조회하면_예외가_발생한다() {
            //given
            final String invalidDepartment = "INVALID_DEPARTMENT";

            //when & then
            assertThatThrownBy(() -> courseService.getOtherDepartmentCourses(invalidDepartment))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }

    @Nested
    class 학제간융합전공_과목_조회_테스트 {

        @BeforeEach
        void setUp() {
            // 소셜데이터사이언스 연계전공 과목들
            // 전학년 과목 2개
            Course socialDataAllGrade1 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "빅데이터분석", "Big Data Analysis", "SDS101",
                    CourseDepartment.SOCIAL_DATA_SCIENCE,
                    CourseGrade.ALL, "김교수", "융합관101"
            );
            Course socialDataAllGrade2 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "데이터사이언스개론", "Intro to Data Science", "SDS102",
                    CourseDepartment.SOCIAL_DATA_SCIENCE,
                    CourseGrade.ALL, null, null
            );

            // 1학년 과목 2개
            Course socialDataFreshman1 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "통계학기초", "Basic Statistics", "SDS201",
                    CourseDepartment.SOCIAL_DATA_SCIENCE,
                    CourseGrade.FRESHMAN, "이교수", "융합관201"
            );
            Course socialDataFreshman2 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "프로그래밍입문", "Programming Intro", "SDS202",
                    CourseDepartment.SOCIAL_DATA_SCIENCE,
                    CourseGrade.FRESHMAN, "박교수", null
            );

            // 2학년 과목 2개
            Course socialDataSophomore1 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "머신러닝", "Machine Learning", "SDS301",
                    CourseDepartment.SOCIAL_DATA_SCIENCE,
                    CourseGrade.SOPHOMORE, null, "융합관301"
            );
            Course socialDataSophomore2 = CourseFixture.createCourseWithDepartmentAndDetails(
                    "데이터시각화", "Data Visualization", "SDS302",
                    CourseDepartment.SOCIAL_DATA_SCIENCE,
                    CourseGrade.SOPHOMORE, "최교수", "융합관302"
            );

            // 미래자동차 연계전공 과목 (다른 연계전공)
            Course futureAutoCourse = CourseFixture.createCourseWithDepartmentAndDetails(
                    "자율주행개론", "Intro to Autonomous Driving", "FA101",
                    CourseDepartment.FUTURE_AUTOMOBILE,
                    CourseGrade.SOPHOMORE, "정교수", "융합관401"
            );

            // 일반 학과 과목 (컴퓨터공학부)
            Course cseCourse = CourseFixture.createCourseWithDetails(
                    "자료구조", "Data Structure", "CSE101",
                    CourseGrade.SOPHOMORE, "홍교수", "공학관101"
            );

            // 스케줄 추가
            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
                    socialDataAllGrade1, "월1,2", CourseDay.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
                    socialDataAllGrade1, "수1,2", CourseDay.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
            );
            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
                    socialDataFreshman1, "화3,4", CourseDay.TUESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
            );

            socialDataAllGrade1.addCourseSchedule(schedule1);
            socialDataAllGrade1.addCourseSchedule(schedule2);
            socialDataFreshman1.addCourseSchedule(schedule3);

            courseRepository.saveAll(List.of(
                    socialDataAllGrade1, socialDataAllGrade2,
                    socialDataFreshman1, socialDataFreshman2,
                    socialDataSophomore1, socialDataSophomore2,
                    futureAutoCourse,
                    cseCourse
            ));
        }

        @Test
        void 소셜데이터사이언스_학과코드로_조회하면_해당_연계전공_과목만_반환된다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);

            //then
            assertThat(response.interdisciplinaryMajorCourseResponses()).hasSize(6);
            assertThat(response.interdisciplinaryMajorCourseResponses())
                    .extracting(InterdisciplinaryMajorCourseResponse::courseCode)
                    .containsExactlyInAnyOrder("SDS101", "SDS102", "SDS201", "SDS202", "SDS301", "SDS302");
        }

        @Test
        void 전학년_1학년_2학년_순서로_정렬되어_조회된다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);
            final List<String> grades = response.interdisciplinaryMajorCourseResponses().stream()
                    .map(InterdisciplinaryMajorCourseResponse::courseGrade)
                    .toList();

            //then
            assertThat(grades).containsExactly(
                    "전학년", "전학년",  // SDS101, SDS102
                    "1학년", "1학년",    // SDS201, SDS202
                    "2학년", "2학년"     // SDS301, SDS302
            );
        }

        @Test
        void 스케줄이_있는_과목은_요일순으로_정렬되어_반환된다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);

            //then
            // SDS101: 월1,2 수1,2
            final InterdisciplinaryMajorCourseResponse allGrade1 = response.interdisciplinaryMajorCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("SDS101"))
                    .findFirst()
                    .orElseThrow();
            assertThat(allGrade1.schedule()).isEqualTo("월1,2 수1,2");

            // SDS201: 화3,4
            final InterdisciplinaryMajorCourseResponse freshman1 = response.interdisciplinaryMajorCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("SDS201"))
                    .findFirst()
                    .orElseThrow();
            assertThat(freshman1.schedule()).isEqualTo("화3,4");
        }

        @Test
        void 스케줄이_없는_과목은_하이픈으로_반환된다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);

            //then
            final InterdisciplinaryMajorCourseResponse allGrade2 = response.interdisciplinaryMajorCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("SDS102"))
                    .findFirst()
                    .orElseThrow();
            assertThat(allGrade2.schedule()).isEqualTo("-");
        }

        @Test
        void null값인_교수명과_강의실은_하이픈으로_반환된다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);

            //then
            final InterdisciplinaryMajorCourseResponse allGrade2 = response.interdisciplinaryMajorCourseResponses().stream()
                    .filter(c -> c.courseCode().equals("SDS102"))
                    .findFirst()
                    .orElseThrow();
            assertThat(allGrade2.professor()).isEqualTo("-");
            assertThat(allGrade2.classroom()).isEqualTo("-");
        }

        @Test
        void 다른_연계전공_과목은_조회되지_않는다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);

            //then
            assertThat(response.interdisciplinaryMajorCourseResponses())
                    .extracting(InterdisciplinaryMajorCourseResponse::courseCode)
                    .doesNotContain("FA101");
        }

        @Test
        void 일반_학과_과목은_조회되지_않는다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final InterdisciplinaryMajorCoursesResponse response = courseService.getInterdisciplinaryMajorCourses(department);

            //then
            assertThat(response.interdisciplinaryMajorCourseResponses())
                    .extracting(InterdisciplinaryMajorCourseResponse::courseCode)
                    .doesNotContain("CSE101");
        }

        @Test
        void 잘못된_학과_코드로_조회하면_예외가_발생한다() {
            //given
            final String invalidDepartment = "INVALID_DEPARTMENT";

            //when & then
            assertThatThrownBy(() -> courseService.getInterdisciplinaryMajorCourses(invalidDepartment))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }

        @Test
        void 일반_학과_코드로_조회하면_예외가_발생한다() {
            //given
            final String normalDepartment = "COMPUTER_ENGINEERING";

            //when & then
            assertThatThrownBy(() -> courseService.getInterdisciplinaryMajorCourses(normalDepartment))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_INTERDISCIPLINARY_DEPARTMENT);
        }
    }

//    @Nested
//    class 키워드_검색_테스트 {
//
//        @BeforeEach
//        void setUp() {
//            // 컴퓨터공학부 과목들
//            Course cse1 = CourseFixture.createCourseWithDetails(
//                    "자료구조", "Data Structure", "CSE101",
//                    CourseGrade.SOPHOMORE, "김교수", "공학관101"
//            );
//            Course cse2 = CourseFixture.createCourseWithDetails(
//                    "알고리즘", "Algorithm", "CSE201",
//                    CourseGrade.SOPHOMORE, "이교수", "공학관201"
//            );
//            Course cse3 = CourseFixture.createCourseWithDetails(
//                    "알고리즘설계", "Algorithm Design", "CSE202",
//                    CourseGrade.JUNIOR, null, null
//            );
//
//            // 수학과 과목들
//            Course math1 = CourseFixture.createCourseWithDepartmentAndDetails(
//                    "선형대수", "Linear Algebra", "MATH101",
//                    CourseDepartment.MATHEMATICS,
//                    CourseGrade.FRESHMAN, "박교수", "자연관101"
//            );
//            Course math2 = CourseFixture.createCourseWithDepartmentAndDetails(
//                    "데이터분석", "Data Analysis", "MATH201",
//                    CourseDepartment.MATHEMATICS,
//                    CourseGrade.SOPHOMORE, "최교수", "자연관201"
//            );
//
//            // 소셜데이터사이언스 연계전공 과목
//            Course sds = CourseFixture.createCourseWithDepartmentAndDetails(
//                    "빅데이터분석", "Big Data Analysis", "SDS101",
//                    CourseDepartment.SOCIAL_DATA_SCIENCE,
//                    CourseGrade.ALL, "정교수", null
//            );
//
//            // 스케줄 추가
//            CourseSchedule schedule1 = CourseScheduleFixture.createCourseSchedule(
//                    cse1, "월3,4", CourseDay.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
//            );
//            CourseSchedule schedule2 = CourseScheduleFixture.createCourseSchedule(
//                    cse1, "수3,4", CourseDay.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0)
//            );
//            CourseSchedule schedule3 = CourseScheduleFixture.createCourseSchedule(
//                    cse2, "화1,2", CourseDay.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)
//            );
//
//            cse1.addCourseSchedule(schedule1);
//            cse1.addCourseSchedule(schedule2);
//            cse2.addCourseSchedule(schedule3);
//
//            courseRepository.saveAll(List.of(cse1, cse2, cse3, math1, math2, sds));
//        }
//
//        @Test
//        void 교과목명_국문으로_검색하면_해당_키워드가_포함된_과목이_조회된다() {
//            //given
//            final String keyword = "알고리즘";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            assertThat(response.searchedCourseResponses()).hasSize(2);
//            assertThat(response.searchedCourseResponses())
//                    .extracting(SearchedCourseResponse::courseTitleKr)
//                    .containsExactlyInAnyOrder("알고리즘", "알고리즘설계");
//        }
//
//        @Test
//        void 교과목명_영문으로_검색하면_해당_키워드가_포함된_과목이_조회된다() {
//            //given
//            final String keyword = "Data";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            assertThat(response.searchedCourseResponses()).hasSize(3);
//            assertThat(response.searchedCourseResponses())
//                    .extracting(SearchedCourseResponse::courseTitleEn)
//                    .containsExactlyInAnyOrder("Data Structure", "Data Analysis", "Big Data Analysis");
//        }
//
//        @Test
//        void 과목코드로_검색하면_해당_키워드가_포함된_과목이_조회된다() {
//            //given
//            final String keyword = "CSE";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            assertThat(response.searchedCourseResponses()).hasSize(3);
//            assertThat(response.searchedCourseResponses())
//                    .extracting(SearchedCourseResponse::courseCode)
//                    .containsExactlyInAnyOrder("CSE101", "CSE201", "CSE202");
//        }
//
//        @Test
//        void 부분_문자열_검색이_정상_동작한다() {
//            //given
//            final String keyword = "101";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            assertThat(response.searchedCourseResponses()).hasSize(3);
//            assertThat(response.searchedCourseResponses())
//                    .extracting(SearchedCourseResponse::courseCode)
//                    .containsExactlyInAnyOrder("CSE101", "MATH101", "SDS101");
//        }
//
//        @Test
//        void 여러_필드에서_동시에_매칭되어도_중복_없이_조회된다() {
//            //given
//            final String keyword = "Data";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            // "빅데이터분석" 과목은 국문 과목명과 영문 과목명 모두에 "Data"를 포함하지만 한 번만 조회
//            assertThat(response.searchedCourseResponses()).hasSize(3);
//            final long sdsCount = response.searchedCourseResponses().stream()
//                    .filter(c -> c.courseCode().equals("SDS101"))
//                    .count();
//            assertThat(sdsCount).isEqualTo(1);
//        }
//
//        @Test
//        void 검색_결과가_없으면_빈_리스트가_반환된다() {
//            //given
//            final String keyword = "존재하지않는과목";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            assertThat(response.searchedCourseResponses()).isEmpty();
//        }
//
//        @Test
//        void 스케줄이_있는_과목은_요일순으로_정렬되어_반환된다() {
//            //given
//            final String keyword = "CSE";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            // CSE101: 월3,4 수3,4
//            final SearchedCourseResponse cse101 = response.searchedCourseResponses().stream()
//                    .filter(c -> c.courseCode().equals("CSE101"))
//                    .findFirst()
//                    .orElseThrow();
//            assertThat(cse101.schedule()).isEqualTo("월3,4 수3,4");
//
//            // CSE201: 화1,2
//            final SearchedCourseResponse cse201 = response.searchedCourseResponses().stream()
//                    .filter(c -> c.courseCode().equals("CSE201"))
//                    .findFirst()
//                    .orElseThrow();
//            assertThat(cse201.schedule()).isEqualTo("화1,2");
//        }
//
//        @Test
//        void 스케줄이_없는_과목은_하이픈으로_반환된다() {
//            //given
//            final String keyword = "알고리즘설계";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            final SearchedCourseResponse cse202 = response.searchedCourseResponses().stream()
//                    .filter(c -> c.courseCode().equals("CSE202"))
//                    .findFirst()
//                    .orElseThrow();
//            assertThat(cse202.schedule()).isEqualTo("-");
//        }
//
//        @Test
//        void null값인_교수명과_강의실은_하이픈으로_반환된다() {
//            //given
//            final String keyword = "알고리즘설계";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            final SearchedCourseResponse cse202 = response.searchedCourseResponses().stream()
//                    .filter(c -> c.courseCode().equals("CSE202"))
//                    .findFirst()
//                    .orElseThrow();
//            assertThat(cse202.professor()).isEqualTo("-");
//            assertThat(cse202.classroom()).isEqualTo("-");
//        }
//
//        @Test
//        void 다양한_학과의_과목들이_모두_검색된다() {
//            //given
//            final String keyword = "101";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keyword);
//
//            //then
//            assertThat(response.searchedCourseResponses())
//                    .extracting(SearchedCourseResponse::courseDepartment)
//                    .containsExactlyInAnyOrder("컴퓨터공학부", "수학과", "소셜데이터사이언스연계전공");
//        }
//
//        @Test
//        void 대소문자_구분_없이_검색된다() {
//            //given
//            final String keywordLower = "data";
//
//            //when
//            final SearchedCoursesResponse response = courseService.searchCourses(keywordLower);
//
//            //then
//            assertThat(response.searchedCourseResponses()).hasSize(3);
//        }
//    }
}
