package uss.code.admin.event;

public record CourseSyncJobCreatedEvent(
        long jobId
) {
    public static CourseSyncJobCreatedEvent of(final long jobId) {
        return new CourseSyncJobCreatedEvent(jobId);
    }
}
