package uss.code.member.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum College {

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
    LIBERAL_ARTS_COLLEGE("융합자유전공대학"),
    NORTHEAST_ASIAN_STUDIES("동북아국제통상물류학부"),
    LAW("법학부"),

    GRADUATE_SCHOOL("일반대학원"),
    GRADUATE_SCHOOL_LOGISTICS("동북아물류대학원"),
    GRADUATE_SCHOOL_EDUCATION("교육대학원"),
    GRADUATE_SCHOOL_PUBLIC_ADMINISTRATION("정책대학원"),
    GRADUATE_SCHOOL_INFORMATION_TECHNOLOGY("정보기술대학원"),
    GRADUATE_SCHOOL_BUSINESS("경영대학원"),
    GRADUATE_SCHOOL_ENGINEERING("공학대학원"),
    GRADUATE_SCHOOL_CULTURE("문화대학원");

    private final String name;
}
