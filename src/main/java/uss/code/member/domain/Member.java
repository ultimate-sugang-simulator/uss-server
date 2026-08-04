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

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "members")
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "student_id")
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

    public int getMaxCredit(){
        if (lastSemesterGPA >= 4.0) {
            return 24;
        } else if (lastSemesterGPA >= 3.5) {
            return 21;
        } else {
            return 19;
        }
    }
}
