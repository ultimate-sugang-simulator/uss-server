package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseDepartment {
    // 인문대학
    KOREAN_LITERATURE(CourseCollege.HUMANITIES, "국어국문학과"),
    ENGLISH_LITERATURE(CourseCollege.HUMANITIES, "영어영문학과"),
    GERMAN_STUDIES(CourseCollege.HUMANITIES, "독어독문학과"),
    FRENCH_STUDIES(CourseCollege.HUMANITIES, "불어불문학과"),
    JAPANESE_LITERATURE(CourseCollege.HUMANITIES, "일본지역문화학과"),
    CHINESE_STUDIES(CourseCollege.HUMANITIES, "중어중국학과"),

    // 자연과학대학
    MATHEMATICS(CourseCollege.NATURAL_SCIENCES, "수학과"),
    PHYSICS(CourseCollege.NATURAL_SCIENCES, "물리학과"),
    CHEMISTRY(CourseCollege.NATURAL_SCIENCES, "화학과"),
    FASHION_INDUSTRY(CourseCollege.NATURAL_SCIENCES, "패션산업학과"),
    MARINE_SCIENCE(CourseCollege.NATURAL_SCIENCES, "해양학과"),

    // 사회과학대학
    SOCIAL_WELFARE(CourseCollege.SOCIAL_SCIENCES, "사회복지학과"),
    MEDIA_COMMUNICATION(CourseCollege.SOCIAL_SCIENCES, "미디어커뮤니케이션학과"),
    LIBRARY_INFO(CourseCollege.SOCIAL_SCIENCES, "문헌정보학과"),
    CREATIVE_HRD(CourseCollege.SOCIAL_SCIENCES, "창의인재개발학과"),

    // 글로벌정경대학
    PUBLIC_ADMINISTRATION(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "행정학과"),
    POLITICS_DIPLOMACY(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "정치외교학과"),
    ECONOMICS(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "경제학과"),
    TRADE(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "무역학부"),
    CONSUMER_SCIENCE(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "소비자학과"),

    // 공과대학
    ENERGY_CHEMICAL(CourseCollege.ENGINEERING, "에너지화학공학과"),
    ELECTRICAL_ENGINEERING(CourseCollege.ENGINEERING, "전기공학과"),
    ELECTRONICS_ENGINEERING(CourseCollege.ENGINEERING, "전자공학과"),
    INDUSTRIAL_MANAGEMENT(CourseCollege.ENGINEERING, "산업경영공학과"),
    MATERIAL_SCIENCE(CourseCollege.ENGINEERING, "신소재공학과"),
    MECHANICAL_ENGINEERING(CourseCollege.ENGINEERING, "기계공학과"),
    BIO_ROBOTICS_ENGINEERING(CourseCollege.ENGINEERING, "바이오-로봇 시스템 공학과"),
    SAFETY_ENGINEERING(CourseCollege.ENGINEERING, "안전공학과"),
    MECHATRONICS_ENGINEERING(CourseCollege.ENGINEERING, "메카트로닉스공학과"),

    // 정보기술대학
    COMPUTER_ENGINEERING(CourseCollege.INFORMATION_TECHNOLOGY, "컴퓨터공학부"),
    INFORMATION_COMMUNICATION_ENGINEERING(CourseCollege.INFORMATION_TECHNOLOGY, "정보통신공학과"),
    EMBEDDED_SYSTEM(CourseCollege.INFORMATION_TECHNOLOGY, "임베디드시스템공학과"),

    // 경영대학
    BUSINESS_ADMINISTRATION(CourseCollege.BUSINESS, "경영학부"),
    DATA_SCIENCE(CourseCollege.BUSINESS, "데이터과학과"),
    TAX_ACCOUNTING(CourseCollege.BUSINESS, "세무회계학과"),
    TECHNO_MANAGEMENT(CourseCollege.BUSINESS, "테크노경영학과"),

    // 예술체육대학
    FINE_ARTS(CourseCollege.ARTS_PHYSICAL_EDUCATION, "조형예술학부"),
    DESIGN(CourseCollege.ARTS_PHYSICAL_EDUCATION, "디자인학부"),
    PERFORMING_ART(CourseCollege.ARTS_PHYSICAL_EDUCATION, "공연예술학과"),
    SPORTS_SCIENCE(CourseCollege.ARTS_PHYSICAL_EDUCATION, "스포츠과학부"),
    HEALTH_EXERCISE(CourseCollege.ARTS_PHYSICAL_EDUCATION, "운동건강학부"),

    // 사범대학
    KOREAN_EDUCATION(CourseCollege.EDUCATION, "국어교육과"),
    ENGLISH_EDUCATION(CourseCollege.EDUCATION, "영어교육과"),
    JAPANESE_EDUCATION(CourseCollege.EDUCATION, "일어교육과"),
    MATH_EDUCATION(CourseCollege.EDUCATION, "수학교육과"),
    PHYSICAL_EDUCATION(CourseCollege.EDUCATION, "체육교육과"),
    EARLY_CHILDHOOD_EDUCATION(CourseCollege.EDUCATION, "유아교육과"),
    HISTORY_EDUCATION(CourseCollege.EDUCATION, "역사교육과"),
    ETHICS_EDUCATION(CourseCollege.EDUCATION, "윤리교육과"),

    // 도시과학대학
    URBAN_ADMINISTRATION(CourseCollege.URBAN_SCIENCE, "도시행정학과"),
    CIVIL_ENVIRONMENT_ENGINEERING(CourseCollege.URBAN_SCIENCE, "도시환경학부"),
    URBAN_ENGINEERING(CourseCollege.URBAN_SCIENCE, "도시공학과"),
    URBAN_ARCHITECTURE(CourseCollege.URBAN_SCIENCE, "도시건축학부"),

    // 생명과학기술대학
    LIFE_SCIENCE(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명과학부"),
    BIOENGINEERING(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명공학부"),

    // 융합자유전공대학
    LIBERAL_ARTS(CourseCollege.LIBERAL_ARTS_COLLEGE, "자유전공학부"),
    INTERNATIONAL_LIBERAL_ARTS(CourseCollege.LIBERAL_ARTS_COLLEGE, "국제자유전공학부"),
    CONVERGENCE(CourseCollege.LIBERAL_ARTS_COLLEGE, "융합학부"),

    // 동북아국제통상학부
    NORTHEAST_ASIAN_TRADE(CourseCollege.NORTHEAST_ASIAN_STUDIES, "동북아국제통상전공"),
    SMART_LOGISTICS_ENGINEERING(CourseCollege.NORTHEAST_ASIAN_STUDIES, "스마트물류공학전공"),
    IBE(CourseCollege.NORTHEAST_ASIAN_STUDIES, "IBE전공"),

    // 법학부
    LAW(CourseCollege.LAW, "법학부"),

    // 일선
    GENERAL_ELECTIVE(CourseCollege.GENERAL_ELECTIVE, "일선"),

    // 교양
    GENERAL_EDUCATION(CourseCollege.GENERAL_EDUCATION, "교양"),

    // 군사학
    MILITARY(CourseCollege.MILITARY, "군사학"),

    // 교직
    TEACHING(CourseCollege.TEACHING, "교직"),

    // 기타
    OPTOELECTRONICS(CourseCollege.ETC, "광전자공학전공(연계)"),
    FUTURE_AUTOMOBILE(CourseCollege.ETC, "미래자동차연계전공"),
    SOCIAL_DATA_SCIENCE(CourseCollege.ETC, "소셜데이터사이언스연계전공"),
    CREATIVE_DESIGN(CourseCollege.ETC, "창의적디자인연계전공"),
    HUMANITIES_CULTURE_ART_PLANNING(CourseCollege.ETC, "인문문화예술기획연계전공"),
    LOGISTICS(CourseCollege.ETC, "물류학전공(연계)"),
    BEAUTY_INDUSTRY(CourseCollege.ETC, "뷰티산업연계전공"),
    FUTURE_EDUCATION_DESIGN(CourseCollege.ETC, "미래교육디자인연계전공"),
    INTERNATIONAL_DEVELOPMENT_COOPERATION(CourseCollege.ETC, "국제개발협력연계전공");

    private final CourseCollege courseCollege;
    private final String name;
}
