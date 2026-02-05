package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record SearchedCourseResponse(
        String courseGrade,
        String courseCode,
        String courseTitleKr,
        String courseTitleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        String classroom,
        String courseDepartment,
        String professor,
        boolean isRegisterable
) {
    public static SearchedCourseResponse from(final Course course){
        return SearchedCourseResponse.builder()
                .courseGrade(course.getCourseGrade().getName())
                .courseCode(course.getCourseCode())
                .courseTitleKr(course.getTitleKr())
                .courseTitleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(course.getFormattedCourseSchedules())
                .classroom(course.getClassroom() != null ? course.getClassroom() : "-")
                .courseDepartment(course.getCourseDepartment().getName())
                .professor(course.getProfessorName() != null ? course.getProfessorName() : "-")
                .isRegisterable(course.isRegisterable())
                .build();
    }
}
