package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.CourseDepartment;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record InterdisciplinaryMajorResponse(
        String code,
        String name
) {
    public static InterdisciplinaryMajorResponse from(final CourseDepartment department) {
        return InterdisciplinaryMajorResponse.builder()
                .code(department.name())
                .name(department.getName())
                .build();
    }
}
