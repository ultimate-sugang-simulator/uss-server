package uss.code.course.dto.response;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record InterdisciplinaryMajorsResponse(
    List<InterdisciplinaryMajorResponse> interdisciplinaryMajorResponses
) {
    public static InterdisciplinaryMajorsResponse of(
            final List<InterdisciplinaryMajorResponse> interdisciplinaryMajorResponses
    ) {
        return InterdisciplinaryMajorsResponse.builder()
                .interdisciplinaryMajorResponses(interdisciplinaryMajorResponses)
                .build();
    }
}
