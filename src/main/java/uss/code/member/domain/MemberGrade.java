package uss.code.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MemberGrade {
    FRESHMAN("1학년", 1),
    SOPHOMORE("2학년", 2),
    JUNIOR("3학년", 3),
    SENIOR("4학년", 4);

    private final String name;
    private final int year;

    public static MemberGrade from(String value){
        return Arrays.stream(values())
                .filter(mg -> mg.name().equalsIgnoreCase(value))
                .findAny()
                .orElse(null);
    }
}
