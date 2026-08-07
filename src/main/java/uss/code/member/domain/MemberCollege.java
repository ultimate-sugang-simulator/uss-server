package uss.code.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum MemberCollege {
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
    LAW("법학부"),
    DEFAULT("미정");

    private final String name;

    public static MemberCollege from(String value){
        return Arrays.stream(values())
                .filter(mc -> mc.name().equalsIgnoreCase(value))
                .findAny()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
