package uss.code.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.auth.dto.request.SignUpRequest;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = PROTECTED)
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "student_id")
    private String studentId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCollege memberCollege;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberDepartment memberDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberGrade memberGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "academic_status")
    private AcademicStatus academicStatus;

    @Column(nullable = false, name = "last_semester_gpa")
    private double lastSemesterGPA;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = PRIVATE)
    private Member(
            final String studentId,
            final String password,
            final String name,
            final MemberCollege memberCollege,
            final MemberDepartment memberDepartment,
            final MemberGrade memberGrade,
            final AcademicStatus academicStatus,
            final double lastSemesterGPA
    ) {
        this.studentId = studentId;
        this.password = password;
        this.name = name;
        this.memberCollege = memberCollege;
        this.memberDepartment = memberDepartment;
        this.memberGrade = memberGrade;
        this.academicStatus = academicStatus;
        this.lastSemesterGPA = lastSemesterGPA;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Member signUp(
            final SignUpRequest request,
            final String encodedPassword
    ) {
        return Member.builder()
                .studentId(request.studentId())
                .password(encodedPassword)
                .name(request.name())
                .memberCollege(MemberCollege.from(request.memberCollege()))
                .memberDepartment(MemberDepartment.from(request.memberDepartment()))
                .memberGrade(MemberGrade.from(request.memberGrade()))
                .academicStatus(AcademicStatus.from(request.academicStatus()))
                .lastSemesterGPA(request.lastSemesterGPA())
                .build();
    }
}
