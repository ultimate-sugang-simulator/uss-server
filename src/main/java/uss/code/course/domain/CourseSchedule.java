package uss.code.course.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "course_schedules")
public class CourseSchedule {

    private static final String LONG_LESSON_CODE_PREFIX = "B";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "day_of_week")
    private CourseDay dayOfWeek;

    @Column(nullable = false, name = "period_code")
    private String periodCode;

    @Column(nullable = false, name = "period_name")
    private String periodName;

    @Column(nullable = false, name = "classroom")
    private String classroom;

    @Column(nullable = false, name = "start_time")
    private LocalTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalTime endTime;

    public void addCourse(final Course course) {
        this.course = course;
    }

    public boolean is75MinLesson() {
        return periodCode.startsWith(LONG_LESSON_CODE_PREFIX);
    }
}
