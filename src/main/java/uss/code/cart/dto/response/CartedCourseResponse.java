package uss.code.cart.dto.response;

import uss.code.course.domain.Course;

public record CartedCourseResponse(
        String courseClassification,
        String courseCode,
        String courseTitleKr,
        String courseTitleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        String classroom,
        String courseDepartment,
        int cartCount,
        String professor
) {
    public static CartedCourseResponse of(final Course course, final Long cartCount) {
        return new CartedCourseResponse(
                course.getCourseClassification().getName(),
                course.getCourseCode(),
                course.getTitleKr(),
                course.getTitleEn(),
                course.getCredits(),
                course.isEnglishCourse(),
                course.getFormattedCourseSchedules(),
                course.getClassroom() != null ? course.getClassroom() : "-",
                course.getCourseDepartment().getName(),
                cartCount.intValue(),
                course.getProfessorName() != null ? course.getProfessorName() : "-"
        );
    }
}
