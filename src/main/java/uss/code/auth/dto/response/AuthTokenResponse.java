package uss.code.auth.dto.response;

import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record AuthTokenResponse(
        String accessToken,
        String refreshToken
) {
    public static AuthTokenResponse of(
            String accessToken,
            String refreshToken
    ){
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
