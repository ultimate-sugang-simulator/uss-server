package uss.code.member.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;

import java.time.LocalDateTime;
import java.util.UUID;

public class MemberFixture {

    public static Member createMember() {
        return createMember(
                UUID.randomUUID().toString().substring(0, 10) + "@example.com",
                "20240001",
                "password123",
                "홍길동",
                MemberCollege.INFORMATION_TECHNOLOGY,
                MemberDepartment.COMPUTER_ENGINEERING,
                MemberGrade.JUNIOR,
                AcademicStatus.ENROLLED,
                3.5
        );
    }

    public static Member createMember(
            final String email,
            final String studentId,
            final String password,
            final String name,
            final MemberCollege memberCollege,
            final MemberDepartment memberDepartment,
            final MemberGrade memberGrade,
            final AcademicStatus academicStatus,
            final double lastSemesterGPA
    ) {
        Member member = new Member();

        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "studentId", studentId);
        ReflectionTestUtils.setField(member, "password", password);
        ReflectionTestUtils.setField(member, "name", name);
        ReflectionTestUtils.setField(member, "memberCollege", memberCollege);
        ReflectionTestUtils.setField(member, "memberDepartment", memberDepartment);
        ReflectionTestUtils.setField(member, "memberGrade", memberGrade);
        ReflectionTestUtils.setField(member, "academicStatus", academicStatus);
        ReflectionTestUtils.setField(member, "lastSemesterGPA", lastSemesterGPA);
        ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "updatedAt", LocalDateTime.now());

        return member;
    }
}
