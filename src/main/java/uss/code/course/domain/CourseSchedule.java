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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

import static lombok.AccessLevel.PRIVATE;

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

    @Builder(access = PRIVATE)
    private CourseSchedule(
            final CourseDay dayOfWeek,
            final String periodCode,
            final String periodName,
            final String classroom,
            final LocalTime startTime,
            final LocalTime endTime
    ) {
        this.dayOfWeek = dayOfWeek;
        this.periodCode = periodCode;
        this.periodName = periodName;
        this.classroom = classroom;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static CourseSchedule create(
            final CourseDay dayOfWeek,
            final String periodCode,
            final String periodName,
            final String classroom,
            final LocalTime startTime,
            final LocalTime endTime
    ) {
        return CourseSchedule.builder()
                .dayOfWeek(dayOfWeek)
                .periodCode(periodCode)
                .periodName(periodName)
                .classroom(classroom)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    public void addCourse(final Course course) {
        this.course = course;
    }

    public boolean is75MinLesson() {
        return periodCode.startsWith(LONG_LESSON_CODE_PREFIX);
    }
}
