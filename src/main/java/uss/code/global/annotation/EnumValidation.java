package uss.code.global.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uss.code.global.util.EnumValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = EnumValidator.class)
@Target(value = {METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface EnumValidation {
    String message() default "유효하지 않은 열거 타입의 값입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends Enum<?>> target();
}
