package uss.code.registration.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record RegistrationCoursesResponse(
        List<RegistrationCourseResponse> registrationCourseResponses
) {
    public static RegistrationCoursesResponse of(final List<RegistrationCourseResponse> registrationCourseResponses) {
        return RegistrationCoursesResponse.builder()
                .registrationCourseResponses(registrationCourseResponses)
                .build();
    }
}
