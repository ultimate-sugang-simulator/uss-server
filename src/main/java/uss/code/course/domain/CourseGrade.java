package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum CourseGrade {
    FRESHMAN("1", "1학년", 1),
    SOPHOMORE("2", "2학년", 2),
    JUNIOR("3", "3학년", 3),
    SENIOR("4", "4학년", 4),
    ALL("0", "전학년", -1);

    private final String code;
    private final String name;
    private final int year;

    public static CourseGrade fromCode(final String code) {
        return Arrays.stream(values())
                .filter(grade -> !grade.code.isBlank())
                .filter(grade -> grade.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
