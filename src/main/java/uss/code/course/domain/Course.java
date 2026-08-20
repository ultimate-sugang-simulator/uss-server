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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static lombok.AccessLevel.PRIVATE;
import static uss.code.course.domain.CourseStatus.ACTIVE;
import static uss.code.course.domain.CourseStatus.CLOSED;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "courses")
public class Course {

    private static final int INITIAL_ENROLLMENT = 0;

    private static final String FIELD_TITLE_KR = "titleKr";
    private static final String FIELD_TITLE_EN = "titleEn";
    private static final String FIELD_COURSE_CODE = "courseCode";
    private static final String FIELD_CREDITS = "credits";
    private static final String FIELD_COLLEGE = "college";
    private static final String FIELD_DEPARTMENT = "department";
    private static final String FIELD_CLASSIFICATION = "classification";
    private static final String FIELD_AREA = "area";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_GRADE = "grade";
    private static final String FIELD_CONCENTRATION = "concentration";
    private static final String FIELD_ENGLISH_COURSE = "isEnglishCourse";
    private static final String FIELD_HUSS_COURSE = "isHussCourse";

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

    @Column(nullable = false, name = "classification_code")
    private String classificationCode;

    @Column(nullable = false, name = "classification_name")
    private String classificationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "area")
    private CourseArea area;

    @Column(nullable = false, name = "area_code")
    private String areaCode;

    @Column(nullable = false, name = "area_name")
    private String areaName;

    @Column(nullable = false, name = "type_code")
    private String typeCode;

    @Column(nullable = false, name = "type_name")
    private String typeName;

    @Column(nullable = false, name = "grade_code")
    private String gradeCode;

    @Column(nullable = false, name = "grade_name")
    private String gradeName;

    @Column(nullable = false, name = "concentration_code")
    private String concentrationCode;

    @Column(nullable = false, name = "concentration_name")
    private String concentrationName;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false, name = "is_english_course")
    private boolean isEnglishCourse;

    @Column(nullable = false, name = "english_code")
    private String englishCode;

    @Column(nullable = false, name = "english_name")
    private String englishName;

    @Column(nullable = false, name = "is_huss_course")
    private boolean isHussCourse;

    @Column(nullable = false, name = "max_capacity")
    private int maxCapacity;

    @Column(nullable = false, name = "current_enrollment")
    private int currentEnrollment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private CourseStatus status;

    @Builder(access = PRIVATE)
    private Course(
            final CourseSnapshot snapshot,
            final int maxCapacity
    ) {
        this.academicYear = snapshot.academicYear();
        this.term = snapshot.term();
        this.titleKr = snapshot.titleKr();
        this.titleEn = snapshot.titleEn();
        this.courseCode = snapshot.courseCode();
        this.haksuCode = snapshot.haksuCode();
        this.college = snapshot.college();
        this.department = snapshot.department();
        this.classificationCode = snapshot.classificationCode();
        this.classificationName = snapshot.classificationName();
        this.area = snapshot.area();
        this.areaCode = snapshot.areaCode();
        this.areaName = snapshot.areaName();
        this.typeCode = snapshot.typeCode();
        this.typeName = snapshot.typeName();
        this.gradeCode = snapshot.gradeCode();
        this.gradeName = snapshot.gradeName();
        this.concentrationCode = snapshot.concentrationCode();
        this.concentrationName = snapshot.concentrationName();
        this.credits = snapshot.credits();
        this.isEnglishCourse = snapshot.isEnglishCourse();
        this.englishCode = snapshot.englishCode();
        this.englishName = snapshot.englishName();
        this.isHussCourse = snapshot.isHussCourse();
        this.maxCapacity = maxCapacity;
        this.currentEnrollment = INITIAL_ENROLLMENT;
        this.status = ACTIVE;
    }

    public static Course create(
            final CourseSnapshot snapshot,
            final int maxCapacity
    ) {
        return Course.builder()
                .snapshot(snapshot)
                .maxCapacity(maxCapacity)
                .build();
    }

    public List<CourseFieldChange> applyUpdate(final CourseSnapshot snapshot) {
        final List<CourseFieldChange> changes = new ArrayList<>();

        replaceIfChanged(changes, FIELD_TITLE_KR, titleKr, snapshot.titleKr(), value -> titleKr = value);
        replaceIfChanged(changes, FIELD_TITLE_EN, titleEn, snapshot.titleEn(), value -> titleEn = value);
        replaceIfChanged(changes, FIELD_COURSE_CODE, courseCode, snapshot.courseCode(), value -> courseCode = value);
        replaceIfChanged(changes, FIELD_CREDITS, credits, snapshot.credits(), value -> credits = value);
        replaceIfChanged(changes, FIELD_HUSS_COURSE, isHussCourse, snapshot.isHussCourse(), value -> isHussCourse = value);

        replaceIfChanged(changes, FIELD_COLLEGE, college, snapshot.college(), CourseCollege::getName, value -> college = value);
        replaceIfChanged(changes, FIELD_DEPARTMENT, department, snapshot.department(), CourseDepartment::getName, value -> department = value);

        replaceClassification(changes, snapshot);
        replaceArea(changes, snapshot);
        replaceType(changes, snapshot);
        replaceGrade(changes, snapshot);
        replaceConcentration(changes, snapshot);
        replaceEnglish(changes, snapshot);

        return changes;
    }

    public void replaceSchedules(final List<CourseSchedule> schedules) {
        this.schedules.clear();
        schedules.forEach(this::addCourseSchedule);
    }

    public void close() {
        this.status = CLOSED;
    }

    public void reopen() {
        this.status = ACTIVE;
    }

    public boolean isActive() {
        return status == ACTIVE;
    }

    public void addCourseSchedule(final CourseSchedule courseSchedule) {
        this.schedules.add(courseSchedule);
        courseSchedule.addCourse(this);
    }

    public boolean is75MinLesson() {
        return schedules.stream().anyMatch(CourseSchedule::is75MinLesson);
    }

    public boolean isRegisterable(){
        return (currentEnrollment < maxCapacity);
    }

    private void replaceClassification(
            final List<CourseFieldChange> changes,
            final CourseSnapshot snapshot
    ) {
        if (isSameCodePair(classificationCode, classificationName, snapshot.classificationCode(), snapshot.classificationName()))
            return;

        changes.add(CourseFieldChange.of(FIELD_CLASSIFICATION, classificationName, snapshot.classificationName()));
        this.classificationCode = snapshot.classificationCode();
        this.classificationName = snapshot.classificationName();
    }

    private void replaceArea(
            final List<CourseFieldChange> changes,
            final CourseSnapshot snapshot
    ) {
        if (isSameCodePair(areaCode, areaName, snapshot.areaCode(), snapshot.areaName()))
            return;

        changes.add(CourseFieldChange.of(FIELD_AREA, areaName, snapshot.areaName()));
        this.area = snapshot.area();
        this.areaCode = snapshot.areaCode();
        this.areaName = snapshot.areaName();
    }

    private void replaceType(
            final List<CourseFieldChange> changes,
            final CourseSnapshot snapshot
    ) {
        if (isSameCodePair(typeCode, typeName, snapshot.typeCode(), snapshot.typeName()))
            return;

        changes.add(CourseFieldChange.of(FIELD_TYPE, typeName, snapshot.typeName()));
        this.typeCode = snapshot.typeCode();
        this.typeName = snapshot.typeName();
    }

    private void replaceGrade(
            final List<CourseFieldChange> changes,
            final CourseSnapshot snapshot
    ) {
        if (isSameCodePair(gradeCode, gradeName, snapshot.gradeCode(), snapshot.gradeName()))
            return;

        changes.add(CourseFieldChange.of(FIELD_GRADE, gradeName, snapshot.gradeName()));
        this.gradeCode = snapshot.gradeCode();
        this.gradeName = snapshot.gradeName();
    }

    private void replaceConcentration(
            final List<CourseFieldChange> changes,
            final CourseSnapshot snapshot
    ) {
        if (isSameCodePair(concentrationCode, concentrationName, snapshot.concentrationCode(), snapshot.concentrationName()))
            return;

        changes.add(CourseFieldChange.of(FIELD_CONCENTRATION, concentrationName, snapshot.concentrationName()));
        this.concentrationCode = snapshot.concentrationCode();
        this.concentrationName = snapshot.concentrationName();
    }

    private void replaceEnglish(
            final List<CourseFieldChange> changes,
            final CourseSnapshot snapshot
    ) {
        if (isEnglishCourse == snapshot.isEnglishCourse()
                && isSameCodePair(englishCode, englishName, snapshot.englishCode(), snapshot.englishName()))
            return;

        changes.add(CourseFieldChange.of(FIELD_ENGLISH_COURSE, englishName, snapshot.englishName()));
        this.isEnglishCourse = snapshot.isEnglishCourse();
        this.englishCode = snapshot.englishCode();
        this.englishName = snapshot.englishName();
    }

    private boolean isSameCodePair(
            final String currentCode,
            final String currentName,
            final String updatedCode,
            final String updatedName
    ) {
        return Objects.equals(currentCode, updatedCode) && Objects.equals(currentName, updatedName);
    }

    private <T> void replaceIfChanged(
            final List<CourseFieldChange> changes,
            final String field,
            final T current,
            final T updated,
            final Consumer<T> setter
    ) {
        replaceIfChanged(changes, field, current, updated, String::valueOf, setter);
    }

    private <T> void replaceIfChanged(
            final List<CourseFieldChange> changes,
            final String field,
            final T current,
            final T updated,
            final Function<T, String> toText,
            final Consumer<T> setter
    ) {
        if (Objects.equals(current, updated))
            return;

        changes.add(CourseFieldChange.of(field, toText.apply(current), toText.apply(updated)));
        setter.accept(updated);
    }
}
