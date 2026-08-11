package uss.code.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import uss.code.course.domain.CourseTerm;

public record SyncPreflightRequest(
        @Schema(
                description = "적재 대상 학년도",
                example = "2026"
        )
        @NotNull(message = "학년도가 비어있습니다.")
        @Min(value = 1000, message = "학년도는 4자리 정수여야 합니다.")
        @Max(value = 9999, message = "학년도는 4자리 정수여야 합니다.")
        Integer academicYear,

        @Schema(
                description = "적재 대상 학기",
                example = "SUMMER"
        )
        @NotNull(message = "학기가 비어있습니다.")
        CourseTerm term
) {}
