package uss.code.admin.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.admin.domain.Admin;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncJobStatus;
import uss.code.admin.domain.SyncPhase;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.domain.SyncStrategy;
import uss.code.course.domain.CourseTerm;

import java.time.LocalDateTime;

public class CourseSyncJobFixture {

    private static final int DEFAULT_ACADEMIC_YEAR = 2026;
    private static final CourseTerm DEFAULT_TERM = CourseTerm.SECOND;

    public static CourseSyncJob createRunningJob(final Admin executedBy) {
        return createRunningJob(executedBy, DEFAULT_ACADEMIC_YEAR, DEFAULT_TERM, SyncStrategy.UPSERT);
    }

    public static CourseSyncJob createRunningJob(
            final Admin executedBy,
            final int academicYear,
            final CourseTerm term,
            final SyncStrategy strategy
    ) {
        return createJob(executedBy, academicYear, term, strategy, LocalDateTime.now());
    }

    public static CourseSyncJob createJob(
            final Admin executedBy,
            final int academicYear,
            final CourseTerm term,
            final SyncStrategy strategy,
            final LocalDateTime startedAt
    ) {
        CourseSyncJob job = new CourseSyncJob();

        ReflectionTestUtils.setField(job, "executedBy", executedBy);
        ReflectionTestUtils.setField(job, "academicYear", academicYear);
        ReflectionTestUtils.setField(job, "term", term);
        ReflectionTestUtils.setField(job, "strategy", strategy);
        ReflectionTestUtils.setField(job, "status", SyncJobStatus.RUNNING);
        ReflectionTestUtils.setField(job, "phase", SyncPhase.COURSE_FETCH);
        ReflectionTestUtils.setField(job, "startedAt", startedAt);
        ReflectionTestUtils.setField(job, "partiallyApplied", false);

        return job;
    }

    public static CourseSyncJob createSucceededJob(
            final Admin executedBy,
            final LocalDateTime startedAt,
            final SyncResult result
    ) {
        CourseSyncJob job = createJob(executedBy, DEFAULT_ACADEMIC_YEAR, DEFAULT_TERM, SyncStrategy.UPSERT, startedAt);

        job.markFetched(result.createdCount() + result.updatedCount(), 0);
        job.succeed(result);

        return job;
    }

    public static CourseSyncJob createFailedJob(
            final Admin executedBy,
            final LocalDateTime startedAt,
            final String reason
    ) {
        CourseSyncJob job = createJob(executedBy, DEFAULT_ACADEMIC_YEAR, DEFAULT_TERM, SyncStrategy.UPSERT, startedAt);

        job.fail(reason, false);

        return job;
    }
}
