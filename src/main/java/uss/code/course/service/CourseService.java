package uss.code.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.dto.response.GeneralEducationCourseResponse;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCourseResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCoursesResponse;
import uss.code.course.dto.response.MajorCourseResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.dto.response.SearchedCourseResponse;
import uss.code.course.dto.response.SearchedCoursesResponse;
import uss.code.course.repository.CourseRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import java.util.List;

import static uss.code.global.exception.domain.BusinessExceptionCode.MEMBER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MajorCoursesResponse getMajorCourses(final long memberId) {
        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RestApiException(MEMBER_NOT_FOUND));

        final List<Course> courses = courseRepository.findByCourseDepartment(
                CourseDepartment.from(member.getMemberDepartment())
        );

        final List<MajorCourseResponse> majorCourseResponses = courses.stream()
                .map(MajorCourseResponse::from)
                .toList();

        return MajorCoursesResponse.of(majorCourseResponses);
    }

    @Transactional(readOnly = true)
    public GeneralEducationCoursesResponse getGeneralEducationCourses(final String courseArea) {
        final List<Course> courses = courseRepository.findByCourseArea(CourseArea.fromGeneralEducation(courseArea));

        final List<GeneralEducationCourseResponse> generalEducationCourseResponses = courses.stream()
                .map(GeneralEducationCourseResponse::from)
                .toList();

        return GeneralEducationCoursesResponse.of(generalEducationCourseResponses);
    }

    /**
     * TODO 현재 타학과 조회 로직인데, 교양, 일선에 해당하는 department가 들어와도 조회됨 -> 특정 학과가 아니면 조회 안되는 로직 추가 필요
     */
    @Transactional(readOnly = true)
    public MajorCoursesResponse getOtherDepartmentCourses(final String department) {
        final CourseDepartment courseDepartment = CourseDepartment.from(department);

        final List<Course> courses = courseRepository.findByCourseDepartment(courseDepartment);

        final List<MajorCourseResponse> majorCourseResponses = courses.stream()
                .map(MajorCourseResponse::from)
                .toList();

        return MajorCoursesResponse.of(majorCourseResponses);
    }

    @Transactional(readOnly = true)
    public InterdisciplinaryMajorCoursesResponse getInterdisciplinaryMajorCourses(final String department) {
        final CourseDepartment courseDepartment = CourseDepartment.fromInterdisciplinary(department);

        final List<Course> courses = courseRepository.findByCourseDepartment(courseDepartment);

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
}
