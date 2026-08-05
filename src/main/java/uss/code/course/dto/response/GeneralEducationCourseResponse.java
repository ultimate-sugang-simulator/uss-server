package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;
import uss.code.course.infra.CourseScheduleFormatter;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record GeneralEducationCourseResponse(
        long id,
        String classification,
        String area,
        String courseCode,
        String haksuCode,
        String titleKr,
        String titleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        boolean isRegisterable
) {
    public static GeneralEducationCourseResponse from(final Course course) {
        return GeneralEducationCourseResponse.builder()
                .id(course.getId())
                .classification(course.getClassification().getName())
                .area(course.getArea().getName())
                .courseCode(course.getCourseCode())
                .haksuCode(course.getHaksuCode())
                .titleKr(course.getTitleKr())
                .titleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(CourseScheduleFormatter.format(course.getSchedules()))
                .isRegisterable(course.isRegisterable())
                .build();
    }
}
