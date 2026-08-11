package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CourseCategoryResponse(
        String code,
        String name,
        List<CourseAreaResponse> areaResponses
) {
    public static CourseCategoryResponse of(
            final String code,
            final String name,
            final List<CourseAreaResponse> areaResponses
    ) {
        return CourseCategoryResponse.builder()
                .code(code)
                .name(name)
                .areaResponses(areaResponses)
                .build();
    }
}
