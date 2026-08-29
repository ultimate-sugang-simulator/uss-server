package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;
import uss.code.course.dto.common.CachedMajorCourse;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record MajorCourseResponse(
        long id,
        String grade,
        String classification,
        String courseCode,
        String haksuCode,
        String titleKr,
        String titleEn,
        int credits,
        boolean isEnglishCourse,
        String englishCourseName,
        String schedule,
        boolean is75MinLesson,
        String suupTypeCode,
        String suupTypeName,
        String cnctrIsuCode,
        String cnctrIsuName,
        boolean isHussCourse,
        String department,
        boolean isRegisterable
) {
    public static MajorCourseResponse from(final Course course) {
        return MajorCourseResponse.of(CachedMajorCourse.from(course), course.isRegisterable());
    }

    public static MajorCourseResponse of(
            final CachedMajorCourse course,
            final boolean isRegisterable
    ) {
        return MajorCourseResponse.builder()
                .id(course.id())
                .grade(course.grade())
                .classification(course.classification())
                .courseCode(course.courseCode())
                .haksuCode(course.haksuCode())
                .titleKr(course.titleKr())
                .titleEn(course.titleEn())
                .credits(course.credits())
                .isEnglishCourse(course.isEnglishCourse())
                .englishCourseName(course.englishCourseName())
                .schedule(course.schedule())
                .is75MinLesson(course.is75MinLesson())
                .suupTypeCode(course.suupTypeCode())
                .suupTypeName(course.suupTypeName())
                .cnctrIsuCode(course.cnctrIsuCode())
                .cnctrIsuName(course.cnctrIsuName())
                .isHussCourse(course.isHussCourse())
                .department(course.department())
                .isRegisterable(isRegisterable)
                .build();
    }
}
