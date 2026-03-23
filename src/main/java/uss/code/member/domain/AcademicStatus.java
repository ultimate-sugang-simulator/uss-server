package uss.code.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum AcademicStatus {
    ENROLLED("재학"),
    LEAVE_OF_ABSENCE("휴학");

    private final String name;

    public static AcademicStatus from(String value){
        return Arrays.stream(values())
                .filter(as -> as.name().equalsIgnoreCase(value))
                .findAny()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
