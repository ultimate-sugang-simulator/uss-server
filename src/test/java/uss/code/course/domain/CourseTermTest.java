package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@IntegrationTest
class CourseTermTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_학기_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseTerm.fromCode("10")).isEqualTo(CourseTerm.FIRST);
            assertThat(CourseTerm.fromCode("20")).isEqualTo(CourseTerm.SECOND);
            assertThat(CourseTerm.fromCode("30")).isEqualTo(CourseTerm.SUMMER);
            assertThat(CourseTerm.fromCode("40")).isEqualTo(CourseTerm.WINTER);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "50";

            //when & then
            assertThatThrownBy(() -> CourseTerm.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }
}
