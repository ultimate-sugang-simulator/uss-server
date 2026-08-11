package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.dto.common.LastJobInfo;
import uss.code.admin.dto.common.SemesterRef;

public record CourseSummaryResponse(
        @Schema(description = "적재된 학기. 강의가 하나도 없으면 null")
        SemesterRef semester,

        @Schema(
                description = "적재된 강의 수. 폐강을 포함한다",
                example = "1203"
        )
        long courseCount,

        @Schema(
                description = "적재된 시간표 수",
                example = "2847"
        )
        long scheduleCount,

        @Schema(description = "가장 최근 동기화 작업. 이력이 없으면 null")
        LastJobInfo lastJob,

        @Schema(
                description = "진행 중인 작업 아이디. 없으면 null",
                example = "41"
        )
        Long runningJobId
) {
    public static CourseSummaryResponse of(
            final SemesterRef semester,
            final long courseCount,
            final long scheduleCount,
            final LastJobInfo lastJob,
            final Long runningJobId
    ) {
        return new CourseSummaryResponse(semester, courseCount, scheduleCount, lastJob, runningJobId);
    }
}
