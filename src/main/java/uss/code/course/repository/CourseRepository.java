package uss.code.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.courseSchedules
        WHERE c.courseDepartment = :course_department
        ORDER BY c.courseGrade, c.courseCode
    """)
    List<Course> findByCourseDepartment(@Param("course_department") final CourseDepartment courseDepartment);

    List<Course> findByCourseArea(CourseArea courseArea);
}
