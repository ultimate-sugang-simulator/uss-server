package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum CourseDay {
    MONDAY("1", "월"),
    TUESDAY("2", "화"),
    WEDNESDAY("3", "수"),
    THURSDAY("4", "목"),
    FRIDAY("5", "금"),
    SATURDAY("6", "토"),
    SUNDAY("", "일");

    private final String code;
    private final String name;

    public static CourseDay fromCode(final String code) {
        return Arrays.stream(values())
                .filter(day -> !day.code.isBlank())
                .filter(day -> day.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
