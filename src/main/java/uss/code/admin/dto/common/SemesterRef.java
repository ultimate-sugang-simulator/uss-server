package uss.code.admin.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.course.domain.CourseTerm;

public record SemesterRef(
        @Schema(
                description = "학년도",
                example = "2026"
        )
        int academicYear,

        @Schema(
                description = "학기",
                example = "SECOND"
        )
        CourseTerm term
) {
    public static SemesterRef of(
            final int academicYear,
            final CourseTerm term
    ) {
        return new SemesterRef(academicYear, term);
    }

    public boolean matches(
            final int academicYear,
            final CourseTerm term
    ) {
        return this.academicYear == academicYear && this.term == term;
    }
}
