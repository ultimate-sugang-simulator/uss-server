package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record MajorCourseResponse(
        String courseGrade,
        String courseClassification,
        String courseCode,
        String courseTitleKr,
        String courseTitleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        String classroom,
        String department,
        String professor,
        boolean isRegisterable
) {
    public static MajorCourseResponse from(final Course course) {
        return MajorCourseResponse.builder()
                .courseGrade(course.getCourseGrade().getName())
                .courseClassification(course.getCourseClassification().getName())
                .courseCode(course.getCourseCode())
                .courseTitleKr(course.getTitleKr())
                .courseTitleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(course.getFormattedCourseSchedules())
                .classroom(course.getClassroom() != null ? course.getClassroom() : "-")
                .department(course.getCourseDepartment().getName())
                .professor(course.getProfessorName() != null ? course.getProfessorName() : "-")
                .isRegisterable(course.isRegisterable())
                .build();
    }
}
