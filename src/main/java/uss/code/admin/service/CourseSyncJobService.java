package uss.code.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncPhase;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.global.exception.domain.RestApiException;

import static uss.code.global.exception.domain.ExceptionCode.SYNC_JOB_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CourseSyncJobService {

    private final CourseSyncJobRepository courseSyncJobRepository;

    @Transactional
    public void changePhase(
            final long jobId,
            final SyncPhase phase
    ) {
        findJob(jobId).changePhase(phase);
    }

    @Transactional
    public void markFetched(
            final long jobId,
            final int fetchedCourseCount,
            final int fetchedScheduleCount
    ) {
        findJob(jobId).markFetched(fetchedCourseCount, fetchedScheduleCount);
    }

    @Transactional
    public void succeed(
            final long jobId,
            final SyncResult result
    ) {
        findJob(jobId).succeed(result);
    }

    @Transactional
    public void fail(
            final long jobId,
            final String reason,
            final boolean partiallyApplied
    ) {
        findJob(jobId).fail(reason, partiallyApplied);
    }

    private CourseSyncJob findJob(final long jobId) {
        return courseSyncJobRepository.findById(jobId)
                .orElseThrow(() -> new RestApiException(SYNC_JOB_NOT_FOUND));
    }
}
