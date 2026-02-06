package uss.code.cart.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CartedCourseResponse(
        Long id,
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
        return CartedCourseResponse.builder()
                .id(course.getId())
                .courseClassification(course.getCourseClassification().getName())
                .courseCode(course.getCourseCode())
                .courseTitleKr(course.getTitleKr())
                .courseTitleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(course.getFormattedCourseSchedules())
                .classroom(course.getClassroom() != null ? course.getClassroom() : "-")
                .courseDepartment(course.getCourseDepartment().getName())
                .cartCount(cartCount.intValue())
                .professor(course.getProfessorName() != null ? course.getProfessorName() : "-")
                .build();
    }
}
