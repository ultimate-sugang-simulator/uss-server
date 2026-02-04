package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record InterdisciplinaryMajorCoursesResponse(
    List<InterdisciplinaryMajorCourseResponse> interdisciplinaryMajorCourseResponses
) {
    public static InterdisciplinaryMajorCoursesResponse of(final List<InterdisciplinaryMajorCourseResponse> interdisciplinaryMajorCourseResponses) {
        return InterdisciplinaryMajorCoursesResponse.builder()
                .interdisciplinaryMajorCourseResponses(interdisciplinaryMajorCourseResponses)
                .build();
    }
}
