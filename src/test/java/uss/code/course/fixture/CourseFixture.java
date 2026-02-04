package uss.code.course.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseClassification;
import uss.code.course.domain.CourseCollege;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.domain.CourseGrade;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseType;

import java.util.ArrayList;

public class CourseFixture {

    private static final String DEFAULT_TITLE_KR = "데이터구조";
    private static final String DEFAULT_TITLE_EN = "Data Structure";
    private static final String DEFAULT_COURSE_CODE = "CSE2010";
    private static final CourseCollege DEFAULT_COLLEGE = CourseCollege.INFORMATION_TECHNOLOGY;
    private static final CourseDepartment DEFAULT_DEPARTMENT = CourseDepartment.COMPUTER_ENGINEERING;
    private static final CourseClassification DEFAULT_CLASSIFICATION = CourseClassification.MAJOR_CORE;
    private static final CourseArea DEFAULT_AREA = CourseArea.MAJOR_CORE;
    private static final CourseType DEFAULT_TYPE = CourseType.LECTURE;
    private static final CourseGrade DEFAULT_GRADE = CourseGrade.SOPHOMORE;
    private static final String DEFAULT_PROFESSOR = "홍길동";
    private static final String DEFAULT_CLASSROOM = "S401";
    private static final int DEFAULT_CREDITS = 3;
    private static final boolean DEFAULT_IS_ENGLISH = false;
    private static final int DEFAULT_MAX_CAPACITY = 50;
    private static final int DEFAULT_CURRENT_ENROLLMENT = 30;

    public static Course createCourse() {
        return createCourse(
                DEFAULT_TITLE_KR,
                DEFAULT_TITLE_EN,
                DEFAULT_COURSE_CODE,
                DEFAULT_COLLEGE,
                DEFAULT_DEPARTMENT,
                DEFAULT_CLASSIFICATION,
                DEFAULT_AREA,
                DEFAULT_TYPE,
                DEFAULT_GRADE,
                DEFAULT_PROFESSOR,
                DEFAULT_CLASSROOM,
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
            final CourseGrade grade,
            final String professor,
            final String classroom
    ) {
        return createCourse(
                titleKr,
                titleEn,
                courseCode,
                DEFAULT_COLLEGE,
                DEFAULT_DEPARTMENT,
                DEFAULT_CLASSIFICATION,
                DEFAULT_AREA,
                DEFAULT_TYPE,
                grade,
                professor,
                classroom,
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
            final CourseDepartment department,
            final CourseGrade grade,
            final String professor,
            final String classroom
    ) {
        CourseCollege college = department.getCourseCollege();
        return createCourse(
                titleKr,
                titleEn,
                courseCode,
                college,
                department,
                DEFAULT_CLASSIFICATION,
                DEFAULT_AREA,
                DEFAULT_TYPE,
                grade,
                professor,
                classroom,
                DEFAULT_CREDITS,
                DEFAULT_IS_ENGLISH,
                DEFAULT_MAX_CAPACITY,
                DEFAULT_CURRENT_ENROLLMENT
        );
    }

    public static Course createCourse(
            final String titleKr,
            final String titleEn,
            final String courseCode,
            final CourseCollege courseCollege,
            final CourseDepartment courseDepartment,
            final CourseClassification courseClassification,
            final CourseArea courseArea,
            final CourseType courseType,
            final CourseGrade courseGrade,
            final String professorName,
            final String classroom,
            final int credits,
            final boolean isEnglishCourse,
            final int maxCapacity,
            final int currentEnrollment
    ) {
        Course course = new Course();

        ReflectionTestUtils.setField(course, "courseSchedules", new ArrayList<CourseSchedule>());
        ReflectionTestUtils.setField(course, "titleKr", titleKr);
        ReflectionTestUtils.setField(course, "titleEn", titleEn);
        ReflectionTestUtils.setField(course, "courseCode", courseCode);
        ReflectionTestUtils.setField(course, "courseCollege", courseCollege);
        ReflectionTestUtils.setField(course, "courseDepartment", courseDepartment);
        ReflectionTestUtils.setField(course, "courseClassification", courseClassification);
        ReflectionTestUtils.setField(course, "courseArea", courseArea);
        ReflectionTestUtils.setField(course, "courseType", courseType);
        ReflectionTestUtils.setField(course, "courseGrade", courseGrade);
        ReflectionTestUtils.setField(course, "professorName", professorName);
        ReflectionTestUtils.setField(course, "classroom", classroom);
        ReflectionTestUtils.setField(course, "credits", credits);
        ReflectionTestUtils.setField(course, "isEnglishCourse", isEnglishCourse);
        ReflectionTestUtils.setField(course, "maxCapacity", maxCapacity);
        ReflectionTestUtils.setField(course, "currentEnrollment", currentEnrollment);

        return course;
    }

}
