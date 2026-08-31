package uss.code.course.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.dto.common.CachedGeneralEducationCourse;
import uss.code.course.dto.common.CachedGeneralEducationCourses;
import uss.code.course.dto.common.CachedMajorCourse;
import uss.code.course.dto.common.CachedMajorCourses;
import uss.code.course.repository.CourseRepository;
import uss.code.member.domain.MemberDepartment;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseCacheLoader {

    public static final String MAJOR_COURSES = "major-courses";
    public static final String GENERAL_EDUCATION_COURSES = "general-education-courses";

    private final CourseRepository courseRepository;

    @Cacheable(cacheNames = MAJOR_COURSES, key = "#memberDepartment.name()")
    @Transactional(readOnly = true)
    public CachedMajorCourses loadMajorCourses(final MemberDepartment memberDepartment) {
        return readMajorCourses(memberDepartment);
    }

    @CachePut(cacheNames = MAJOR_COURSES, key = "#memberDepartment.name()")
    @Transactional(readOnly = true)
    public CachedMajorCourses refreshMajorCourses(final MemberDepartment memberDepartment) {
        return readMajorCourses(memberDepartment);
    }

    @Cacheable(cacheNames = GENERAL_EDUCATION_COURSES, key = "#courseArea.name()")
    @Transactional(readOnly = true)
    public CachedGeneralEducationCourses loadGeneralEducationCourses(final CourseArea courseArea) {
        return readGeneralEducationCourses(courseArea);
    }

    @CachePut(cacheNames = GENERAL_EDUCATION_COURSES, key = "#courseArea.name()")
    @Transactional(readOnly = true)
    public CachedGeneralEducationCourses refreshGeneralEducationCourses(final CourseArea courseArea) {
        return readGeneralEducationCourses(courseArea);
    }

    private CachedMajorCourses readMajorCourses(final MemberDepartment memberDepartment) {
        final List<CourseDepartment> departments = CourseDepartment.ownedBy(memberDepartment);

        final List<CachedMajorCourse> courses = courseRepository.findByDepartmentIn(departments).stream()
                .map(CachedMajorCourse::from)
                .toList();

        return CachedMajorCourses.of(courses);
    }

    private CachedGeneralEducationCourses readGeneralEducationCourses(final CourseArea courseArea) {
        final List<CachedGeneralEducationCourse> courses = courseRepository.findByArea(courseArea).stream()
                .map(CachedGeneralEducationCourse::from)
                .toList();

        return CachedGeneralEducationCourses.of(courses);
    }
}
