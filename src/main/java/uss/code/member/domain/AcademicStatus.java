package uss.code.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

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
                .orElse(null);
    }
}
