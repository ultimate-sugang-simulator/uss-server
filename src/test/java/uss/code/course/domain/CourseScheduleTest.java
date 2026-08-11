package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;

import static org.assertj.core.api.Assertions.assertThat;

class CourseScheduleTest {

    @Nested
    class 수업_길이_판정_테스트 {

        @Test
        void 교시_코드가_B로_시작하면_75분_수업이다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final CourseSchedule morning = CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "B01");
            final CourseSchedule night = CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "B10");

            //then
            assertThat(morning.is75MinLesson()).isTrue();
            assertThat(night.is75MinLesson()).isTrue();
        }

        @Test
        void 교시_코드가_A나_C로_시작하면_75분_수업이_아니다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final CourseSchedule day = CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "A03");
            final CourseSchedule night = CourseScheduleFixture.createCourseScheduleWithPeriodCode(course, "C01");

            //then
            assertThat(day.is75MinLesson()).isFalse();
            assertThat(night.is75MinLesson()).isFalse();
        }
    }
}
