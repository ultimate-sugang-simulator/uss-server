package uss.code.cart.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.course.domain.Course;
import uss.code.member.domain.Member;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "carts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "course_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false,name = "course_id")
    private Course course;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
