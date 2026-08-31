package uss.code.course.dto.response;

import lombok.Builder;
import uss.code.course.domain.Course;
import uss.code.course.dto.common.CachedGeneralEducationCourse;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record GeneralEducationCourseResponse(
        long id,
        String classification,
        String area,
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
        boolean isRegisterable
) {
    public static GeneralEducationCourseResponse from(final Course course) {
        return GeneralEducationCourseResponse.of(CachedGeneralEducationCourse.from(course), course.isRegisterable());
    }

    public static GeneralEducationCourseResponse of(
            final CachedGeneralEducationCourse course,
            final boolean isRegisterable
    ) {
        return GeneralEducationCourseResponse.builder()
                .id(course.id())
                .classification(course.classification())
                .area(course.area())
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
                .isRegisterable(isRegisterable)
                .build();
    }
}
