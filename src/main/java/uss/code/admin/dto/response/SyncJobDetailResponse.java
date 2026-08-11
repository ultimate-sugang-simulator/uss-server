package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncJobStatus;
import uss.code.admin.domain.SyncStrategy;
import uss.code.admin.dto.common.SyncProgress;
import uss.code.course.domain.CourseTerm;

import java.time.LocalDateTime;

public record SyncJobDetailResponse(
        @Schema(
                description = "작업 아이디",
                example = "41"
        )
        long jobId,

        @Schema(
                description = "대상 학년도",
                example = "2026"
        )
        int academicYear,

        @Schema(
                description = "대상 학기",
                example = "SECOND"
        )
        CourseTerm term,

        @Schema(
                description = "적재 전략",
                example = "UPSERT"
        )
        SyncStrategy strategy,

        @Schema(
                description = "작업 상태",
                example = "RUNNING"
        )
        SyncJobStatus status,

        @Schema(
                description = "실행한 관리자 이름",
                example = "김학사"
        )
        String executedBy,

        @Schema(
                description = "시작 시각",
                example = "2026-08-05T14:22:00"
        )
        LocalDateTime startedAt,

        @Schema(
                description = "종료 시각. 진행 중이면 null",
                example = "2026-08-05T14:24:10"
        )
        LocalDateTime finishedAt,

        @Schema(
                description = "소요 시간(초). 진행 중이면 null",
                example = "130"
        )
        Long durationSeconds,

        @Schema(
                description = "학교 API에서 수집한 강의 건수. 성공에서만 채워진다",
                example = "2439"
        )
        Integer fetchedCourseCount,

        @Schema(
                description = "학교 API에서 수집한 시간표 건수. 성공에서만 채워진다",
                example = "9109"
        )
        Integer fetchedScheduleCount,

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
        Integer closedCount,

        @Schema(
                description = "경고 건수. 성공이 아니면 null",
                example = "1"
        )
        Integer warningCount,

        @Schema(description = "진행 상황. 진행 중일 때만 채워진다")
        SyncProgress progress,

        @Schema(
                description = "적재 도중 실패해 일부만 반영된 상태인지 여부",
                example = "false"
        )
        boolean partiallyApplied,

        @Schema(
                description = "실패 사유. 실패에서만 채워진다",
                example = "연계 API 호출에 실패했습니다."
        )
        String failureReason
) {
    public static SyncJobDetailResponse from(final CourseSyncJob job) {
        return new SyncJobDetailResponse(
                job.getId(),
                job.getAcademicYear(),
                job.getTerm(),
                job.getStrategy(),
                job.getStatus(),
                job.getExecutedBy().getName(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.durationSeconds(),
                job.getFetchedCourseCount(),
                job.getFetchedScheduleCount(),
                job.getCreatedCount(),
                job.getUpdatedCount(),
                job.getClosedCount(),
                job.getWarningCount(),
                toProgress(job),
                job.isPartiallyApplied(),
                job.getFailureReason()
        );
    }

    private static SyncProgress toProgress(final CourseSyncJob job) {
        if (!job.isRunning()) {
            return null;
        }

        return SyncProgress.of(job.getPhase());
    }
}
