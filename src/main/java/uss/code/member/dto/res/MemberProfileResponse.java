package uss.code.member.dto.res;

import lombok.Builder;
import uss.code.member.domain.Member;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record MemberProfileResponse(
        String department,
        String studentId,
        String name,
        String grade,
        String academicStatus
) {
    public static MemberProfileResponse of(
            final Member member
    ){
        return MemberProfileResponse.builder()
                .department(member.getMemberDepartment().getName())
                .studentId(member.getStudentId())
                .name(member.getName())
                .grade(member.getMemberGrade().getName())
                .academicStatus(member.getAcademicStatus().getName())
                .build();
    }
}
