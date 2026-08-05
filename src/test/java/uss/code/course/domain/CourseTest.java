package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.course.fixture.CourseFixture;
import uss.code.course.fixture.CourseScheduleFixture;
import uss.code.global.infra.IntegrationTest;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CourseTest {

    @Nested
    class 시간표_문자열_조립_테스트 {

        @Test
        void 시간표가_없으면_하이픈을_반환한다() {
            //given
            final Course course = CourseFixture.createCourse();

            //when
            final String schedule = course.getFormattedCourseSchedules();

            //then
            assertThat(schedule).isEqualTo("-");
        }

        @Test
        void 강의실이_하나면_교시를_쉼표로_잇는다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.TUESDAY, "1-2A", "07-407",
                    LocalTime.of(9, 0), LocalTime.of(10, 15)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.THURSDAY, "2B-3", "07-407",
                    LocalTime.of(10, 30), LocalTime.of(11, 45)
            ));

            //when
            final String schedule = course.getFormattedCourseSchedules();

            //then
            assertThat(schedule).isEqualTo("[07-407:화(1-2A),목(2B-3)]");
        }

        @Test
        void 강의실이_여러_개면_강의실별로_묶는다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.MONDAY, "5B-6", "05-506",
                    LocalTime.of(13, 30), LocalTime.of(14, 45)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.WEDNESDAY, "5B-6", "05-507",
                    LocalTime.of(13, 30), LocalTime.of(14, 45)
            ));

            //when
            final String schedule = course.getFormattedCourseSchedules();

            //then
            assertThat(schedule).isEqualTo("[05-506:월(5B-6)][05-507:수(5B-6)]");
        }

        @Test
        void 떨어져_있는_같은_강의실은_한_묶음으로_합친다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.MONDAY, "1-2A", "15-113",
                    LocalTime.of(9, 0), LocalTime.of(10, 15)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.TUESDAY, "4-5A", "가상건물-200",
                    LocalTime.of(12, 0), LocalTime.of(13, 15)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.WEDNESDAY, "7-8A", "15-113",
                    LocalTime.of(15, 0), LocalTime.of(16, 15)
            ));

            //when
            final String schedule = course.getFormattedCourseSchedules();

            //then
            assertThat(schedule).isEqualTo("[15-113:월(1-2A),수(7-8A)][가상건물-200:화(4-5A)]");
        }

        @Test
        void 등록_순서와_무관하게_요일_순으로_정렬한다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.FRIDAY, "3", "07-407",
                    LocalTime.of(11, 0), LocalTime.of(11, 50)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.MONDAY, "1", "07-407",
                    LocalTime.of(9, 0), LocalTime.of(9, 50)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.WEDNESDAY, "2", "07-407",
                    LocalTime.of(10, 0), LocalTime.of(10, 50)
            ));

            //when
            final String schedule = course.getFormattedCourseSchedules();

            //then
            assertThat(schedule).isEqualTo("[07-407:월(1),수(2),금(3)]");
        }

        @Test
        void 같은_요일이면_시작_시각_순으로_정렬한다() {
            //given
            final Course course = CourseFixture.createCourse();
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.MONDAY, "6", "08-201",
                    LocalTime.of(14, 0), LocalTime.of(14, 50)
            ));
            course.addCourseSchedule(CourseScheduleFixture.createCourseSchedule(
                    course, CourseDay.MONDAY, "5", "08-201",
                    LocalTime.of(13, 0), LocalTime.of(13, 50)
            ));

            //when
            final String schedule = course.getFormattedCourseSchedules();

            //then
            assertThat(schedule).isEqualTo("[08-201:월(5),월(6)]");
        }
    }

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
}
