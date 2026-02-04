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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "courses")
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "course", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<CourseSchedule> courseSchedules = new ArrayList<>();

    @Column(nullable = false, name = "title_kr")
    private String titleKr;

    @Column(nullable = false, name = "title_en")
    private String titleEn;

    @Column(nullable = false, unique = true, name = "course_code")
    private String courseCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_college")
    private CourseCollege courseCollege;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_department")
    private CourseDepartment courseDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_classification")
    private CourseClassification courseClassification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_area")
    private CourseArea courseArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_type")
    private CourseType courseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "course_grade")
    private CourseGrade courseGrade;

    @Column(name = "professor_name")
    private String professorName;

    @Column
    private String classroom;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false, name = "is_english_course")
    private boolean isEnglishCourse;

    @Column(nullable = false, name = "max_capacity")
    private int maxCapacity;

    @Column(nullable = false, name = "current_enrollment")
    private int currentEnrollment;

    public String getFormattedCourseSchedules(){
        if(courseSchedules.isEmpty())
            return "-";

        return courseSchedules.stream()
                .sorted(Comparator.comparing(CourseSchedule::getCourseDay))
                .map(CourseSchedule::getScheduleText)
                .collect(Collectors.joining(", "));
    }

    public void addCourseSchedule(CourseSchedule courseSchedule) {
        this.courseSchedules.add(courseSchedule);
    }

    public boolean isRegisterable(){
        return (currentEnrollment < maxCapacity);
    }

}
