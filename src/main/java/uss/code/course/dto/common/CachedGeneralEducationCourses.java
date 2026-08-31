package uss.code.course.dto.common;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CachedGeneralEducationCourses(
        List<CachedGeneralEducationCourse> courses
) {
    public static CachedGeneralEducationCourses of(final List<CachedGeneralEducationCourse> courses) {
        return CachedGeneralEducationCourses.builder()
                .courses(courses)
                .build();
    }
}
