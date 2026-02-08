package uss.code.registration.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import uss.code.course.domain.Course;
import uss.code.member.domain.Member;
import uss.code.registration.domain.Registration;

import java.time.LocalDateTime;

public class RegistrationFixture {

    public static Registration createRegistration(final Member member, final Course course) {
        return createRegistration(member, course, LocalDateTime.now());
    }

    public static Registration createRegistration(
            final Member member,
            final Course course,
            final LocalDateTime createdAt
    ) {
        Registration registration = new Registration();

        ReflectionTestUtils.setField(registration, "member", member);
        ReflectionTestUtils.setField(registration, "course", course);
        ReflectionTestUtils.setField(registration, "createdAt", createdAt);

        return registration;
    }
}
