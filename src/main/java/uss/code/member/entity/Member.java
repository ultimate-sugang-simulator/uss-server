package uss.code.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, name = "student_number")
    private int studentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCollege memberCollege;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberDepartment memberDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "academic_status")
    private AcademicStatus academicStatus;

    @Column(nullable = false, name = "last_semester_gpa")
    private double lastSemesterGPA;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
