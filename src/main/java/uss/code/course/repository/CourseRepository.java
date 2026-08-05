package uss.code.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.schedules
        WHERE c.department = :department
        ORDER BY c.grade
    """)
    List<Course> findByDepartment(@Param("department") final CourseDepartment department);

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.schedules
        WHERE c.area = :area
        ORDER BY c.grade
    """)
    List<Course> findByArea(@Param("area") final CourseArea area);

    @Query(value = """
        SELECT DISTINCT c.*
        FROM courses c
        WHERE MATCH(c.course_code, c.haksu_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
        ORDER BY MATCH(c.course_code, c.haksu_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
    """, nativeQuery = true)
    List<Course> findByKeyword(@Param("keyword") final String keyword);

    @Query("""
        SELECT c
        FROM Course c
        LEFT JOIN FETCH c.schedules
        WHERE c.id = :id
    """)
    Optional<Course> findByIdWithSchedules(@Param("id") final long id);
}
