package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uss.code.admin.domain.CourseSyncDetail;
import uss.code.admin.domain.SyncChangeType;
import uss.code.admin.dto.common.ChangedFieldInfo;

import java.util.List;

import static uss.code.admin.domain.SyncChangeType.UPDATED;

public record SyncChangeResponse(
        @Schema(
                description = "학수번호",
                example = "0000018001"
        )
        String haksuCode,

        @Schema(
                description = "과목명(국문). 경고 항목은 확보에 실패해 null일 수 있다",
                example = "신소재공학실험(1)"
        )
        String courseName,

        @Schema(description = "변경된 필드 목록. 수정 항목에서만 채워진다")
        List<ChangedFieldInfo> changedFields,

        @Schema(
                description = "경고 사유. 경고 항목에서만 채워진다",
                example = "미등록 학과 코드: 0000999"
        )
        String reason
) {
    public static SyncChangeResponse from(final CourseSyncDetail detail) {
        return new SyncChangeResponse(
                detail.getHaksuCode(),
                detail.getCourseName(),
                toChangedFields(detail),
                detail.getReason()
        );
    }

    private static List<ChangedFieldInfo> toChangedFields(final CourseSyncDetail detail) {
        final SyncChangeType changeType = detail.getChangeType();

        if (changeType != UPDATED) {
            return null;
        }

        return detail.getChangedFields().stream()
                .map(ChangedFieldInfo::from)
                .toList();
    }
}
