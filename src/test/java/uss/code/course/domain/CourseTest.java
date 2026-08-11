package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CourseTest {

    @Nested
    class 수강_가능_판정_테스트 {

        @Test
        void 현재_수강인원이_정원보다_적으면_신청할_수_있다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final boolean registerable = course.isRegisterable();

            //then
            assertThat(registerable).isTrue();
        }

        @Test
        void 현재_수강인원이_정원과_같으면_신청할_수_없다() {
            //given
            final Course course = CourseFixture.createCourse(
                    "데이터구조", "Data Structure", "CSE2010", "CSE2010001",
                    CourseCollege.INFORMATION_TECHNOLOGY,
                    CourseDepartment.COMPUTER_ENGINEERING,
                    CourseClassification.MAJOR_CORE,
                    CourseArea.MAJOR_CORE,
                    CourseType.LECTURE,
                    CourseGrade.SOPHOMORE,
                    3, false, 50, 50
            );

            //when
            final boolean registerable = course.isRegisterable();

            //then
            assertThat(registerable).isFalse();
        }
    }

    @Nested
    class 수업_길이_판정_테스트 {

        @Test
        void 시간표_중_하나라도_75분이면_75분_수업이다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "A03"));
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "B02"));

            //when
            final boolean is75MinLesson = course.is75MinLesson();

            //then
            assertThat(is75MinLesson).isTrue();
        }

        @Test
        void 시간표가_모두_50분이면_75분_수업이_아니다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "A03"));
            course.addCourseSchedule(CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "C01"));

            //when
            final boolean is75MinLesson = course.is75MinLesson();

            //then
            assertThat(is75MinLesson).isFalse();
        }

        @Test
        void 시간표가_없으면_75분_수업이_아니다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final boolean is75MinLesson = course.is75MinLesson();

            //then
            assertThat(is75MinLesson).isFalse();
        }
    }
}
