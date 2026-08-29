package uss.code.admin.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;
import uss.code.admin.event.CourseSyncJobCreatedEvent;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.admin.service.CourseSyncJobService;
import uss.code.course.domain.CourseTerm;

import java.util.List;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import static uss.code.admin.domain.SyncPhase.PERSIST;
import static uss.code.admin.domain.SyncPhase.TIMETABLE_FETCH;

@Log4j2
@Component
@RequiredArgsConstructor
public class CourseSyncExecutor {

    private final InuCourseApiClient inuCourseApiClient;
    private final CourseSyncApplier courseSyncApplier;

    private final CourseSyncJobService courseSyncJobService;
    private final CourseSyncJobRepository courseSyncJobRepository;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void execute(final CourseSyncJobCreatedEvent event) {
        final long jobId = event.jobId();

        boolean applied = false;

        try {
            final CourseSyncJob job = courseSyncJobRepository.findById(jobId).orElseThrow();
            final int academicYear = job.getAcademicYear();
            final CourseTerm term = job.getTerm();

            final List<InuCourseResponse> courses = inuCourseApiClient.fetchCourses(academicYear, term);

            courseSyncJobService.changePhase(jobId, TIMETABLE_FETCH);
            final List<InuTimetableResponse> timetables = inuCourseApiClient.fetchTimetables(academicYear, term);

            courseSyncJobService.markFetched(jobId, courses.size(), timetables.size());
            courseSyncJobService.changePhase(jobId, PERSIST);

            final SyncResult result = courseSyncApplier.apply(jobId, courses, timetables);
            applied = true;

            courseSyncJobService.succeed(jobId, result);
        } catch (final Exception e) {
            log.error("Course sync failed. jobId={}, message={}", jobId, e.getMessage(), e);
            courseSyncJobService.fail(jobId, e.getMessage(), applied);
        }
    }
}
