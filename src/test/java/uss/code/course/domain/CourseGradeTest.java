package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@IntegrationTest
class CourseGradeTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_학년_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseGrade.fromCode("1")).isEqualTo(CourseGrade.FRESHMAN);
            assertThat(CourseGrade.fromCode("2")).isEqualTo(CourseGrade.SOPHOMORE);
            assertThat(CourseGrade.fromCode("3")).isEqualTo(CourseGrade.JUNIOR);
            assertThat(CourseGrade.fromCode("4")).isEqualTo(CourseGrade.SENIOR);
        }

        @Test
        void 전학년_코드는_0이다() {
            //given
            // API가 주는 이름은 '전학년'이지만 1~4학년은 '1', '2'처럼 숫자만 와서 이름으로는 매칭할 수 없다
            final String code = "0";

            //when
            final CourseGrade grade = CourseGrade.fromCode(code);

            //then
            assertThat(grade).isEqualTo(CourseGrade.ALL);
            assertThat(grade.getYear()).isEqualTo(-1);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "5";

            //when & then
            assertThatThrownBy(() -> CourseGrade.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }
}
