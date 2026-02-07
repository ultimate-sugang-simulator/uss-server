package uss.code.cart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uss.code.course.domain.Course;
import uss.code.member.domain.Member;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "carts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "course_id"})
        }
)
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

    @Builder(access = PRIVATE)
    public Cart(
            final Member member,
            final Course course
    ) {
        this.member = member;
        this.course = course;
        this.createdAt = LocalDateTime.now();
    }

    public static Cart create(
            final Member member,
            final Course course
    ) {
        return Cart.builder()
                .member(member)
                .course(course)
                .build();
    }
}
