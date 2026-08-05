package uss.code.registration.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record RegistrationCourseResponse(
        long id,
        String classification,
        String courseCode,
        String haksuCode,
        String titleKr,
        String titleEn,
        int credits,
        boolean isEnglishCourse,
        String schedule,
        String department
) {
    public static RegistrationCourseResponse from(final Course course) {
        return RegistrationCourseResponse.builder()
                .id(course.getId())
                .classification(course.getClassification().getName())
                .courseCode(course.getCourseCode())
                .haksuCode(course.getHaksuCode())
                .titleKr(course.getTitleKr())
                .titleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .schedule(course.getFormattedCourseSchedules())
                .department(course.getDepartment().getName())
                .build();
    }
}
