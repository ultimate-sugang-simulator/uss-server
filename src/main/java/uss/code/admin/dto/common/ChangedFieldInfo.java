package uss.code.admin.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.CourseSyncChangedField;

public record ChangedFieldInfo(
        @Schema(
                description = "변경된 필드 식별자",
                example = "schedule"
        )
        String field,

        @Schema(
                description = "변경 전 값",
                example = "[04-301:월(1-2A)]"
        )
        String before,

        @Schema(
                description = "변경 후 값",
                example = "[04-305:월(1-2A)]"
        )
        String after
) {
    public static ChangedFieldInfo from(final CourseSyncChangedField changedField) {
        return new ChangedFieldInfo(
                changedField.getField(),
                changedField.getBeforeValue(),
                changedField.getAfterValue()
        );
    }
}
