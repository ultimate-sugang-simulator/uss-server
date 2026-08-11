package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.SystemSemester;
import uss.code.course.domain.CourseTerm;

public record SystemSemesterResponse(
        @Schema(
                description = "표시 학년도",
                example = "2026"
        )
        int academicYear,

        @Schema(
                description = "표시 학기",
                example = "SECOND"
        )
        CourseTerm term
) {
    public static SystemSemesterResponse from(final SystemSemester systemSemester) {
        return new SystemSemesterResponse(
                systemSemester.getAcademicYear(),
                systemSemester.getTerm()
        );
    }
}
