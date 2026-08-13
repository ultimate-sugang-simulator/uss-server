package uss.code.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;

import java.util.Arrays;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_DEPARTMENT;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;

@Getter
@RequiredArgsConstructor
public enum MemberDepartment {
    // 인문대학
    KOREAN_LITERATURE(MemberCollege.HUMANITIES, "국어국문학과"),
    ENGLISH_LITERATURE(MemberCollege.HUMANITIES, "영어영문학과"),
    GERMAN_STUDIES(MemberCollege.HUMANITIES, "독어독문학과"),
    FRENCH_STUDIES(MemberCollege.HUMANITIES, "불어불문학과"),
    JAPANESE_LITERATURE(MemberCollege.HUMANITIES, "일본지역문화학과"),
    CHINESE_STUDIES(MemberCollege.HUMANITIES, "중어중국학과"),

    // 자연과학대학
    MATHEMATICS(MemberCollege.NATURAL_SCIENCES, "수학과"),
    PHYSICS(MemberCollege.NATURAL_SCIENCES, "물리학과"),
    CHEMISTRY(MemberCollege.NATURAL_SCIENCES, "화학과"),
    FASHION_INDUSTRY(MemberCollege.NATURAL_SCIENCES, "패션산업학과"),
    MARINE_SCIENCE(MemberCollege.NATURAL_SCIENCES, "해양학과"),

    // 사회과학대학
    SOCIAL_WELFARE(MemberCollege.SOCIAL_SCIENCES, "사회복지학과"),
    MEDIA_COMMUNICATION(MemberCollege.SOCIAL_SCIENCES, "미디어커뮤니케이션학과"),
    LIBRARY_INFO(MemberCollege.SOCIAL_SCIENCES, "문헌정보학과"),
    CREATIVE_HRD(MemberCollege.SOCIAL_SCIENCES, "창의인재개발학과"),

    // 글로벌정경대학
    PUBLIC_ADMINISTRATION(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "행정학과"),
    POLITICS_DIPLOMACY(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "정치외교학과"),
    ECONOMICS(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "경제학과"),
    ECONOMICS_NIGHT(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "경제학과(야)"),
    GLOBAL_TRADE_SERVICE(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "Global Trade & Service학부"),
    TRADE_NIGHT(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "무역학부(야)"),
    CONSUMER_SCIENCE(MemberCollege.COMMERCE_PUBLIC_AFFAIRS, "소비자학과"),

    // 공과대학
    MECHANICAL_ENGINEERING(MemberCollege.ENGINEERING, "기계공학과"),
    ELECTRICAL_ENGINEERING(MemberCollege.ENGINEERING, "전기공학과"),
    ELECTRONICS_ENGINEERING_SCHOOL(MemberCollege.ENGINEERING, "전자공학부"),
    INDUSTRIAL_MANAGEMENT(MemberCollege.ENGINEERING, "산업경영공학과"),
    MATERIAL_SCIENCE(MemberCollege.ENGINEERING, "신소재공학과"),
    SAFETY_ENGINEERING(MemberCollege.ENGINEERING, "안전공학과"),
    ENERGY_CHEMICAL(MemberCollege.ENGINEERING, "에너지화학공학과"),
    BIO_ROBOTICS_ENGINEERING(MemberCollege.ENGINEERING, "바이오-로봇시스템공학과"),

    // 정보기술대학
    COMPUTER_ENGINEERING(MemberCollege.INFORMATION_TECHNOLOGY, "컴퓨터공학부"),
    INFORMATION_COMMUNICATION_ENGINEERING(MemberCollege.INFORMATION_TECHNOLOGY, "정보통신공학과"),
    EMBEDDED_SYSTEM(MemberCollege.INFORMATION_TECHNOLOGY, "임베디드시스템공학과"),

    // 경영대학
    BUSINESS_ADMINISTRATION(MemberCollege.BUSINESS, "경영학부"),
    DATA_SCIENCE(MemberCollege.BUSINESS, "데이터과학과"),
    TAX_ACCOUNTING(MemberCollege.BUSINESS, "세무회계학과"),

