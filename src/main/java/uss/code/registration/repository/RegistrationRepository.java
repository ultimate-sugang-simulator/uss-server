package uss.code.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.course.domain.CourseTerm;
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

    @Query("""
        SELECT COUNT(r)
        FROM Registration r
        WHERE r.course.academicYear = :academicYear
          AND r.course.term = :term
    """)
    long countBySemester(
            @Param("academicYear") final int academicYear,
            @Param("term") final CourseTerm term
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM Registration r
        WHERE r.course IN (
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
