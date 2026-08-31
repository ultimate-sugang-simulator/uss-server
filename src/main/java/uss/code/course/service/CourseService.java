package uss.code.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.dto.common.CachedGeneralEducationCourses;
import uss.code.course.dto.common.CachedMajorCourses;
import uss.code.course.dto.common.CourseCapacity;
import uss.code.course.dto.common.CourseCategory;
import uss.code.course.dto.common.CourseTermInfo;
import uss.code.course.dto.response.CourseAreaResponse;
import uss.code.course.dto.response.CourseCategoriesResponse;
import uss.code.course.dto.response.CourseCategoryResponse;
import uss.code.course.dto.response.CourseTermResponse;
import uss.code.course.dto.response.CourseTermsResponse;
import uss.code.course.dto.response.GeneralEducationCourseResponse;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCourseResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorsResponse;
import uss.code.course.dto.response.MajorCourseResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.dto.response.SearchedCourseResponse;
import uss.code.course.dto.response.SearchedCoursesResponse;
import uss.code.course.infra.CourseCacheLoader;
import uss.code.course.repository.CourseRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.domain.MemberDepartment;
import uss.code.member.repository.MemberRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseCacheLoader courseCacheLoader;

    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MajorCoursesResponse getMajorCourses(final long memberId) {
        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        final List<CourseDepartment> departments = CourseDepartment.ownedBy(member.getDepartment());
        if (departments.isEmpty()) {
            return MajorCoursesResponse.of(List.of());
        }

        final CachedMajorCourses cachedCourses = courseCacheLoader.loadMajorCourses(member.getDepartment());
        final Map<Long, CourseCapacity> capacities = courseRepository.findCapacitiesByDepartmentIn(departments).stream()
                .collect(toMap(CourseCapacity::id, identity()));

        final List<MajorCourseResponse> majorCourseResponses = cachedCourses.courses().stream()
                .filter(course -> capacities.containsKey(course.id()))
                .map(course -> MajorCourseResponse.of(course, capacities.get(course.id()).isRegisterable()))
                .toList();

        return MajorCoursesResponse.of(majorCourseResponses);
    }

    @Transactional(readOnly = true)
    public GeneralEducationCoursesResponse getGeneralEducationCourses(final String courseArea) {
        final CourseArea area = CourseArea.fromGeneralEducation(courseArea);

        final CachedGeneralEducationCourses cachedCourses = courseCacheLoader.loadGeneralEducationCourses(area);
        final Map<Long, CourseCapacity> capacities = courseRepository.findCapacitiesByArea(area).stream()
                .collect(toMap(CourseCapacity::id, identity()));

        final List<GeneralEducationCourseResponse> generalEducationCourseResponses = cachedCourses.courses().stream()
                .filter(course -> capacities.containsKey(course.id()))
                .map(course -> GeneralEducationCourseResponse.of(course, capacities.get(course.id()).isRegisterable()))
                .toList();

        return GeneralEducationCoursesResponse.of(generalEducationCourseResponses);
    }

    @Transactional(readOnly = true)
    public MajorCoursesResponse getOtherDepartmentCourses(final String department) {
        final MemberDepartment memberDepartment = MemberDepartment.from(department);

        final List<CourseDepartment> departments = CourseDepartment.ownedBy(memberDepartment);
        if (departments.isEmpty()) {
            return MajorCoursesResponse.of(List.of());
        }

        final List<Course> courses = courseRepository.findByDepartmentIn(departments);

        final List<MajorCourseResponse> majorCourseResponses = courses.stream()
                .map(MajorCourseResponse::from)
                .toList();

        return MajorCoursesResponse.of(majorCourseResponses);
    }

    @Transactional(readOnly = true)
    public InterdisciplinaryMajorCoursesResponse getInterdisciplinaryMajorCourses(final String department) {
        final CourseDepartment courseDepartment = CourseDepartment.fromInterdisciplinary(department);

        final List<Course> courses = courseRepository.findByDepartment(courseDepartment);

        final List<InterdisciplinaryMajorCourseResponse> interdisciplinaryMajorCoursesResponses = courses.stream()
                .map(InterdisciplinaryMajorCourseResponse::from)
                .toList();

        return InterdisciplinaryMajorCoursesResponse.of(interdisciplinaryMajorCoursesResponses);
    }

    @Transactional(readOnly = true)
    public SearchedCoursesResponse searchCourses(final String keyword) {
        final List<Course> courses = courseRepository.findByKeyword(keyword);

        final List<SearchedCourseResponse> searchedCourseResponses = courses.stream()
                .map(SearchedCourseResponse::from)
                .toList();

        return SearchedCoursesResponse.of(searchedCourseResponses);
    }

    @Transactional(readOnly = true)
    public MajorCoursesResponse getHussCourses() {
        final List<Course> courses = courseRepository.findHussCourses();

        final List<MajorCourseResponse> majorCourseResponses = courses.stream()
                .map(MajorCourseResponse::from)
                .toList();

        return MajorCoursesResponse.of(majorCourseResponses);
    }

    @Transactional(readOnly = true)
    public CourseCategoriesResponse getCategories() {
        final Map<String, List<CourseCategory>> areasByClassification = courseRepository.findCategories().stream()
                .collect(groupingBy(CourseCategory::classificationCode, LinkedHashMap::new, toList()));

        final List<CourseCategoryResponse> categoryResponses = areasByClassification.values().stream()
                .map(areas -> CourseCategoryResponse.of(
                        areas.get(0).classificationCode(),
                        areas.get(0).classificationName(),
                        areas.stream().map(CourseAreaResponse::from).toList()
                ))
                .toList();

        return CourseCategoriesResponse.of(categoryResponses);
    }

    @Transactional(readOnly = true)
    public CourseTermsResponse getTerms() {
        final List<CourseTermInfo> termInfos = courseRepository.findTerms();

        final List<CourseTermResponse> termResponses = termInfos.stream()
                .map(CourseTermResponse::from)
                .toList();

        return CourseTermsResponse.of(termResponses);
    }

    @Transactional(readOnly = true)
    public InterdisciplinaryMajorsResponse getInterdisciplinaryMajors() {
        final List<CourseDepartment> interdisciplinaryDepartments = CourseDepartment.interdisciplinaryValues();
        final List<CourseDepartment> existingDepartments = courseRepository.findDepartmentsIn(interdisciplinaryDepartments);

        final List<InterdisciplinaryMajorResponse> interdisciplinaryMajorResponses = interdisciplinaryDepartments.stream()
                .filter(existingDepartments::contains)
                .map(InterdisciplinaryMajorResponse::from)
                .toList();

        return InterdisciplinaryMajorsResponse.of(interdisciplinaryMajorResponses);
    }
}
