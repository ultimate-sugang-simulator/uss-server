package uss.code.registration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.course.domain.Course;
import uss.code.registration.domain.Registration;
import uss.code.registration.dto.response.RegistrationCourseResponse;
import uss.code.registration.dto.response.RegistrationCoursesResponse;
import uss.code.registration.repository.RegistrationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public RegistrationCoursesResponse getRegistrationCourse(final long memberId) {
        List<Registration> registrations = registrationRepository.findByMemberId(memberId);

        List<Course> courses = registrations.stream()
                .map(Registration::getCourse)
                .toList();

        List<RegistrationCourseResponse> registrationCourseResponses = courses.stream()
                .map(RegistrationCourseResponse::from)
                .toList();

        return RegistrationCoursesResponse.of(registrationCourseResponses);
    }
}
