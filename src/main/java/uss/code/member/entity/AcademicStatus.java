package uss.code.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcademicStatus {
    ENROLLED("재학"),
    LEAVE_OF_ABSENCE("휴학");

    private final String name;
}
