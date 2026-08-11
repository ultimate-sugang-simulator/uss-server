package uss.code.admin.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admin.domain.CourseSyncDetail;
import uss.code.admin.domain.CourseSyncJob;
import uss.code.admin.domain.SyncResult;
import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;
import uss.code.admin.repository.CourseSyncDetailRepository;
import uss.code.admin.repository.CourseSyncJobRepository;
import uss.code.cart.repository.CartRepository;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseFieldChange;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseSnapshot;
import uss.code.course.domain.CourseTerm;
import uss.code.course.dto.common.CourseTermInfo;
import uss.code.course.infra.CourseScheduleFormatter;
import uss.code.course.repository.CourseRepository;
import uss.code.course.repository.CourseScheduleRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.registration.repository.RegistrationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static uss.code.admin.domain.SyncChangeType.WARNING;
import static uss.code.admin.domain.SyncStrategy.REPLACE;
import static uss.code.admin.domain.SyncStrategy.UPSERT;
import static uss.code.course.domain.CourseStatus.ACTIVE;
import static uss.code.course.domain.CourseStatus.CLOSED;
import static uss.code.global.exception.domain.ExceptionCode.SYNC_JOB_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class CourseSyncApplier {

    private static final int DEFAULT_MAX_CAPACITY = 100;
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_SCHEDULE = "schedule";

    private final CourseSyncMapper courseSyncMapper;

    private final CourseSyncJobRepository courseSyncJobRepository;
    private final CourseSyncDetailRepository courseSyncDetailRepository;

    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CartRepository cartRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    public SyncResult apply(
            final long jobId,
            final List<InuCourseResponse> courses,
            final List<InuTimetableResponse> timetables
    ) {
        deleteLoadedSemesterIfReplace(jobId);

        final CourseSyncJob job = findJob(jobId);

        final List<CourseSyncDetail> details = new ArrayList<>();

        final Map<String, CourseSnapshot> snapshots = toSnapshots(job, courses, details);
        final Map<String, List<CourseSchedule>> schedules = toSchedules(job, timetables, snapshots, details);

        final SyncResult result = applyByStrategy(job, snapshots, schedules, details);

        courseSyncDetailRepository.saveAll(details);

        return result;
    }

    private Map<String, CourseSnapshot> toSnapshots(
            final CourseSyncJob job,
            final List<InuCourseResponse> courses,
            final List<CourseSyncDetail> details
    ) {
        final Map<String, CourseSnapshot> snapshots = new LinkedHashMap<>();

        for (final InuCourseResponse course : courses) {
            try {
                final CourseSnapshot snapshot = courseSyncMapper.toSnapshot(course, job.getAcademicYear(), job.getTerm());
                snapshots.put(snapshot.haksuCode(), snapshot);
            } catch (final CourseMappingException e) {
                details.add(CourseSyncDetail.warning(job, course.haksuCode(), course.titleKr(), e.getMessage()));
            }
        }

        return snapshots;
    }

    private Map<String, List<CourseSchedule>> toSchedules(
            final CourseSyncJob job,
            final List<InuTimetableResponse> timetables,
            final Map<String, CourseSnapshot> snapshots,
            final List<CourseSyncDetail> details
    ) {
        final Map<String, List<CourseSchedule>> schedules = new LinkedHashMap<>();

        for (final InuTimetableResponse timetable : timetables) {
            final CourseSnapshot snapshot = snapshots.get(timetable.haksuCode());

            if (snapshot == null) {
                continue;
            }

            try {
                schedules.computeIfAbsent(timetable.haksuCode(), key -> new ArrayList<>())
                        .add(courseSyncMapper.toSchedule(timetable));
            } catch (final CourseMappingException e) {
                details.add(CourseSyncDetail.warning(job, timetable.haksuCode(), snapshot.titleKr(), e.getMessage()));
            }
        }

        return schedules;
    }

    private SyncResult applyByStrategy(
            final CourseSyncJob job,
            final Map<String, CourseSnapshot> snapshots,
            final Map<String, List<CourseSchedule>> schedules,
            final List<CourseSyncDetail> details
    ) {
        if (job.getStrategy() == UPSERT) {
            return upsert(job, snapshots, schedules, details);
        }

        return createAll(job, snapshots, schedules, details);
    }

    private void deleteLoadedSemesterIfReplace(final long jobId) {
        if (findJob(jobId).getStrategy() != REPLACE) {
            return;
        }

        for (final CourseTermInfo semester : courseRepository.findTerms()) {
            final int academicYear = semester.academicYear();
            final CourseTerm term = semester.term();

            registrationRepository.deleteBySemester(academicYear, term);
            cartRepository.deleteBySemester(academicYear, term);
            courseScheduleRepository.deleteBySemester(academicYear, term);
            courseRepository.deleteBySemester(academicYear, term);
        }
    }

    private CourseSyncJob findJob(final long jobId) {
        return courseSyncJobRepository.findById(jobId)
                .orElseThrow(() -> new RestApiException(SYNC_JOB_NOT_FOUND));
    }

    private SyncResult createAll(
            final CourseSyncJob job,
            final Map<String, CourseSnapshot> snapshots,
            final Map<String, List<CourseSchedule>> schedules,
            final List<CourseSyncDetail> details
    ) {
        final List<Course> created = new ArrayList<>();

        for (final CourseSnapshot snapshot : snapshots.values()) {
            created.add(toNewCourse(snapshot, schedules));
            details.add(CourseSyncDetail.created(job, snapshot.haksuCode(), snapshot.titleKr()));
        }

        courseRepository.saveAll(created);

        return SyncResult.of(created.size(), 0, 0, countWarnings(details));
    }

    private SyncResult upsert(
            final CourseSyncJob job,
            final Map<String, CourseSnapshot> snapshots,
            final Map<String, List<CourseSchedule>> schedules,
            final List<CourseSyncDetail> details
    ) {
        final Map<String, Course> remaining = toCoursesByHaksuCode(job);

        final List<Course> created = new ArrayList<>();
        int updatedCount = 0;

        for (final CourseSnapshot snapshot : snapshots.values()) {
            final Course course = remaining.remove(snapshot.haksuCode());

            if (course == null) {
                created.add(toNewCourse(snapshot, schedules));
                details.add(CourseSyncDetail.created(job, snapshot.haksuCode(), snapshot.titleKr()));
                continue;
            }

            final List<CourseFieldChange> changes = updateCourse(course, snapshot, schedules);

            if (changes.isEmpty()) {
                continue;
            }

            details.add(CourseSyncDetail.updated(job, snapshot.haksuCode(), snapshot.titleKr(), changes));
            updatedCount++;
        }

        courseRepository.saveAll(created);

        final int closedCount = closeMissingCourses(job, remaining.values(), details);

        return SyncResult.of(created.size(), updatedCount, closedCount, countWarnings(details));
    }

    private Map<String, Course> toCoursesByHaksuCode(final CourseSyncJob job) {
        final Map<String, Course> courses = new LinkedHashMap<>();

        courseRepository.findAllBySemesterWithSchedules(job.getAcademicYear(), job.getTerm())
                .forEach(course -> courses.put(course.getHaksuCode(), course));

        return courses;
    }

    private List<CourseFieldChange> updateCourse(
            final Course course,
            final CourseSnapshot snapshot,
            final Map<String, List<CourseSchedule>> schedules
    ) {
        final List<CourseFieldChange> changes = course.applyUpdate(snapshot);

        if (!course.isActive()) {
            changes.add(CourseFieldChange.of(FIELD_STATUS, CLOSED.name(), ACTIVE.name()));
            course.reopen();
        }

        final List<CourseSchedule> updatedSchedules = schedules.getOrDefault(snapshot.haksuCode(), List.of());
        final String before = CourseScheduleFormatter.format(course.getSchedules());
        final String after = CourseScheduleFormatter.format(updatedSchedules);

        if (!before.equals(after)) {
            changes.add(CourseFieldChange.of(FIELD_SCHEDULE, before, after));
            course.replaceSchedules(updatedSchedules);
        }

        return changes;
    }

    private int closeMissingCourses(
            final CourseSyncJob job,
            final Iterable<Course> missingCourses,
            final List<CourseSyncDetail> details
    ) {
        int closedCount = 0;

        for (final Course course : missingCourses) {
            if (!course.isActive()) {
                continue;
            }

            course.close();
            details.add(CourseSyncDetail.closed(job, course.getHaksuCode(), course.getTitleKr()));
            closedCount++;
        }

        return closedCount;
    }

    private Course toNewCourse(
            final CourseSnapshot snapshot,
            final Map<String, List<CourseSchedule>> schedules
    ) {
        final Course course = Course.create(snapshot, DEFAULT_MAX_CAPACITY);

        schedules.getOrDefault(snapshot.haksuCode(), List.of())
                .forEach(course::addCourseSchedule);

        return course;
    }

    private int countWarnings(final List<CourseSyncDetail> details) {
        return (int) details.stream()
                .filter(detail -> detail.getChangeType() == WARNING)
                .count();
    }
}
