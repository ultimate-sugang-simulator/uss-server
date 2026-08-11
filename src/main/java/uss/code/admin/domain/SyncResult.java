package uss.code.admin.domain;

public record SyncResult(
        int createdCount,

        int updatedCount,

        int closedCount,

        int warningCount
) {
    public static SyncResult of(
            final int createdCount,
            final int updatedCount,
            final int closedCount,
            final int warningCount
    ) {
        return new SyncResult(createdCount, updatedCount, closedCount, warningCount);
    }
}
