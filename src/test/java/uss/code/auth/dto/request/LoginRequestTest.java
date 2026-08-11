package uss.code.auth.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private static final String VALID_PASSWORD = "password123";

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private boolean isValid(final String studentId) {
        return validator.validate(new LoginRequest(studentId, VALID_PASSWORD)).isEmpty();
    }

    @Nested
    class 학번_형식_검증_테스트 {

        @Test
        void 기존_9자리_숫자_학번은_통과한다() {
            assertThat(isValid("202012345")).isTrue();
        }

        @Test
        void 영문자가_섞여도_통과한다() {
            assertThat(isValid("a2020b12345")).isTrue();
        }

        @Test
        void 영문자로만_이뤄져도_통과한다() {
            assertThat(isValid("teststudent")).isTrue();
        }

        @Test
        void 스무자면_통과한다() {
            assertThat(isValid("a1234567890123456789")).isTrue();
        }

        @Test
        void 스물한자면_실패한다() {
            assertThat(isValid("a12345678901234567890")).isFalse();
        }

        @Test
        void 비어있으면_실패한다() {
            assertThat(isValid("")).isFalse();
        }

        @Test
        void 특수문자가_들어가면_실패한다() {
            assertThat(isValid("2020-12345")).isFalse();
        }

        @Test
        void 공백이_들어가면_실패한다() {
            assertThat(isValid("2020 12345")).isFalse();
        }
    }
}
