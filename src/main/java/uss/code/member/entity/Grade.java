package uss.code.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {
    FRESHMAN("1학년", 1),
    SOPHOMORE("2학년", 2),
    JUNIOR("3학년", 3),
    SENIOR("4학년", 4);

    private final String name;
    private final int year;
}
