package uss.code.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationCodeSendRequest(
        @NotBlank(message = "이메일이 비어있습니다.")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@inu\\.ac\\.kr$", message = "인천대학교 이메일(@inu.ac.kr)만 사용 가능합니다")
        String email
) {
}