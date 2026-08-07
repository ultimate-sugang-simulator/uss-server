package uss.code.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DepartmentUpdateRequest(
        @Schema(
                description = "변경할 학과",
                example = "COMPUTER_ENGINEERING"
        )
        @NotBlank(message = "학과가 비어있습니다.")
        String department
) {}
