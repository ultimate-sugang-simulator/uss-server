package uss.code.course.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "courses")
public class Course {

    private static final String NO_SCHEDULE = "-";
    private static final String GROUP_PREFIX = "[";
    private static final String GROUP_SUFFIX = "]";
    private static final String CLASSROOM_DELIMITER = ":";
    private static final String PERIOD_DELIMITER = ",";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "course", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    @BatchSize(size = 1000)
    private List<CourseSchedule> schedules = new ArrayList<>();

    @Column(nullable = false, name = "academic_year")
    private int academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "term")
    private CourseTerm term;

    @Column(nullable = false, name = "title_kr")
    private String titleKr;

    @Column(nullable = false, name = "title_en")
    private String titleEn;

    @Column(nullable = false, name = "course_code")
    private String courseCode;

    @Column(nullable = false, name = "haksu_code")
    private String haksuCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "college")
    private CourseCollege college;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "department")
    private CourseDepartment department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "classification")
    private CourseClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "area")
    private CourseArea area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type")
    private CourseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "grade")
    private CourseGrade grade;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false, name = "is_english_course")
    private boolean isEnglishCourse;

    @Column(nullable = false, name = "max_capacity")
    private int maxCapacity;

    @Column(nullable = false, name = "current_enrollment")
    private int currentEnrollment;

    public String getFormattedCourseSchedules(){
        if(schedules.isEmpty())
            return NO_SCHEDULE;

        return groupByClassroom().entrySet().stream()
                .map(entry -> formatClassroomSchedules(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining());
    }

    private Map<String, List<CourseSchedule>> groupByClassroom() {
        return schedules.stream()
                .sorted(Comparator.comparing(CourseSchedule::getDayOfWeek)
                        .thenComparing(CourseSchedule::getStartTime))
                .collect(Collectors.groupingBy(
                        CourseSchedule::getClassroom,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String formatClassroomSchedules(
            final String classroom,
            final List<CourseSchedule> classroomSchedules
    ) {
        final String periods = classroomSchedules.stream()
                .map(CourseSchedule::getScheduleText)
                .collect(Collectors.joining(PERIOD_DELIMITER));

        return GROUP_PREFIX + classroom + CLASSROOM_DELIMITER + periods + GROUP_SUFFIX;
    }

    public void addCourseSchedule(final CourseSchedule courseSchedule) {
        this.schedules.add(courseSchedule);
        courseSchedule.addCourse(this);
    }

    public boolean isRegisterable(){
        return (currentEnrollment < maxCapacity);
    }

    public void incrementEnrollment(){
        currentEnrollment++;
    }

    public void decrementEnrollment(){
        currentEnrollment--;
    }
}
