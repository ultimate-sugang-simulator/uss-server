package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@IntegrationTest
class CourseTypeTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_수업_유형_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseType.fromCode("1")).isEqualTo(CourseType.LECTURE);
            assertThat(CourseType.fromCode("7")).isEqualTo(CourseType.OCU);
            assertThat(CourseType.fromCode("25")).isEqualTo(CourseType.K_MOOC);
        }

        @Test
        void 이름이_달라도_코드로_변환된다() {
            //given
            // API 이름은 '담장너머~,사회봉사(1)'로 enum 이름('사회봉사(1)')과 다르다
            final String code = "11";

            //when
            final CourseType type = CourseType.fromCode(code);

            //then
            assertThat(type).isEqualTo(CourseType.SOCIAL_SERVICE_1);
            assertThat(type.getName()).isEqualTo("사회봉사(1)");
        }

        @Test
        void 한_자리와_두_자리_코드를_구분한다() {
            //given

            //when & then
            assertThat(CourseType.fromCode("2")).isEqualTo(CourseType.LAB);
            assertThat(CourseType.fromCode("20")).isEqualTo(CourseType.THEORY_LANGUAGE);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "99";

            //when & then
            assertThatThrownBy(() -> CourseType.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }
}
