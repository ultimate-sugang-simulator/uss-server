package uss.code.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record AuthTokenResponse(
        @Schema(
                description = "액세스 토큰",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzAwMDAwMDAwfQ.signature"
        )
        String accessToken
) {
    public static AuthTokenResponse of(final String accessToken){
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
