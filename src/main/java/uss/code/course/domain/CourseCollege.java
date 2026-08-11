package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;
import java.util.Optional;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum CourseCollege {
    HUMANITIES("A000", "인문대학"),
    NATURAL_SCIENCES("B000", "자연과학대학"),
    SOCIAL_SCIENCES("C000", "사회과학대학"),
    COMMERCE_PUBLIC_AFFAIRS("0000689", "글로벌정경대학"),
    ENGINEERING("E000", "공과대학"),
    INFORMATION_TECHNOLOGY("I000", "정보기술대학"),
    BUSINESS("J000", "경영대학"),
    ARTS_PHYSICAL_EDUCATION("0000190", "예술체육대학"),
    EDUCATION("0000063", "사범대학"),
    URBAN_SCIENCE("0000033", "도시과학대학"),
    LIFE_SCIENCES_BIOENGINEERING("0000182", "생명과학기술대학"),
    NONE("0000465", "단과대구분없음"),
    LIBERAL_ARTS_COLLEGE("0000837", "융합자유전공대학"),
    LAW("0000706", "단과대구분없음(법학)"),
    GENERAL_EDUCATION("X000", "교양"),
    TEACHING("Y000", "교직"),
    GENERAL_ELECTIVE("W000", "일선"),
    MILITARY("Z000", "군사학"),
    ETC("V000", "기타");

    private final String code;
    private final String name;

    public static Optional<CourseCollege> tryFromCode(final String code) {
        return Arrays.stream(values())
                .filter(college -> !college.code.isBlank())
                .filter(college -> college.code.equals(code))
                .findFirst();
    }

    public static CourseCollege fromCode(final String code) {
        return tryFromCode(code)
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }
}
