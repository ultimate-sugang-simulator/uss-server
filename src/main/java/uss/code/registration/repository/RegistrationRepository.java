package uss.code.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.registration.domain.Registration;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    @Query("""
        SELECT r
        FROM Registration r
        JOIN FETCH r.course
        WHERE r.member.id = :memberId
    """)
    List<Registration> findByMemberId(@Param("memberId") final long memberId);

    @Query("""
        SELECT r
        FROM Registration r
        WHERE r.member.id = :memberId AND r.course.id = :courseId
    """)
    Optional<Registration> findByMemberIdAndCourseId(
            @Param("memberId") final long memberId,
            @Param("courseId") final long courseId
    );
}
