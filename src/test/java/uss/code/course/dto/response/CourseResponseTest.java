package uss.code.course.dto.response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uss.code.cart.dto.response.CartedCourseResponse;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseDay;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.course.repository.CourseRepository;
import uss.code.global.infra.IntegrationTest;
import uss.code.registration.dto.response.RegistrationCourseResponse;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CourseResponseTest {

    @Autowired
    private CourseRepository courseRepository;

    private Course createCourse() {
        return courseRepository.save(CourseFixture.createCourse());
    }

    private Course createCourseWithSchedule() {
        final Course course = CourseFixture.createCourse();
        course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                course, CourseDay.MONDAY, "1-2A", "07-407",
                LocalTime.of(9, 0), LocalTime.of(10, 15)
        ));
        return courseRepository.save(course);
    }

    @Nested
    class 강의_식별자_매핑_테스트 {

        @Test
        void 전공_응답은_과목코드와_학수번호를_함께_담는다() {
            //given
            final Course course = createCourse();

            //when
            final MajorCourseResponse response = MajorCourseResponse.from(course);

            //then
            assertThat(response.courseCode()).isEqualTo("CSE2010");
            assertThat(response.haksuCode()).isEqualTo("CSE2010001");
        }

        @Test
        void 교양_응답도_학수번호를_담는다() {
            //given
            final Course course = createCourse();

            //when
            final GeneralEducationCourseResponse response = GeneralEducationCourseResponse.from(course);

            //then
            assertThat(response.courseCode()).isEqualTo("CSE2010");
            assertThat(response.haksuCode()).isEqualTo("CSE2010001");
        }

        @Test
        void 연계전공_응답도_학수번호를_담는다() {
            //given
            final Course course = createCourse();

            //when
            final InterdisciplinaryMajorCourseResponse response = InterdisciplinaryMajorCourseResponse.from(course);

            //then
            assertThat(response.haksuCode()).isEqualTo("CSE2010001");
        }

        @Test
        void 검색_응답도_학수번호를_담는다() {
            //given
            final Course course = createCourse();

            //when
            final SearchedCourseResponse response = SearchedCourseResponse.from(course);

            //then
            assertThat(response.haksuCode()).isEqualTo("CSE2010001");
        }

        @Test
        void 장바구니_응답도_학수번호를_담는다() {
            //given
            final Course course = createCourse();

            //when
            final CartedCourseResponse response = CartedCourseResponse.of(course, 3L);

            //then
            assertThat(response.haksuCode()).isEqualTo("CSE2010001");
            assertThat(response.cartCount()).isEqualTo(3);
        }

        @Test
        void 수강신청_응답도_학수번호를_담는다() {
            //given
            final Course course = createCourse();

            //when
            final RegistrationCourseResponse response = RegistrationCourseResponse.from(course);

            //then
            assertThat(response.haksuCode()).isEqualTo("CSE2010001");
        }
    }

    @Nested
    class 시간표_문자열_매핑_테스트 {

        @Test
        void 응답의_schedule에_강의실과_교시가_함께_담긴다() {
            //given
            final Course course = createCourseWithSchedule();

            //when
            final MajorCourseResponse response = MajorCourseResponse.from(course);

            //then
            assertThat(response.schedule()).isEqualTo("[07-407:월(1-2A)]");
        }

        @Test
        void 시간표가_없으면_하이픈이_담긴다() {
            //given
            final Course course = createCourse();

            //when
            final MajorCourseResponse response = MajorCourseResponse.from(course);

            //then
            assertThat(response.schedule()).isEqualTo("-");
        }
    }

    @Nested
    class 강의_정보_매핑_테스트 {

        @Test
        void 학년과_이수구분과_학과를_이름으로_내보낸다() {
            //given
            final Course course = createCourse();

            //when
            final MajorCourseResponse response = MajorCourseResponse.from(course);

            //then
            assertThat(response.grade()).isEqualTo("2학년");
            assertThat(response.classification()).isEqualTo("전공핵심");
            assertThat(response.department()).isEqualTo("컴퓨터공학부");
        }

        @Test
        void 교양_응답은_이수영역을_이름으로_내보낸다() {
            //given
            final Course course = createCourse();

            //when
            final GeneralEducationCourseResponse response = GeneralEducationCourseResponse.from(course);

            //then
            assertThat(response.area()).isEqualTo("전공핵심");
            assertThat(response.titleKr()).isEqualTo("데이터구조");
            assertThat(response.titleEn()).isEqualTo("Data Structure");
        }
    }
}
