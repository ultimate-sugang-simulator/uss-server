package uss.code.auth.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uss.code.global.annotation.EnumValidation;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;

public record SignUpRequest(

        @NotBlank(message = "학번이 비어있습니다.")
        @Pattern(regexp = "^\\d{9}$", message = "학번은 9자리 숫자여야 합니다.")
        String studentId,

        @NotBlank(message = "비밀번호가 비어있습니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @NotBlank(message = "이름이 비어있습니다.")
        String name,

        @NotBlank(message = "단과대학이 비어있습니다.")
        @EnumValidation(target = MemberCollege.class, message = "유효하지 않은 단과대학입니다.")
        String memberCollege,

        @NotBlank(message = "학과/부가 비어있습니다.")
        @EnumValidation(target = MemberDepartment.class, message = "유효하지 않은 학과/부입니다.")
        String memberDepartment,

        @NotBlank(message = "학년이 비어있습니다.")
        @EnumValidation(target = MemberGrade.class, message = "유효하지 않은 학년입니다.")
        String memberGrade,

        @NotBlank(message = "학적상태가 비어있습니다.")
        @EnumValidation(target = AcademicStatus.class, message = "유효하지 않은 학적상태입니다.")
        String academicStatus,

        @NotNull(message = "직전 학기 학점이 비어있습니다.")
        @DecimalMin(value = "0.0", message = "학점은 0.0 이상이어야 합니다.")
        @DecimalMax(value = "4.5", message = "학점은 4.5 이하여야 합니다.")
        Double lastSemesterGPA
) {
}
