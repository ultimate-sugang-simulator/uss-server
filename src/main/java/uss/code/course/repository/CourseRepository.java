package uss.code.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.dto.common.CourseCategory;
import uss.code.course.dto.common.CourseTermInfo;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.schedules
        WHERE c.department = :department
        ORDER BY c.gradeCode, c.classificationCode, c.haksuCode
    """)
    List<Course> findByDepartment(@Param("department") final CourseDepartment department);

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.schedules
        WHERE c.area = :area
        ORDER BY c.gradeCode, c.classificationCode, c.haksuCode
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

    @Query("""
        SELECT DISTINCT new uss.code.course.dto.common.CourseCategory(
            c.classificationCode, c.classificationName, c.areaCode, c.areaName)
        FROM Course c
        ORDER BY c.classificationCode, c.areaCode
    """)
    List<CourseCategory> findCategories();

    @Query("""
        SELECT DISTINCT new uss.code.course.dto.common.CourseTermInfo(c.academicYear, c.term)
        FROM Course c
        ORDER BY c.academicYear DESC, c.term
    """)
    List<CourseTermInfo> findTerms();

    @Query("""
        SELECT DISTINCT c.department
        FROM Course c
        WHERE c.department IN :departments
    """)
    List<CourseDepartment> findDepartmentsIn(@Param("departments") final List<CourseDepartment> departments);

    @Query("""
        SELECT DISTINCT c
        FROM Course c
        LEFT JOIN FETCH c.schedules
        WHERE c.isHussCourse = true
        ORDER BY c.gradeCode, c.classificationCode, c.haksuCode
    """)
    List<Course> findHussCourses();
}
