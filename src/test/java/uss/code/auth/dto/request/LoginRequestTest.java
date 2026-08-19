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

    private boolean isValid(final String email) {
        return validator.validate(new LoginRequest(email, VALID_PASSWORD)).isEmpty();
    }

    @Nested
    class 이메일_형식_검증_테스트 {

        @Test
        void 학교_이메일이면_통과한다() {
            assertThat(isValid("student@inu.ac.kr")).isTrue();
        }

        @Test
        void 일반_도메인_이메일도_통과한다() {
            assertThat(isValid("user123@gmail.com")).isTrue();
        }

        @Test
        void 골뱅이가_없으면_실패한다() {
            assertThat(isValid("student.inu.ac.kr")).isFalse();
        }

        @Test
        void 공백이_들어가면_실패한다() {
            assertThat(isValid("stu dent@inu.ac.kr")).isFalse();
        }

        @Test
        void 비어있으면_실패한다() {
            assertThat(isValid("")).isFalse();
        }
    }
}
