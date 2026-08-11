package uss.code.course.domain;

import lombok.Builder;

@Builder
public record CourseSnapshot(
        int academicYear,

        CourseTerm term,

        String titleKr,

        String titleEn,

        String courseCode,

        String haksuCode,

        CourseCollege college,

        CourseDepartment department,

        String classificationCode,

        String classificationName,

        CourseArea area,

        String areaCode,

        String areaName,

        String typeCode,

        String typeName,

        String gradeCode,

        String gradeName,

        String concentrationCode,

        String concentrationName,

        int credits,

        boolean isEnglishCourse,

        String englishCode,

        String englishName,

        boolean isHussCourse
) {
}
