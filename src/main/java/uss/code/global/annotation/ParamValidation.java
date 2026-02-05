package uss.code.global.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uss.code.global.infra.ParamValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = ParamValidator.class)
@Target(value = {METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface ParamValidation {
    String message() default "유효하지 않은 파라미터입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minLength() default 1;

    int maxLength() default Integer.MAX_VALUE;
}
