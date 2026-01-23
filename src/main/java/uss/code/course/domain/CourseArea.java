package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseArea {
    // 전공
    MAJOR_ADVANCED("전공심화"),
    MAJOR_BASIC("전공기초"),
    MAJOR_CORE("전공핵심"),

    // 기초교양
    BASIC_LIBERAL_ARTS("기초교양"),
    FOUNDATION_OF_LEARNING("학문의기초"),
    BASIC_SCIENCE_ENGINEERING("기초과학·공학"),

    // 핵심교양
    CORE_LIBERAL_ARTS("핵심교양"),
    CORE_HUMANITIES("(핵심)인문"),
    CORE_SOCIAL("(핵심)사회"),
    CORE_FOREIGN_LANGUAGE("(핵심)외국어"),
    CORE_INU_SEMINAR("(핵심)INU세미나"),
    CORE_SCIENCE_TECHNOLOGY("(핵심)과학기술"),
    CORE_ARTS_PHYSICAL("(핵심)예술체육"),

    // 심화교양
    ADVANCED_LIBERAL_ARTS("심화교양"),
    ADVANCED_HUMANITIES("인문"),
    ADVANCED_SCIENCE_TECHNOLOGY("과학기술"),
    ADVANCED_FOREIGN_LANGUAGE("외국어"),
    ADVANCED_ARTS_PHYSICAL("예술체육"),
    ADVANCED_SOCIAL("사회"),

    // 기타
    TEACHING("교직"),
    GENERAL_ELECTIVE("일반선택"),
    MILITARY("군사학");

    private final String name;
}
