package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;
import uss.code.course.infra.CourseScheduleFormatter;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record MajorCourseResponse(
        long id,
        String grade,
        String classification,
        String courseCode,
        String haksuCode,
        String titleKr,
        String titleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        String department,
        boolean isRegisterable
) {
    public static MajorCourseResponse from(final Course course) {
        return MajorCourseResponse.builder()
                .id(course.getId())
                .grade(course.getGrade().getName())
                .classification(course.getClassification().getName())
                .courseCode(course.getCourseCode())
                .haksuCode(course.getHaksuCode())
                .titleKr(course.getTitleKr())
                .titleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(CourseScheduleFormatter.format(course.getSchedules()))
                .department(course.getDepartment().getName())
                .isRegisterable(course.isRegisterable())
                .build();
    }
}
