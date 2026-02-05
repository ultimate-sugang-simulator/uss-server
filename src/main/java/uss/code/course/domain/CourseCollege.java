package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseCollege {
    HUMANITIES("인문대학"),
    NATURAL_SCIENCES("자연과학대학"),
    SOCIAL_SCIENCES("사회과학대학"),
    COMMERCE_PUBLIC_AFFAIRS("글로벌정경대학"),
    ENGINEERING("공과대학"),
    INFORMATION_TECHNOLOGY("정보기술대학"),
    BUSINESS("경영대학"),
    ARTS_PHYSICAL_EDUCATION("예술체육대학"),
    EDUCATION("사범대학"),
    URBAN_SCIENCE("도시과학대학"),
    LIFE_SCIENCES_BIOENGINEERING("생명과학기술대학"),
    NONE("단과대구분없음"),
    LIBERAL_ARTS_COLLEGE("융합자유전공대학"),
    LAW("단과대구분없음(법학)"),
    GENERAL_EDUCATION("교양"),
    TEACHING("교직"),
    GENERAL_ELECTIVE("일선"),
    MILITARY("군사학"),
    ETC("기타");

    private final String name;
}
