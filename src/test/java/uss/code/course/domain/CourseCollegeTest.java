package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@IntegrationTest
class CourseCollegeTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 문자로_시작하는_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseCollege.fromCode("A000")).isEqualTo(CourseCollege.HUMANITIES);
            assertThat(CourseCollege.fromCode("E000")).isEqualTo(CourseCollege.ENGINEERING);
            assertThat(CourseCollege.fromCode("I000")).isEqualTo(CourseCollege.INFORMATION_TECHNOLOGY);
        }

        @Test
        void 숫자로_시작하는_코드도_상수로_변환한다() {
            //given
            // 연계 API의 단과대 코드는 'A000' 형태와 '0000033' 형태가 섞여 있다
            //when & then
            assertThat(CourseCollege.fromCode("0000033")).isEqualTo(CourseCollege.URBAN_SCIENCE);
            assertThat(CourseCollege.fromCode("0000689")).isEqualTo(CourseCollege.COMMERCE_PUBLIC_AFFAIRS);
        }

        @Test
        void 단과대구분없음과_법학은_다른_상수다() {
            //given

            //when & then
            assertThat(CourseCollege.fromCode("0000465")).isEqualTo(CourseCollege.NONE);
            assertThat(CourseCollege.fromCode("0000706")).isEqualTo(CourseCollege.LAW);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "Q000";

            //when & then
            assertThatThrownBy(() -> CourseCollege.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }
}
