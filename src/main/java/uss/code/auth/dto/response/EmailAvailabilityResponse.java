package uss.code.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record EmailAvailabilityResponse(
        @Schema(
                description = "이메일 사용 가능 여부",
                example = "true"
        )
        boolean available
) {
    public static EmailAvailabilityResponse of(final boolean available) {
        return EmailAvailabilityResponse.builder()
                .available(available)
                .build();
    }
}
