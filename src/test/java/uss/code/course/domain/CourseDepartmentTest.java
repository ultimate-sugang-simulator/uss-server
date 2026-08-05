package uss.code.course.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.infra.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_INTERDISCIPLINARY_DEPARTMENT;

@IntegrationTest
class CourseDepartmentTest {

    @Nested
    class 코드값_변환_테스트 {

        @Test
        void 연계_API의_학과_코드를_상수로_변환한다() {
            //given

            //when & then
            assertThat(CourseDepartment.fromCode("0000077")).isEqualTo(CourseDepartment.COMPUTER_ENGINEERING);
            assertThat(CourseDepartment.fromCode("AIA1")).isEqualTo(CourseDepartment.KOREAN_LITERATURE);
            assertThat(CourseDepartment.fromCode("XAA0")).isEqualTo(CourseDepartment.GENERAL_EDUCATION);
        }

        @Test
        void 이번_학기에_새로_생긴_학과도_변환된다() {
            //given

            //when & then
            assertThat(CourseDepartment.fromCode("0000913")).isEqualTo(CourseDepartment.GLOBAL_TRADE_SERVICE);
            assertThat(CourseDepartment.fromCode("0000912")).isEqualTo(CourseDepartment.INTELLIGENT_ROBOT_SYSTEM);
        }

        @Test
        void 정의되지_않은_코드면_예외가_발생한다() {
            //given
            final String unknownCode = "9999999";

            //when & then
            assertThatThrownBy(() -> CourseDepartment.fromCode(unknownCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }

        @Test
        void 빈_코드는_개설되지_않은_학과로_매칭되지_않는다() {
            //given
            // 무역학부, 국제개발협력연계전공은 이번 학기 개설이 없어 code가 빈 문자열이다
            final String blankCode = "";

            //when & then
            assertThatThrownBy(() -> CourseDepartment.fromCode(blankCode))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }

        @Test
        void 코드가_없는_상수도_이름으로는_찾을_수_있다() {
            //given

            //when & then
            assertThat(CourseDepartment.from("TRADE")).isEqualTo(CourseDepartment.TRADE);
            assertThat(CourseDepartment.TRADE.getCode()).isEmpty();
        }
    }

    @Nested
    class 연계전공_판정_테스트 {

        @Test
        void 연계전공이면_그대로_반환한다() {
            //given
            final String department = "SOCIAL_DATA_SCIENCE";

            //when
            final CourseDepartment courseDepartment = CourseDepartment.fromInterdisciplinary(department);

            //then
            assertThat(courseDepartment).isEqualTo(CourseDepartment.SOCIAL_DATA_SCIENCE);
        }

        @Test
        void 새로_추가된_지능형로봇시스템도_연계전공으로_인정된다() {
            //given
            final String department = "INTELLIGENT_ROBOT_SYSTEM";

            //when
            final CourseDepartment courseDepartment = CourseDepartment.fromInterdisciplinary(department);

            //then
            assertThat(courseDepartment).isEqualTo(CourseDepartment.INTELLIGENT_ROBOT_SYSTEM);
        }

        @Test
        void 연계전공이_아니면_예외가_발생한다() {
            //given
            final String department = "COMPUTER_ENGINEERING";

            //when & then
            assertThatThrownBy(() -> CourseDepartment.fromInterdisciplinary(department))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_INTERDISCIPLINARY_DEPARTMENT);
        }

        @Test
        void 존재하지_않는_학과명이면_예외가_발생한다() {
            //given
            final String department = "UNKNOWN_DEPARTMENT";

            //when & then
            assertThatThrownBy(() -> CourseDepartment.fromInterdisciplinary(department))
                    .isInstanceOf(RestApiException.class)
                    .hasFieldOrPropertyWithValue("exceptionCode", INVALID_ENUM_TYPE);
        }
    }
}
