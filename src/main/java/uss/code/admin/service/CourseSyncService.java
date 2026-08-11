package uss.code.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admin.domain.Admin;
import uss.code.admin.domain.CourseSyncDetail;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncChangeType;
import uss.code.admin.domain.SyncStrategy;
import uss.code.admin.dto.common.SemesterRef;
import uss.code.admin.dto.common.SyncDeleteCounts;
import uss.code.admin.dto.request.SyncJobCreateRequest;
import uss.code.admin.dto.request.SyncPreflightRequest;
import uss.code.admin.dto.response.SyncChangeResponse;
import uss.code.admin.dto.response.SyncJobCreatedResponse;
import uss.code.admin.dto.response.SyncJobDetailResponse;
import uss.code.admin.dto.response.SyncJobResponse;
import uss.code.admin.dto.response.SyncPreflightResponse;
import uss.code.admin.event.CourseSyncJobCreatedEvent;
import uss.code.admin.repository.AdminRepository;
import uss.code.admin.repository.CourseSyncDetailRepository;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.cart.repository.CartRepository;
import uss.code.course.domain.CourseTerm;
import uss.code.course.dto.common.CourseTermInfo;
import uss.code.course.repository.CourseRepository;
import uss.code.course.repository.CourseScheduleRepository;
import uss.code.global.dto.response.PageResponse;
import uss.code.global.exception.domain.RestApiException;
import uss.code.registration.repository.RegistrationRepository;

import java.util.List;
import java.util.Optional;

import static uss.code.admin.domain.SyncJobStatus.RUNNING;
import static uss.code.admin.domain.SyncStrategy.INITIAL;
import static uss.code.admin.domain.SyncStrategy.REPLACE;
import static uss.code.admin.domain.SyncStrategy.UPSERT;
import static uss.code.global.exception.domain.ExceptionCode.ADMIN_NOT_FOUND;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_JOB_ALREADY_RUNNING;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_JOB_NOT_FOUND;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_STRATEGY_MISMATCH;

@Service
@RequiredArgsConstructor
public class CourseSyncService {

    private static final int PAGE_SIZE = 10;
    private static final int PAGE_NUMBER_OFFSET = 1;

    private final ApplicationEventPublisher eventPublisher;

    private final AdminRepository adminRepository;
    private final CourseSyncJobRepository courseSyncJobRepository;
    private final CourseSyncDetailRepository courseSyncDetailRepository;

    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CartRepository cartRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public SyncPreflightResponse preflight(final SyncPreflightRequest request) {
        final Optional<SemesterRef> loadedSemester = findLoadedSemester();
        final SyncStrategy strategy = judgeStrategy(loadedSemester, request.academicYear(), request.term());

        return SyncPreflightResponse.of(
                strategy,
                loadedSemester.orElse(null),
                SemesterRef.of(request.academicYear(), request.term()),
                countDeletions(strategy, loadedSemester)
        );
    }

    @Transactional
    public SyncJobCreatedResponse createJob(
            final long adminId,
            final SyncJobCreateRequest request
    ) {
        if (courseSyncJobRepository.findFirstByStatus(RUNNING).isPresent())
            throw new RestApiException(SYNC_JOB_ALREADY_RUNNING);

        final SyncStrategy strategy = judgeStrategy(findLoadedSemester(), request.academicYear(), request.term());

        if (strategy != request.expectedStrategy())
            throw new RestApiException(SYNC_STRATEGY_MISMATCH);

        final Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RestApiException(ADMIN_NOT_FOUND));

        final CourseSyncJob job = courseSyncJobRepository.save(
                CourseSyncJob.start(admin, request.academicYear(), request.term(), strategy)
        );

        eventPublisher.publishEvent(CourseSyncJobCreatedEvent.of(job.getId()));

        return SyncJobCreatedResponse.of(job.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<SyncJobResponse> getJobs(final int page) {
        final Page<CourseSyncJob> jobs = courseSyncJobRepository.findAllByOrderByStartedAtDesc(toPageable(page));

        final List<SyncJobResponse> syncJobResponses = jobs.getContent().stream()
                .map(SyncJobResponse::from)
                .toList();

        return PageResponse.of(jobs, syncJobResponses);
    }

    @Transactional(readOnly = true)
    public SyncJobDetailResponse getJob(final long jobId) {
        final CourseSyncJob job = courseSyncJobRepository.findByIdWithAdmin(jobId)
                .orElseThrow(() -> new RestApiException(SYNC_JOB_NOT_FOUND));

        return SyncJobDetailResponse.from(job);
    }

    @Transactional(readOnly = true)
    public PageResponse<SyncChangeResponse> getJobDetails(
            final long jobId,
            final SyncChangeType changeType,
            final int page
    ) {
        if (!courseSyncJobRepository.existsById(jobId))
            throw new RestApiException(SYNC_JOB_NOT_FOUND);

        final Page<CourseSyncDetail> details = courseSyncDetailRepository
                .findByJobIdAndChangeTypeOrderByHaksuCodeAsc(jobId, changeType, toPageable(page));

        final List<SyncChangeResponse> syncChangeResponses = details.getContent().stream()
                .map(SyncChangeResponse::from)
                .toList();

        return PageResponse.of(details, syncChangeResponses);
    }

    private SyncStrategy judgeStrategy(
            final Optional<SemesterRef> loadedSemester,
            final int academicYear,
            final CourseTerm term
    ) {
        if (loadedSemester.isEmpty()) {
            return INITIAL;
        }

        if (loadedSemester.get().matches(academicYear, term)) {
            return UPSERT;
        }

        return REPLACE;
    }

    private SyncDeleteCounts countDeletions(
            final SyncStrategy strategy,
            final Optional<SemesterRef> loadedSemester
    ) {
        if (strategy != REPLACE || loadedSemester.isEmpty()) {
            return SyncDeleteCounts.empty();
        }

        final SemesterRef semester = loadedSemester.get();
        final int academicYear = semester.academicYear();
        final CourseTerm term = semester.term();

        return SyncDeleteCounts.of(
                courseRepository.countBySemester(academicYear, term),
                courseScheduleRepository.countBySemester(academicYear, term),
                cartRepository.countBySemester(academicYear, term),
                registrationRepository.countBySemester(academicYear, term)
        );
    }

    private Optional<SemesterRef> findLoadedSemester() {
        final List<CourseTermInfo> loadedSemesters = courseRepository.findTerms();

        if (loadedSemesters.isEmpty()) {
            return Optional.empty();
        }

        final CourseTermInfo loaded = loadedSemesters.get(0);

        return Optional.of(SemesterRef.of(loaded.academicYear(), loaded.term()));
    }

    private Pageable toPageable(final int page) {
        return PageRequest.of(page - PAGE_NUMBER_OFFSET, PAGE_SIZE);
    }
}
