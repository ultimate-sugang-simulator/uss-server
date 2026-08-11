package uss.code.admin.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncJobStatus;

import java.time.LocalDateTime;

public record LastJobInfo(
        @Schema(
                description = "작업 아이디",
                example = "40"
        )
        long jobId,

        @Schema(
                description = "작업 상태",
                example = "SUCCESS"
        )
        SyncJobStatus status,

        @Schema(
                description = "시작 시각",
                example = "2026-08-05T14:22:00"
        )
        LocalDateTime startedAt,

        @Schema(
                description = "생성된 강의 수. 성공이 아니면 null",
                example = "12"
        )
        Integer createdCount,

        @Schema(
                description = "수정된 강의 수. 성공이 아니면 null",
                example = "45"
        )
        Integer updatedCount,

        @Schema(
                description = "폐강된 강의 수. 성공이 아니면 null",
                example = "3"
        )
        Integer closedCount
) {
    public static LastJobInfo from(final CourseSyncJob job) {
        return new LastJobInfo(
                job.getId(),
                job.getStatus(),
                job.getStartedAt(),
                job.getCreatedCount(),
                job.getUpdatedCount(),
                job.getClosedCount()
        );
    }
}
