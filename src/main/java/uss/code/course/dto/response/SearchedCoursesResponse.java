package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record SearchedCoursesResponse(
    List<SearchedCourseResponse> searchedCourseResponses
) {
    public static SearchedCoursesResponse of(final List<SearchedCourseResponse> searchedCourseResponses){
        return SearchedCoursesResponse.builder()
                .searchedCourseResponses(searchedCourseResponses)
                .build();
    }
}
