package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record MajorCoursesResponse(
    List<MajorCourseResponse> majorCourseResponses
) {
    public static MajorCoursesResponse of(final List<MajorCourseResponse> majorCourseResponses) {
        return MajorCoursesResponse.builder()
                .majorCourseResponses(majorCourseResponses)
                .build();
    }
}
