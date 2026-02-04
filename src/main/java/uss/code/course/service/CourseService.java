package uss.code.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.course.domain.Course;
import uss.code.course.domain.CourseArea;
import uss.code.course.domain.CourseDepartment;
import uss.code.course.dto.response.GeneralEducationCourseResponse;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.MajorCourseResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.repository.CourseRepository;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.Member;
import uss.code.member.repository.MemberRepository;

import java.util.List;

import static uss.code.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND;

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
                CourseDepartment.fromMemberDepartment(member.getMemberDepartment())
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
}
