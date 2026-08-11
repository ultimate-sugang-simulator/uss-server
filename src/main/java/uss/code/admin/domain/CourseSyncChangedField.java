package uss.code.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import uss.code.course.domain.CourseFieldChange;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "course_sync_changed_fields")
public class CourseSyncChangedField {

    private static final int VALUE_MAX_LENGTH = 500;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "detail_id")
    private CourseSyncDetail detail;

    @Column(nullable = false, name = "field")
    private String field;

    @Column(nullable = false, name = "before_value")
    private String beforeValue;

    @Column(nullable = false, name = "after_value")
    private String afterValue;

    @Builder(access = PRIVATE)
    private CourseSyncChangedField(
            final CourseSyncDetail detail,
            final String field,
            final String beforeValue,
            final String afterValue
    ) {
        this.detail = detail;
        this.field = field;
        this.beforeValue = truncate(beforeValue);
        this.afterValue = truncate(afterValue);
    }

    public static CourseSyncChangedField create(
            final CourseSyncDetail detail,
            final CourseFieldChange change
    ) {
        return CourseSyncChangedField.builder()
                .detail(detail)
                .field(change.field())
                .beforeValue(change.before())
                .afterValue(change.after())
                .build();
    }

    private String truncate(final String value) {
        if (value == null) {
            return "";
        }

        if (value.length() <= VALUE_MAX_LENGTH) {
            return value;
        }

        return value.substring(0, VALUE_MAX_LENGTH);
    }
}
