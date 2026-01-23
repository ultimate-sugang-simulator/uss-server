package uss.code.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@Table(name = "course_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "course_id")
    private Course course;

    @Column(nullable = false, name = "schedule_text")
    private String scheduleText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_day")
    private CourseDay courseDay;

    @Column(nullable = false, name = "start_time")
    private LocalTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalTime endTime;

}
