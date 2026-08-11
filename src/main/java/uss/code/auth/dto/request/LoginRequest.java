package uss.code.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "학번이 비어있습니다.")
        @Pattern(regexp = "^[A-Za-z0-9]{1,20}$", message = "학번은 20자 이하의 영문자 또는 숫자여야 합니다.")
        String studentId,

        @NotBlank(message = "비밀번호가 비어있습니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password
) {}