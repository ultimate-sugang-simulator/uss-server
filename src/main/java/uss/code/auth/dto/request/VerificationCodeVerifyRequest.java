package uss.code.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerificationCodeVerifyRequest(
        @NotBlank(message = "이메일이 비어있습니다.")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@inu\\.ac\\.kr$", message = "인천대학교 이메일(@inu.ac.kr)만 사용 가능합니다")
        String email,

        @NotBlank(message = "인증코드가 비어있습니다.")
        @Size(min = 6, max = 6, message = "인증코드의 길이는 6입니다.")
        String code
) {
}
