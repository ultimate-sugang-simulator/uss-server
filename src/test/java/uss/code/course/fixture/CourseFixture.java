package uss.code.course.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseClassification;
import uss.code.course.domain.CourseCollege;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.domain.CourseGrade;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseStatus;
import uss.code.course.domain.CourseTerm;
import uss.code.course.domain.CourseType;

import java.util.ArrayList;

public class CourseFixture {

    private static final int DEFAULT_ACADEMIC_YEAR = 2026;
    private static final CourseTerm DEFAULT_TERM = CourseTerm.SECOND;
    private static final String DEFAULT_TITLE_KR = "데이터구조";
    private static final String DEFAULT_TITLE_EN = "Data Structure";
    private static final String DEFAULT_COURSE_CODE = "CSE2010";
    private static final String DEFAULT_HAKSU_CODE = "CSE2010001";
    private static final CourseCollege DEFAULT_COLLEGE = CourseCollege.INFORMATION_TECHNOLOGY;
    private static final CourseDepartment DEFAULT_DEPARTMENT = CourseDepartment.COMPUTER_ENGINEERING;
    private static final CourseClassification DEFAULT_CLASSIFICATION = CourseClassification.MAJOR_CORE;
    private static final CourseArea DEFAULT_AREA = CourseArea.MAJOR_CORE;
    private static final CourseType DEFAULT_TYPE = CourseType.LECTURE;
    private static final CourseGrade DEFAULT_GRADE = CourseGrade.SOPHOMORE;
    private static final int DEFAULT_CREDITS = 3;
    private static final boolean DEFAULT_IS_ENGLISH = false;
    private static final String NOT_ENGLISH_CODE = "0";
    private static final String NOT_ENGLISH_NAME = "비대상";
    private static final String ENGLISH_CODE = "1";
    private static final String ENGLISH_NAME = "원어강의(EN)";
    private static final String DEFAULT_CONCENTRATION_CODE = "0";
    private static final String DEFAULT_CONCENTRATION_NAME = "일반(1~15주)";
    private static final boolean DEFAULT_IS_HUSS = false;
    private static final CourseStatus DEFAULT_STATUS = CourseStatus.ACTIVE;
    private static final int DEFAULT_MAX_CAPACITY = 50;
    private static final int DEFAULT_CURRENT_ENROLLMENT = 30;

    public static Course createCourse() {
        return createCourse(
                DEFAULT_TITLE_KR,
                DEFAULT_TITLE_EN,
                DEFAULT_COURSE_CODE,
                DEFAULT_HAKSU_CODE,
                DEFAULT_COLLEGE,
                DEFAULT_DEPARTMENT,
                DEFAULT_CLASSIFICATION,
                DEFAULT_AREA,
                DEFAULT_TYPE,
                DEFAULT_GRADE,
                DEFAULT_CREDITS,
                DEFAULT_IS_ENGLISH,
                DEFAULT_MAX_CAPACITY,
                DEFAULT_CURRENT_ENROLLMENT
        );
    }

    public static Course createCourseWithDetails(
            final String titleKr,
            final String titleEn,
            final String courseCode,
            final String haksuCode,
            final CourseGrade grade
    ) {
        return createCourse(
                titleKr,
                titleEn,
                courseCode,
                haksuCode,
                DEFAULT_COLLEGE,
                DEFAULT_DEPARTMENT,
                DEFAULT_CLASSIFICATION,
                DEFAULT_AREA,
                DEFAULT_TYPE,
                grade,
                DEFAULT_CREDITS,
                DEFAULT_IS_ENGLISH,
                DEFAULT_MAX_CAPACITY,
                DEFAULT_CURRENT_ENROLLMENT
        );
    }

    public static Course createCourseWithDepartmentAndDetails(
            final String titleKr,
            final String titleEn,
            final String courseCode,
            final String haksuCode,
            final CourseDepartment department,
            final CourseGrade grade
    ) {
        CourseCollege college = department.getCourseCollege();
        return createCourse(
                titleKr,
                titleEn,
                courseCode,
                haksuCode,
                college,
                department,
                DEFAULT_CLASSIFICATION,
                DEFAULT_AREA,
                DEFAULT_TYPE,
                grade,
                DEFAULT_CREDITS,
                DEFAULT_IS_ENGLISH,
                DEFAULT_MAX_CAPACITY,
                DEFAULT_CURRENT_ENROLLMENT
        );
    }

    public static Course createHussCourse(
            final String titleKr,
            final String titleEn,
            final String courseCode,
            final String haksuCode,
            final CourseDepartment department
    ) {
        Course course = createCourseWithDepartmentAndDetails(
                titleKr,
                titleEn,
                courseCode,
                haksuCode,
                department,
                DEFAULT_GRADE
        );
        ReflectionTestUtils.setField(course, "isHussCourse", true);

        return course;
    }

    public static Course createCourse(
            final String titleKr,
            final String titleEn,
            final String courseCode,
            final String haksuCode,
            final CourseCollege college,
            final CourseDepartment department,
            final CourseClassification classification,
            final CourseArea area,
            final CourseType type,
            final CourseGrade grade,
            final int credits,
            final boolean isEnglishCourse,
            final int maxCapacity,
            final int currentEnrollment
    ) {
        Course course = new Course();

        ReflectionTestUtils.setField(course, "schedules", new ArrayList<CourseSchedule>());
        ReflectionTestUtils.setField(course, "academicYear", DEFAULT_ACADEMIC_YEAR);
        ReflectionTestUtils.setField(course, "term", DEFAULT_TERM);
        ReflectionTestUtils.setField(course, "titleKr", titleKr);
        ReflectionTestUtils.setField(course, "titleEn", titleEn);
        ReflectionTestUtils.setField(course, "courseCode", courseCode);
        ReflectionTestUtils.setField(course, "haksuCode", haksuCode);
        ReflectionTestUtils.setField(course, "college", college);
        ReflectionTestUtils.setField(course, "department", department);
        ReflectionTestUtils.setField(course, "classificationCode", classification.getCode());
        ReflectionTestUtils.setField(course, "classificationName", classification.getName());
        ReflectionTestUtils.setField(course, "area", area);
        ReflectionTestUtils.setField(course, "areaCode", area.getCode());
        ReflectionTestUtils.setField(course, "areaName", area.getName());
        ReflectionTestUtils.setField(course, "typeCode", type.getCode());
        ReflectionTestUtils.setField(course, "typeName", type.getName());
        ReflectionTestUtils.setField(course, "gradeCode", grade.getCode());
        ReflectionTestUtils.setField(course, "gradeName", grade.getName());
        ReflectionTestUtils.setField(course, "concentrationCode", DEFAULT_CONCENTRATION_CODE);
        ReflectionTestUtils.setField(course, "concentrationName", DEFAULT_CONCENTRATION_NAME);
        ReflectionTestUtils.setField(course, "credits", credits);
        ReflectionTestUtils.setField(course, "isEnglishCourse", isEnglishCourse);
        ReflectionTestUtils.setField(course, "englishCode", isEnglishCourse ? ENGLISH_CODE : NOT_ENGLISH_CODE);
        ReflectionTestUtils.setField(course, "englishName", isEnglishCourse ? ENGLISH_NAME : NOT_ENGLISH_NAME);
        ReflectionTestUtils.setField(course, "isHussCourse", DEFAULT_IS_HUSS);
        ReflectionTestUtils.setField(course, "maxCapacity", maxCapacity);
        ReflectionTestUtils.setField(course, "currentEnrollment", currentEnrollment);
        ReflectionTestUtils.setField(course, "status", DEFAULT_STATUS);

        return course;
    }

}
