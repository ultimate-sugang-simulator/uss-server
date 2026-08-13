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
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "college")
    private MemberCollege college;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "department")
    private MemberDepartment department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "grade")
    private MemberGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "academic_status")
    private AcademicStatus academicStatus;

    @Column(nullable = false, name = "last_semester_gpa")
    private double lastSemesterGpa;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = PRIVATE)
    private Member(
            final String studentId,
            final String name,
            final MemberCollege college,
            final MemberDepartment department,
            final MemberGrade grade,
            final AcademicStatus academicStatus,
            final double lastSemesterGpa
    ) {
        this.studentId = studentId;
        this.name = name;
        this.college = college;
        this.department = department;
        this.grade = grade;
        this.academicStatus = academicStatus;
        this.lastSemesterGpa = lastSemesterGpa;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Member createDefault(
            final String studentId
    ) {
        return Member.builder()
                .studentId(studentId)
                .name(studentId)
                .college(MemberCollege.DEFAULT)
                .department(MemberDepartment.DEFAULT)
                .grade(MemberGrade.DEFAULT)
                .academicStatus(AcademicStatus.DEFAULT)
                .lastSemesterGpa(0.0)
                .build();
    }

    public void updateDepartment(final MemberDepartment department) {
        this.department = department;
        this.college = department.getMemberCollege();
        this.updatedAt = LocalDateTime.now();
    }

    public int getMaxCredit(){
        if (lastSemesterGpa >= 4.0) {
            return 24;
        } else if (lastSemesterGpa >= 3.5) {
            return 21;
        } else {
            return 19;
        }
    }
}
