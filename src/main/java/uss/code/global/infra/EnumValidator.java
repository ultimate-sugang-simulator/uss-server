package uss.code.global.infra;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uss.code.global.annotation.EnumValidation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<EnumValidation, String> {
    private Set<String> validValues;

    @Override
    public void initialize(EnumValidation constraintAnnotation) {
        this.validValues = Arrays.stream(constraintAnnotation.target().getEnumConstants())
                .map(e -> e.name().toUpperCase())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(
            final String value,
            final ConstraintValidatorContext context
    ) {
        if (value == null)
            return true;

        return validValues.contains(value.toUpperCase());
    }
}
