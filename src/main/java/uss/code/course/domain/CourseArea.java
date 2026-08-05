package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_GENERAL_EDUCATION_AREA;

@Getter
@RequiredArgsConstructor
public enum CourseArea {
    // 전공 영역
    MAJOR_ADVANCED("35", "전공심화"),
    MAJOR_BASIC("31", "전공기초"),
    MAJOR_CORE("34", "전공핵심"),

    // 기초 교양 영역
    BASIC_SCIENCE_ENGINEERING("162", "기초과학·공학"),
    ACADEMIC_FOUNDATION("161", "학문의기초"),

    // 핵심 교양 영역
    CORE_INU_SEMINAR("171", "(핵심)INU세미나"),
    CORE_HUMANITIES("172", "(핵심)인문"),
    CORE_SOCIAL("173", "(핵심)사회"),
    CORE_SCIENCE_TECHNOLOGY("174", "(핵심)과학기술"),
    CORE_ARTS_SPORTS("175", "(핵심)예술체육"),
    CORE_FOREIGN_LANGUAGE("176", "(핵심)외국어"),

    // 심화 교양 영역
    HUMANITIES("182", "인문"),
    SOCIAL("183", "사회"),
    SCIENCE_TECHNOLOGY("184", "과학기술"),
    ARTS_SPORTS("185", "예술체육"),
    FOREIGN_LANGUAGE("186", "외국어"),

    // 기타
    TEACHING("51", "교직"),
    GENERAL_ELECTIVE("81", "일반선택"),
    MILITARY("71", "군사학");

    private final String code;
    private final String name;

    public static CourseArea fromCode(final String code) {
        return Arrays.stream(values())
                .filter(area -> !area.code.isBlank())
                .filter(area -> area.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }

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
        return this == BASIC_SCIENCE_ENGINEERING ||
                this == ACADEMIC_FOUNDATION ||
                this == CORE_INU_SEMINAR ||
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
