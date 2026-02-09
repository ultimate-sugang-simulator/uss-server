package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseType {
    LECTURE("강의(이론)"),
    THEORY_LANGUAGE("이론(어학)"),
    LAB("실험실습"),
    THEORY_LAB("이론실험실습"),
    PHYSICAL_EDUCATION("체육실기"),
    ART_PRACTICE("미술실기"),
    ARTS_PHYSICAL_PRACTICE("예술체육실기"),
    E_LEARNING("e-Learning"),
    E_LEARNING_HUSS("e-Learning(HUSS)"),
    ONLINE_BLENDED("온라인혼합형강좌"),
    ONLINE_BLENDED_HUSS("온라인혼합형강좌(HUSS)"),
    FIELD_TYPE_HUSS("현장형(HUSS)"),
    K_MOOC("K-MOOC"),
    OCU("열린사이버대학(OCU)"),
    SOCIAL_SERVICE_1("사회봉사(1)"),
    SOCIAL_SERVICE_2("사회봉사(2)"),
    SOCIAL_SERVICE_3("사회봉사(3)"),
    RISE_WITHOUT_SCHEDULE("RISE(시간표 없음)"),
    RISE_WITH_SCHEDULE("RISE(시간표 있음)"),
    SELF_DESIGNED_SEMINAR("자기설계세미나");

    private final String name;
}
