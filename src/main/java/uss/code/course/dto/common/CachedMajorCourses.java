package uss.code.course.dto.common;

import lombok.Builder;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CachedMajorCourses(
        List<CachedMajorCourse> courses
) {
    public static CachedMajorCourses of(final List<CachedMajorCourse> courses) {
        return CachedMajorCourses.builder()
                .courses(courses)
                .build();
    }
}
