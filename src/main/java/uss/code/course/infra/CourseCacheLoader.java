package uss.code.course.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.dto.common.CachedMajorCourse;
import uss.code.course.dto.common.CachedMajorCourses;
import uss.code.course.repository.CourseRepository;
import uss.code.member.domain.MemberDepartment;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseCacheLoader {

    public static final String MAJOR_COURSES = "major-courses";

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

    private CachedMajorCourses readMajorCourses(final MemberDepartment memberDepartment) {
        final List<CourseDepartment> departments = CourseDepartment.ownedBy(memberDepartment);

        final List<CachedMajorCourse> courses = courseRepository.findByDepartmentIn(departments).stream()
                .map(CachedMajorCourse::from)
                .toList();

        return CachedMajorCourses.of(courses);
    }
}
