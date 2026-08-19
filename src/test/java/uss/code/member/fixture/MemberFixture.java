package uss.code.member.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.member.domain.AcademicStatus;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberCollege;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.domain.MemberGrade;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class MemberFixture {

    private static final String ENCODED_PASSWORD = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final AtomicLong EMAIL_SEQUENCE = new AtomicLong();

    public static Member createMember() {
        return createMember(
                "20240001",
                "홍길동",
                MemberCollege.INFORMATION_TECHNOLOGY,
                MemberDepartment.COMPUTER_ENGINEERING,
                MemberGrade.JUNIOR,
                AcademicStatus.ENROLLED,
                3.5
        );
    }

    public static Member createMember(
            final String studentId,
            final String name,
            final MemberCollege college,
            final MemberDepartment department,
            final MemberGrade grade,
            final AcademicStatus academicStatus,
            final double lastSemesterGpa
    ) {
        return createMember(
                "member" + EMAIL_SEQUENCE.incrementAndGet() + "@inu.ac.kr",
                ENCODED_PASSWORD,
                studentId,
                name,
                college,
                department,
                grade,
                academicStatus,
                lastSemesterGpa
        );
    }

    public static Member createMember(
            final String email,
            final String password,
            final String studentId,
            final String name,
            final MemberCollege college,
            final MemberDepartment department,
            final MemberGrade grade,
            final AcademicStatus academicStatus,
            final double lastSemesterGpa
    ) {
        Member member = new Member();

        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "password", password);
        ReflectionTestUtils.setField(member, "studentId", studentId);
        ReflectionTestUtils.setField(member, "name", name);
        ReflectionTestUtils.setField(member, "college", college);
        ReflectionTestUtils.setField(member, "department", department);
        ReflectionTestUtils.setField(member, "grade", grade);
        ReflectionTestUtils.setField(member, "academicStatus", academicStatus);
        ReflectionTestUtils.setField(member, "lastSemesterGpa", lastSemesterGpa);
        ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "updatedAt", LocalDateTime.now());

        return member;
    }

}
