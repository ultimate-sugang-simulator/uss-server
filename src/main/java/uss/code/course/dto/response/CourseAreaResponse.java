package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.dto.common.CourseCategory;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CourseAreaResponse(
        String code,
        String name
) {
    public static CourseAreaResponse from(final CourseCategory courseCategory) {
        return CourseAreaResponse.builder()
                .code(courseCategory.areaCode())
                .name(courseCategory.areaName())
                .build();
    }
}
