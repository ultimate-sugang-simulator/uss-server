package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.course.fixture.CourseFixture;
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
}
