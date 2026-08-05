package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum CourseClassification {
    MAJOR_ADVANCED("41", "전공심화"),
    MAJOR_BASIC("25", "전공기초"),
    MAJOR_CORE("31", "전공핵심"),
    BASIC_LIBERAL_ARTS("11", "기초교양"),
    CORE_LIBERAL_ARTS("21", "핵심교양"),
    ADVANCED_LIBERAL_ARTS("23", "심화교양"),
    TEACHING("50", "교직"),
    GENERAL_ELECTIVE("80", "일반선택"),
    MILITARY("70", "군사학");

    private final String code;
    private final String name;

    public static CourseClassification fromCode(final String code) {
        return Arrays.stream(values())
                .filter(classification -> !classification.code.isBlank())
                .filter(classification -> classification.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
