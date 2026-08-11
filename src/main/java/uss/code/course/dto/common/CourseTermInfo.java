package uss.code.course.dto.common;

import uss.code.course.domain.CourseTerm;

public record CourseTermInfo(
        int academicYear,
        CourseTerm term
) {
}
