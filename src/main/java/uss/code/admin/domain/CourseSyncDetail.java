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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import uss.code.course.domain.CourseFieldChange;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static lombok.AccessLevel.PRIVATE;
import static uss.code.admin.domain.SyncChangeType.CLOSED;
import static uss.code.admin.domain.SyncChangeType.CREATED;
import static uss.code.admin.domain.SyncChangeType.UPDATED;
import static uss.code.admin.domain.SyncChangeType.WARNING;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "course_sync_details")
public class CourseSyncDetail {

    private static final int REASON_MAX_LENGTH = 255;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "job_id")
    private CourseSyncJob job;

    @OneToMany(mappedBy = "detail", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<CourseSyncChangedField> changedFields = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "change_type")
    private SyncChangeType changeType;

    @Column(nullable = false, name = "haksu_code")
    private String haksuCode;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "reason")
    private String reason;

    @Builder(access = PRIVATE)
    private CourseSyncDetail(
            final CourseSyncJob job,
            final SyncChangeType changeType,
            final String haksuCode,
            final String courseName,
            final String reason
    ) {
        this.job = job;
        this.changeType = changeType;
        this.haksuCode = haksuCode;
        this.courseName = courseName;
        this.reason = truncate(reason);
    }

    public static CourseSyncDetail created(
            final CourseSyncJob job,
            final String haksuCode,
            final String courseName
    ) {
        return CourseSyncDetail.builder()
                .job(job)
                .changeType(CREATED)
                .haksuCode(haksuCode)
                .courseName(courseName)
                .build();
    }

    public static CourseSyncDetail updated(
            final CourseSyncJob job,
            final String haksuCode,
            final String courseName,
            final List<CourseFieldChange> changes
    ) {
        final CourseSyncDetail detail = CourseSyncDetail.builder()
                .job(job)
                .changeType(UPDATED)
                .haksuCode(haksuCode)
                .courseName(courseName)
                .build();

        changes.forEach(detail::addChangedField);

        return detail;
    }

    public static CourseSyncDetail closed(
            final CourseSyncJob job,
            final String haksuCode,
            final String courseName
    ) {
        return CourseSyncDetail.builder()
                .job(job)
                .changeType(CLOSED)
                .haksuCode(haksuCode)
                .courseName(courseName)
                .build();
    }

    public static CourseSyncDetail warning(
            final CourseSyncJob job,
            final String haksuCode,
            final String courseName,
            final String reason
    ) {
        return CourseSyncDetail.builder()
                .job(job)
                .changeType(WARNING)
                .haksuCode(haksuCode)
                .courseName(courseName)
                .reason(reason)
                .build();
    }

    private void addChangedField(final CourseFieldChange change) {
        this.changedFields.add(CourseSyncChangedField.create(this, change));
    }

    private String truncate(final String reason) {
        if (reason == null || reason.length() <= REASON_MAX_LENGTH) {
            return reason;
        }

        return reason.substring(0, REASON_MAX_LENGTH);
    }
}
