package uss.code.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.course.domain.CourseTerm;

import java.time.Duration;
import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static uss.code.admin.domain.SyncJobStatus.FAILED;
import static uss.code.admin.domain.SyncJobStatus.RUNNING;
import static uss.code.admin.domain.SyncJobStatus.SUCCESS;
import static uss.code.admin.domain.SyncPhase.COURSE_FETCH;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "course_sync_jobs")
public class CourseSyncJob {

    private static final int FAILURE_REASON_MAX_LENGTH = 1000;
    private static final String UNKNOWN_FAILURE_REASON = "알 수 없는 오류";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "admin_id")
    private Admin executedBy;

    @Column(nullable = false, name = "academic_year")
    private int academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "term")
    private CourseTerm term;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "strategy")
    private SyncStrategy strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private SyncJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase")
    private SyncPhase phase;

    @Column(nullable = false, name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "fetched_course_count")
    private Integer fetchedCourseCount;

    @Column(name = "fetched_schedule_count")
    private Integer fetchedScheduleCount;

    @Column(name = "created_count")
    private Integer createdCount;

    @Column(name = "updated_count")
    private Integer updatedCount;

    @Column(name = "closed_count")
    private Integer closedCount;

    @Column(name = "warning_count")
    private Integer warningCount;

    @Column(nullable = false, name = "partially_applied")
    private boolean partiallyApplied;

    @Column(name = "failure_reason")
    private String failureReason;

    @Builder(access = PRIVATE)
    private CourseSyncJob(
            final Admin executedBy,
            final int academicYear,
            final CourseTerm term,
            final SyncStrategy strategy
    ) {
        this.executedBy = executedBy;
        this.academicYear = academicYear;
        this.term = term;
        this.strategy = strategy;
        this.status = RUNNING;
        this.phase = COURSE_FETCH;
        this.startedAt = LocalDateTime.now();
        this.partiallyApplied = false;
    }

    public static CourseSyncJob start(
            final Admin executedBy,
            final int academicYear,
            final CourseTerm term,
            final SyncStrategy strategy
    ) {
        return CourseSyncJob.builder()
                .executedBy(executedBy)
                .academicYear(academicYear)
                .term(term)
                .strategy(strategy)
                .build();
    }

    public void changePhase(final SyncPhase phase) {
        this.phase = phase;
    }

    public void markFetched(
            final int fetchedCourseCount,
            final int fetchedScheduleCount
    ) {
        this.fetchedCourseCount = fetchedCourseCount;
        this.fetchedScheduleCount = fetchedScheduleCount;
    }

    public void succeed(final SyncResult result) {
        this.status = SUCCESS;
        this.phase = null;
        this.createdCount = result.createdCount();
        this.updatedCount = result.updatedCount();
        this.closedCount = result.closedCount();
        this.warningCount = result.warningCount();
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(
            final String reason,
            final boolean partiallyApplied
    ) {
        this.status = FAILED;
        this.phase = null;
        this.failureReason = truncate(reason);
        this.partiallyApplied = partiallyApplied;
        this.finishedAt = LocalDateTime.now();
    }

    public boolean isRunning() {
        return status == RUNNING;
    }

    public boolean isSuccess() {
        return status == SUCCESS;
    }

    public Long durationSeconds() {
        if (finishedAt == null) {
            return null;
        }

        return Duration.between(startedAt, finishedAt).toSeconds();
    }

    private String truncate(final String reason) {
        if (reason == null || reason.isBlank()) {
            return UNKNOWN_FAILURE_REASON;
        }

        if (reason.length() <= FAILURE_REASON_MAX_LENGTH) {
            return reason;
        }

        return reason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }
}
