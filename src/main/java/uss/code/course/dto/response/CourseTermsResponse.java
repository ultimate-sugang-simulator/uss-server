package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CourseTermsResponse(
    List<CourseTermResponse> termResponses
) {
    public static CourseTermsResponse of(final List<CourseTermResponse> termResponses) {
        return CourseTermsResponse.builder()
                .termResponses(termResponses)
                .build();
    }
}
