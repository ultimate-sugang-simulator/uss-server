package uss.code.admin.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

public record SyncDeleteCounts(
        @Schema(
                description = "삭제 예정 강의 수",
                example = "1203"
        )
        long courses,

        @Schema(
                description = "삭제 예정 시간표 수",
                example = "2847"
        )
        long schedules,

        @Schema(
                description = "삭제 예정 장바구니 수",
                example = "87"
        )
        long carts,

        @Schema(
                description = "삭제 예정 수강신청 수",
                example = "41"
        )
        long registrations
) {
    private static final long NOTHING = 0L;

    public static SyncDeleteCounts of(
            final long courses,
            final long schedules,
            final long carts,
            final long registrations
    ) {
        return new SyncDeleteCounts(courses, schedules, carts, registrations);
    }

    public static SyncDeleteCounts empty() {
        return new SyncDeleteCounts(NOTHING, NOTHING, NOTHING, NOTHING);
    }
}
