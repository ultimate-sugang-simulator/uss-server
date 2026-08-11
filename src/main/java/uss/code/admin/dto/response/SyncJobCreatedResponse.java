package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SyncJobCreatedResponse(
        @Schema(
                description = "생성된 작업 아이디",
                example = "41"
        )
        long jobId
) {
    public static SyncJobCreatedResponse of(final long jobId) {
        return new SyncJobCreatedResponse(jobId);
    }
}
