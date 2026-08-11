package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CourseCategoriesResponse(
    List<CourseCategoryResponse> categoryResponses
) {
    public static CourseCategoriesResponse of(final List<CourseCategoryResponse> categoryResponses) {
        return CourseCategoriesResponse.builder()
                .categoryResponses(categoryResponses)
                .build();
    }
}
