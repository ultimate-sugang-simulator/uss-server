package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseClassification {
    MAJOR_ADVANCED("전공심화"),
    MAJOR_BASIC("전공기초"),
    MAJOR_CORE("전공핵심"),
    BASIC_LIBERAL_ARTS("기초교양"),
    CORE_LIBERAL_ARTS("핵심교양"),
    ADVANCED_LIBERAL_ARTS("심화교양"),
    TEACHING("교직"),
    GENERAL_ELECTIVE("일반선택"),
    MILITARY("군사학");

    private final String name;
}
