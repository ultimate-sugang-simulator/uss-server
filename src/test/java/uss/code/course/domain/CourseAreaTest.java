package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_GENERAL_EDUCATION_AREA;

@IntegrationTest
class CourseAreaTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_영역_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseArea.fromCode("34")).isEqualTo(CourseArea.MAJOR_CORE);
            assertThat(CourseArea.fromCode("161")).isEqualTo(CourseArea.ACADEMIC_FOUNDATION);
            assertThat(CourseArea.fromCode("186")).isEqualTo(CourseArea.FOREIGN_LANGUAGE);
        }

        @Test
        void 이름이_달라도_코드로_변환된다() {
            //given
            // API 이름은 '기초과학ㆍ공학'(U+318D), enum 이름은 '기초과학·공학'(U+00B7)으로 문자가 다르다
            final String code = "162";

            //when
            final CourseArea area = CourseArea.fromCode(code);

            //then
            assertThat(area).isEqualTo(CourseArea.BASIC_SCIENCE_ENGINEERING);
            assertThat(area.getName()).isNotEqualTo("기초과학ㆍ공학");
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "999";

            //when & then
            assertThatThrownBy(() -> CourseArea.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }

    @Nested
    class 교양_영역_판정_테스트 {

        @Test
        void 교양_영역이면_그대로_반환한다() {
            //given
            final String courseArea = "CORE_HUMANITIES";

            //when
            final CourseArea area = CourseArea.fromGeneralEducation(courseArea);

            //then
            assertThat(area).isEqualTo(CourseArea.CORE_HUMANITIES);
        }

        @Test
        void 교양_영역이_아니면_예외가_발생한다() {
            //given
            final String courseArea = "MAJOR_CORE";

            //when & then
            assertThatThrownBy(() -> CourseArea.fromGeneralEducation(courseArea))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_GENERAL_EDUCATION_AREA);
        }
    }
}