    // 예술체육대학
    FINE_ARTS_SCHOOL(MemberCollege.ARTS_PHYSICAL_EDUCATION, "조형예술학부"),
    DESIGN(MemberCollege.ARTS_PHYSICAL_EDUCATION, "디자인학부"),
    PERFORMING_ART(MemberCollege.ARTS_PHYSICAL_EDUCATION, "공연예술학과"),
    SPORTS_SCIENCE(MemberCollege.ARTS_PHYSICAL_EDUCATION, "스포츠과학부"),
    HEALTH_EXERCISE(MemberCollege.ARTS_PHYSICAL_EDUCATION, "운동건강학부"),

    // 사범대학
    KOREAN_EDUCATION(MemberCollege.EDUCATION, "국어교육과"),
    ENGLISH_EDUCATION(MemberCollege.EDUCATION, "영어교육과"),
    JAPANESE_EDUCATION(MemberCollege.EDUCATION, "일어교육과"),
    MATH_EDUCATION(MemberCollege.EDUCATION, "수학교육과"),
    PHYSICAL_EDUCATION(MemberCollege.EDUCATION, "체육교육과"),
    EARLY_CHILDHOOD_EDUCATION(MemberCollege.EDUCATION, "유아교육과"),
    HISTORY_EDUCATION(MemberCollege.EDUCATION, "역사교육과"),
    ETHICS_EDUCATION(MemberCollege.EDUCATION, "윤리교육과"),

    // 도시과학대학
    URBAN_ADMINISTRATION(MemberCollege.URBAN_SCIENCE, "도시행정학과"),
    URBAN_ENVIRONMENT_ENGINEERING_SCHOOL(MemberCollege.URBAN_SCIENCE, "도시환경공학부"),
    URBAN_ENGINEERING(MemberCollege.URBAN_SCIENCE, "도시공학과"),
    URBAN_ARCHITECTURE_SCHOOL(MemberCollege.URBAN_SCIENCE, "도시건축학부"),

    // 생명과학기술대학
    LIFE_SCIENCE_SCHOOL(MemberCollege.LIFE_SCIENCES_BIOENGINEERING, "생명과학부"),
    BIOENGINEERING_SCHOOL(MemberCollege.LIFE_SCIENCES_BIOENGINEERING, "생명공학부"),

    // 융합자유전공대학
    LIBERAL_ARTS(MemberCollege.LIBERAL_ARTS_COLLEGE, "자유전공학부"),
    INTERNATIONAL_LIBERAL_ARTS(MemberCollege.LIBERAL_ARTS_COLLEGE, "국제자유전공학부"),
    CONVERGENCE(MemberCollege.LIBERAL_ARTS_COLLEGE, "융합학부"),

    // 동북아국제통상물류학부
    NORTHEAST_ASIAN_TRADE_MAJOR(MemberCollege.NORTHEAST_ASIA_TRADE_LOGISTICS, "동북아국제통상전공"),
    SMART_LOGISTICS_ENGINEERING_MAJOR(MemberCollege.NORTHEAST_ASIA_TRADE_LOGISTICS, "스마트물류공학전공"),
    IBE_MAJOR(MemberCollege.NORTHEAST_ASIA_TRADE_LOGISTICS, "IBE전공"),

    // 법학부
    LAW(MemberCollege.LAW, "법학부"),

    // 기본값 (포털 로그인으로 신규 생성된 회원의 미설정 상태)
    DEFAULT(MemberCollege.DEFAULT, "미정");

    private final MemberCollege memberCollege;
    private final String name;

    public static MemberDepartment from(String value){
        return Arrays.stream(values())
                .filter(md -> md.name().equalsIgnoreCase(value))
                .findAny()
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }

    public static MemberDepartment fromSelectable(final String value) {
        final MemberDepartment memberDepartment = from(value);

        if (memberDepartment == DEFAULT) {
            throw new RestApiException(INVALID_DEPARTMENT);
        }

        return memberDepartment;
    }
}
