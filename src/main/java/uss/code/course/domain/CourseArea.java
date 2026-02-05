package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_GENERAL_EDUCATION_AREA;

@Getter
@RequiredArgsConstructor
public enum CourseArea {
    // 전공 영역
    MAJOR_ADVANCED("전공심화"),
    MAJOR_BASIC("전공기초"),
    MAJOR_CORE("전공핵심"),

    // 핵심 교양 영역
    CORE_INU_SEMINAR("(핵심)INU세미나"),
    CORE_HUMANITIES("(핵심)인문"),
    CORE_SOCIAL("(핵심)사회"),
    CORE_SCIENCE_TECHNOLOGY("(핵심)과학기술"),
    CORE_ARTS_SPORTS("(핵심)예술체육"),
    CORE_FOREIGN_LANGUAGE("(핵심)외국어"),

    // 일반 교양 영역
    HUMANITIES("인문"),
    SOCIAL("사회"),
    SCIENCE_TECHNOLOGY("과학기술"),
    ARTS_SPORTS("예술체육"),
    FOREIGN_LANGUAGE("외국어"),

    // 기타
    TEACHING("교직"),
    GENERAL_ELECTIVE("일반선택"),
    MILITARY("군사학");

    private final String name;

    public static CourseArea from(final String courseArea) {
        try{
            return CourseArea.valueOf(courseArea.toUpperCase());
        }catch(IllegalArgumentException e){
            throw new RestApiException(INVALID_ENUM_TYPE);
        }
    }

    public static CourseArea fromGeneralEducation(final String courseArea) {
        final CourseArea area = from(courseArea);

        if (!area.isGeneralEducationArea()) {
            throw new RestApiException(INVALID_GENERAL_EDUCATION_AREA);
        }

        return area;
    }

    private boolean isGeneralEducationArea() {
        return this == CORE_INU_SEMINAR ||
                this == CORE_HUMANITIES ||
                this == CORE_SOCIAL ||
                this == CORE_SCIENCE_TECHNOLOGY ||
                this == CORE_ARTS_SPORTS ||
                this == CORE_FOREIGN_LANGUAGE ||
                this == HUMANITIES ||
                this == SOCIAL ||
                this == SCIENCE_TECHNOLOGY ||
                this == ARTS_SPORTS ||
                this == FOREIGN_LANGUAGE;
    }
}
