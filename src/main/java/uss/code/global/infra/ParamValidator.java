package uss.code.global.infra;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uss.code.global.annotation.ParamValidation;

public class ParamValidator implements ConstraintValidator<ParamValidation, String> {
    private int minLength;
    private int maxLength;

    @Override
    public void initialize(ParamValidation constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(
            final String value,
            final ConstraintValidatorContext context
    ) {
        if (value == null)
            return true;

        if (value.isBlank())
            return false;

        final String trimmed = value.trim();
        return trimmed.length() >= minLength && trimmed.length() <= maxLength;
    }
}
