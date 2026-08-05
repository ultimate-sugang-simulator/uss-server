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
import java.util.List;

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
