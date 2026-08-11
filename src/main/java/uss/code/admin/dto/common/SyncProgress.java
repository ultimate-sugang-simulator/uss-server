package uss.code.admin.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.SyncPhase;

public record SyncProgress(
        @Schema(
                description = "진행 단계",
                example = "COURSE_FETCH"
        )
        SyncPhase phase
) {
    public static SyncProgress of(final SyncPhase phase) {
        return new SyncProgress(phase);
    }
}
