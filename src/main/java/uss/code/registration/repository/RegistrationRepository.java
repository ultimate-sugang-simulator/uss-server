package uss.code.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.registration.domain.Registration;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    @Query("""
        SELECT r
        FROM Registration r
        JOIN FETCH r.course
        WHERE r.member.id = :memberId
    """)
    List<Registration> findByMemberId(@Param("memberId") final long memberId);
}
