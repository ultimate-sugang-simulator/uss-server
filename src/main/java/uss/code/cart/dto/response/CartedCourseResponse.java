package uss.code.cart.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;
import uss.code.course.infra.CourseScheduleFormatter;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CartedCourseResponse(
        long id,
        String classification,
        String courseCode,
        String haksuCode,
        String titleKr,
        String titleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        String department,
        int cartCount,
        boolean isRegisterable
) {
    public static CartedCourseResponse of(final Course course, final Long cartCount) {
        return CartedCourseResponse.builder()
                .id(course.getId())
                .classification(course.getClassificationName())
                .courseCode(course.getCourseCode())
                .haksuCode(course.getHaksuCode())
                .titleKr(course.getTitleKr())
                .titleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(CourseScheduleFormatter.format(course.getSchedules()))
                .department(course.getDepartment().getName())
                .cartCount(cartCount.intValue())
                .isRegisterable(course.isActive() && course.isRegisterable())
                .build();
    }
}
