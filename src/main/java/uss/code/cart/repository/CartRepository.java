package uss.code.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.cart.domain.Cart;
import uss.code.course.domain.CourseTerm;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
        SELECT c
        FROM Cart c
        JOIN FETCH c.course course
        LEFT JOIN FETCH course.schedules
        WHERE c.member.id = :memberId
        ORDER BY c.createdAt
    """)
    List<Cart> findByMemberId(@Param("memberId") final long memberId);

    @Query("""
        SELECT c
        FROM Cart c
        WHERE c.member.id = :memberId
        AND c.course.id = :courseId
    """)
    Optional<Cart> findByMemberIdAndCourseId(
            @Param("memberId") final long memberId,
            @Param("courseId") final long courseId
    );

    @Query("""
        SELECT COUNT(c)
        FROM Cart c
        WHERE c.course.academicYear = :academicYear
          AND c.course.term = :term
    """)
    long countBySemester(
            @Param("academicYear") final int academicYear,
            @Param("term") final CourseTerm term
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM Cart c
        WHERE c.course IN (
            SELECT course
            FROM Course course
            WHERE course.academicYear = :academicYear
              AND course.term = :term
        )
    """)
    void deleteBySemester(
            @Param("academicYear") final int academicYear,
            @Param("term") final CourseTerm term
    );
}
