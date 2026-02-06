package uss.code.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.cart.domain.Cart;
import uss.code.cart.dto.common.CartCount;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
        SELECT c
        FROM Cart c
        JOIN FETCH c.course course
        LEFT JOIN FETCH  course.courseSchedules
        WHERE c.member.id = :memberId
        ORDER BY c.createdAt
    """)
    List<Cart> findCartedCoursesByMemberId(@Param("memberId") final long memberId);

    @Query("""
        SELECT new uss.code.cart.dto.common.CartCount(c.course.id, COUNT(c))
        FROM Cart c
        WHERE c.course.id IN :courseIds
        GROUP BY c.course.id
    """)
    List<CartCount> countCartedCoursesByCourseId(@Param("courseIds") final List<Long> courseIds);
}
