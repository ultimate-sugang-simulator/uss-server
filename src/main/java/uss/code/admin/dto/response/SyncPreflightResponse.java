package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.SyncStrategy;
import uss.code.admin.dto.common.SemesterRef;
import uss.code.admin.dto.common.SyncDeleteCounts;

public record SyncPreflightResponse(
        @Schema(
                description = "판정된 적재 전략",
                example = "REPLACE"
        )
        SyncStrategy strategy,

        @Schema(description = "현재 적재된 학기. 최초 적재면 null")
        SemesterRef currentSemester,

        @Schema(description = "요청한 대상 학기")
        SemesterRef targetSemester,

        @Schema(description = "삭제 예정 건수. REPLACE가 아니면 전부 0")
        SyncDeleteCounts deleteCounts
) {
    public static SyncPreflightResponse of(
            final SyncStrategy strategy,
            final SemesterRef currentSemester,
            final SemesterRef targetSemester,
            final SyncDeleteCounts deleteCounts
    ) {
        return new SyncPreflightResponse(strategy, currentSemester, targetSemester, deleteCounts);
    }
}
