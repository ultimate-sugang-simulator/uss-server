package uss.code.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminTokenResponse(
        @Schema(
                description = "백오피스 호출용 액세스 토큰",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String accessToken,

        @Schema(
                description = "관리자 이름",
                example = "김학사"
        )
        String name
) {
    public static AdminTokenResponse of(
            final String accessToken,
            final String name
    ) {
        return new AdminTokenResponse(accessToken, name);
    }
}
