package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseGrade {
    FRESHMAN("1학년", 1),
    SOPHOMORE("2학년", 2),
    JUNIOR("3학년", 3),
    SENIOR("4학년", 4),
    ALL("전학년", -1);

    private final String name;
    private final int year;
}
