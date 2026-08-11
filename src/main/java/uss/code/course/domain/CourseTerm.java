package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;
import java.util.Optional;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum CourseTerm {
    FIRST("10", "1학기"),
    SECOND("20", "2학기"),
    SUMMER("30", "여름계절학기"),
    WINTER("40", "겨울계절학기");

    private final String code;
    private final String name;

    public static Optional<CourseTerm> tryFromCode(final String code) {
        return Arrays.stream(values())
                .filter(term -> !term.code.isBlank())
                .filter(term -> term.code.equals(code))
                .findFirst();
    }

    public static CourseTerm fromCode(final String code) {
        return tryFromCode(code)
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
