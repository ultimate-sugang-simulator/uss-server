package uss.code.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @Schema(
                description = "이메일",
                example = "student@inu.ac.kr"
        )
        @NotBlank(message = "이메일이 비어있습니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(
                description = "비밀번호",
                example = "password1234"
        )
        @NotBlank(message = "비밀번호가 비어있습니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @Schema(
                description = "학번",
                example = "202012345"
        )
        @NotBlank(message = "학번이 비어있습니다.")
        @Pattern(regexp = "^[A-Za-z0-9]{1,20}$", message = "학번은 20자 이하의 영문자 또는 숫자여야 합니다.")
        String studentId,

        @Schema(
                description = "이름",
                example = "홍길동"
        )
        @NotBlank(message = "이름이 비어있습니다.")
        String name,

        @Schema(
                description = "단과대학",
                example = "INFORMATION_TECHNOLOGY"
        )
        @NotBlank(message = "단과대학이 비어있습니다.")
        String college,

        @Schema(
                description = "학과(부)",
                example = "COMPUTER_ENGINEERING"
        )
        @NotBlank(message = "학과가 비어있습니다.")
        String department,

        @Schema(
                description = "학년",
                example = "JUNIOR"
        )
        @NotBlank(message = "학년이 비어있습니다.")
        String grade,

        @Schema(
                description = "학적 상태",
                example = "ENROLLED"
        )
        @NotBlank(message = "학적 상태가 비어있습니다.")
        String academicStatus,

        @Schema(
                description = "직전 학기 성적",
                example = "3.5"
        )
        @NotNull(message = "직전 학기 성적이 비어있습니다.")
        @DecimalMin(value = "0.0", message = "직전 학기 성적은 0.0 이상이어야 합니다.")
        @DecimalMax(value = "4.5", message = "직전 학기 성적은 4.5 이하여야 합니다.")
        Double lastSemesterGpa
) {}
