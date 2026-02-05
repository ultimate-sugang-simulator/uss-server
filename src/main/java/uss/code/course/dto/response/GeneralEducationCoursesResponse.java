package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record GeneralEducationCoursesResponse(
    List<GeneralEducationCourseResponse> generalEducationCourseResponses
) {
    public static GeneralEducationCoursesResponse of(final List<GeneralEducationCourseResponse> generalEducationCourseResponses) {
        return GeneralEducationCoursesResponse.builder()
                .generalEducationCourseResponses(generalEducationCourseResponses)
                .build();
    }
}
