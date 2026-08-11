package uss.code.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @Schema(
                description = "관리자 로그인 아이디",
                example = "local-admin"
        )
        @NotBlank(message = "아이디가 비어있습니다.")
        @Size(max = 50, message = "아이디는 50자 이하여야 합니다.")
        String loginId,

        @Schema(
                description = "관리자 비밀번호",
                example = "uss-local-admin"
        )
        @NotBlank(message = "비밀번호가 비어있습니다.")
        @Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
        String password
) {}
