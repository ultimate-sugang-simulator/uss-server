package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record MajorCoursesResponse(
    List<MajorCourseResponse> majorCourses
) {
    public static MajorCoursesResponse of(final List<MajorCourseResponse> majorCourses) {
        return MajorCoursesResponse.builder()
                .majorCourses(majorCourses)
                .build();
    }
}
