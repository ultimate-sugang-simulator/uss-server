package uss.code.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.course.domain.CourseTerm;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "system_semesters")
public class SystemSemester {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "academic_year")
    private int academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "term")
    private CourseTerm term;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = PRIVATE)
    private SystemSemester(
            final int academicYear,
            final CourseTerm term
    ) {
        this.academicYear = academicYear;
        this.term = term;
        this.updatedAt = LocalDateTime.now();
    }

    public static SystemSemester create(
            final int academicYear,
            final CourseTerm term
    ) {
        return SystemSemester.builder()
                .academicYear(academicYear)
                .term(term)
                .build();
    }

    public void change(
            final int academicYear,
            final CourseTerm term
    ) {
        this.academicYear = academicYear;
        this.term = term;
        this.updatedAt = LocalDateTime.now();
    }
}
