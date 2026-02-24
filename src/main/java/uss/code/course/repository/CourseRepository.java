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
        LEFT JOIN FETCH c.courseSchedules
        WHERE c.courseDepartment = :course_department
        ORDER BY c.courseGrade
    """)
    List<Course> findByCourseDepartment(@Param("course_department") final CourseDepartment courseDepartment);

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.courseSchedules
        WHERE c.courseArea = :course_area
        ORDER BY c.courseGrade
    """)
    List<Course> findByCourseArea(@Param("course_area") final CourseArea courseArea);

    @Query(value = """
        SELECT DISTINCT c.*
        FROM courses c
        WHERE MATCH(c.course_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
        ORDER BY MATCH(c.course_code, c.title_kr, c.title_en) AGAINST(:keyword IN BOOLEAN MODE)
    """, nativeQuery = true)
    List<Course> findByKeyword(@Param("keyword") final String keyword);

    @Query("""
        SELECT c
        FROM Course c
        LEFT JOIN FETCH c.courseSchedules
        WHERE c.id = :id
    """)
    Optional<Course> findByIdWithSchedules(@Param("id") final long id);
}
