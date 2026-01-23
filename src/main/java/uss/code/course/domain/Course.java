package uss.code.course.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false, name = "professor_name")
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

}
