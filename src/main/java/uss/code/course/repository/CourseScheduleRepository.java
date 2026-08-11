package uss.code.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.course.domain.CourseSchedule;
import uss.code.course.domain.CourseTerm;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {
    @Query("""
        SELECT COUNT(s)
        FROM CourseSchedule s
        WHERE s.course.academicYear = :academicYear
          AND s.course.term = :term
    """)
    long countBySemester(
            @Param("academicYear") final int academicYear,
            @Param("term") final CourseTerm term
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM CourseSchedule s
        WHERE s.course IN (
            SELECT c
            FROM Course c
            WHERE c.academicYear = :academicYear
              AND c.term = :term
        )
    """)
    void deleteBySemester(
            @Param("academicYear") final int academicYear,
            @Param("term") final CourseTerm term
    );
}
