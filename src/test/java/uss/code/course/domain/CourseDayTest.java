package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@IntegrationTest
class CourseDayTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_요일_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseDay.fromCode("1")).isEqualTo(CourseDay.MONDAY);
            assertThat(CourseDay.fromCode("2")).isEqualTo(CourseDay.TUESDAY);
            assertThat(CourseDay.fromCode("3")).isEqualTo(CourseDay.WEDNESDAY);
            assertThat(CourseDay.fromCode("4")).isEqualTo(CourseDay.THURSDAY);
            assertThat(CourseDay.fromCode("5")).isEqualTo(CourseDay.FRIDAY);
            assertThat(CourseDay.fromCode("6")).isEqualTo(CourseDay.SATURDAY);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "8";

            //when & then
            assertThatThrownBy(() -> CourseDay.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }

        @Test
        void 빈_코드는_일요일로_매칭되지_않는다() {
            //given
            // 일요일은 실데이터에 없어 code가 빈 문자열이다
            final String blankCode = "";

            //when & then
            assertThatThrownBy(() -> CourseDay.fromCode(blankCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
            assertThat(CourseDay.SUNDAY.getCode()).isEmpty();
        }
    }

    @Nested
    class 정렬_순서_테스트 {

        @Test
        void 선언_순서가_월요일부터_일요일까지다() {
            //given
            // 시간표 정렬이 enum 선언 순서에 의존한다
            //when & then
            assertThat(CourseDay.values()).containsExactly(
                    CourseDay.MONDAY,
                    CourseDay.TUESDAY,
                    CourseDay.WEDNESDAY,
                    CourseDay.THURSDAY,
                    CourseDay.FRIDAY,
                    CourseDay.SATURDAY,
                    CourseDay.SUNDAY
            );
        }
    }
}
