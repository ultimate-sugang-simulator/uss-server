package uss.code.course.dto.common;

import lombok.Builder;
import uss.code.course.domain.Course;
import uss.code.course.infra.CourseScheduleFormatter;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record CachedGeneralEducationCourse(
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

        boolean isHussCourse
) {
    public static CachedGeneralEducationCourse from(final Course course) {
        return CachedGeneralEducationCourse.builder()
                .id(course.getId())
                .classification(course.getClassificationName())
                .area(course.getAreaName())
                .courseCode(course.getCourseCode())
                .haksuCode(course.getHaksuCode())
                .titleKr(course.getTitleKr())
                .titleEn(course.getTitleEn())
                .credits(course.getCredits())
                .isEnglishCourse(course.isEnglishCourse())
                .englishCourseName(course.isEnglishCourse() ? course.getEnglishName() : null)
                .schedule(CourseScheduleFormatter.format(course.getSchedules()))
                .is75MinLesson(course.is75MinLesson())
                .suupTypeCode(course.getTypeCode())
                .suupTypeName(course.getTypeName())
                .cnctrIsuCode(course.getConcentrationCode())
                .cnctrIsuName(course.getConcentrationName())
                .isHussCourse(course.isHussCourse())
                .build();
    }
}
