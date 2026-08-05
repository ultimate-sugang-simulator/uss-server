package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@IntegrationTest
class CourseClassificationTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_이수구분_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseClassification.fromCode("11")).isEqualTo(CourseClassification.BASIC_LIBERAL_ARTS);
            assertThat(CourseClassification.fromCode("25")).isEqualTo(CourseClassification.MAJOR_BASIC);
            assertThat(CourseClassification.fromCode("31")).isEqualTo(CourseClassification.MAJOR_CORE);
            assertThat(CourseClassification.fromCode("41")).isEqualTo(CourseClassification.MAJOR_ADVANCED);
        }

        @Test
        void 이수구분과_이수영역은_코드_체계가_다르다() {
            //given
            // 전공핵심이 이수구분에서는 31, 이수영역에서는 34다
            //when & then
            assertThat(CourseClassification.fromCode("31")).isEqualTo(CourseClassification.MAJOR_CORE);
            assertThat(CourseArea.fromCode("31")).isEqualTo(CourseArea.MAJOR_BASIC);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "99";

            //when & then
            assertThatThrownBy(() -> CourseClassification.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }
}
