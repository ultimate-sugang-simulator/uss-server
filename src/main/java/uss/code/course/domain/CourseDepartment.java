package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.MemberDepartment;

import static uss.code.global.exception.domain.BusinessExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.BusinessExceptionCode.INVALID_INTERDISCIPLINARY_DEPARTMENT;

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
    ECONOMICS_NIGHT(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "경제학과(야)"),
    TRADE(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "무역학부"),
    TRADE_NIGHT(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "무역학부(야)"),
    CONSUMER_SCIENCE(CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "소비자학과"),

    // 공과대학
    MECHANICAL_ENGINEERING(CourseCollege.ENGINEERING, "기계공학과"),
    ELECTRICAL_ENGINEERING(CourseCollege.ENGINEERING, "전기공학과"),
    ELECTRONICS_ENGINEERING(CourseCollege.ENGINEERING, "전자공학과"),
    ELECTRONICS_ENGINEERING_DEPARTMENT(CourseCollege.ENGINEERING, "전자공학부"),
    ELECTRONICS_ENGINEERING_MAJOR(CourseCollege.ENGINEERING, "전자공학전공"),
    SEMICONDUCTOR_CONVERGENCE(CourseCollege.ENGINEERING, "반도체융합전공"),
    INDUSTRIAL_MANAGEMENT(CourseCollege.ENGINEERING, "산업경영공학과"),
    MATERIAL_SCIENCE(CourseCollege.ENGINEERING, "신소재공학과"),
    SAFETY_ENGINEERING(CourseCollege.ENGINEERING, "안전공학과"),
    ENERGY_CHEMICAL(CourseCollege.ENGINEERING, "에너지화학공학과"),
    BIO_ROBOTICS_ENGINEERING(CourseCollege.ENGINEERING, "바이오-로봇시스템공학과"),

    // 정보기술대학
    COMPUTER_ENGINEERING(CourseCollege.INFORMATION_TECHNOLOGY, "컴퓨터공학부"),
    INFORMATION_COMMUNICATION_ENGINEERING(CourseCollege.INFORMATION_TECHNOLOGY, "정보통신공학과"),
    EMBEDDED_SYSTEM(CourseCollege.INFORMATION_TECHNOLOGY, "임베디드시스템공학과"),

    // 경영대학
    BUSINESS_ADMINISTRATION(CourseCollege.BUSINESS, "경영학부"),
    DATA_SCIENCE(CourseCollege.BUSINESS, "데이터과학과"),
    TAX_ACCOUNTING(CourseCollege.BUSINESS, "세무회계학과"),

    // 예술체육대학
    FINE_ARTS(CourseCollege.ARTS_PHYSICAL_EDUCATION, "조형예술학부"),
    KOREAN_PAINTING(CourseCollege.ARTS_PHYSICAL_EDUCATION, "한국화전공"),
    WESTERN_PAINTING(CourseCollege.ARTS_PHYSICAL_EDUCATION, "서양화전공"),
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
    URBAN_ENVIRONMENT_ENGINEERING_DEPARTMENT(CourseCollege.URBAN_SCIENCE, "도시환경공학부"),
    CIVIL_ENVIRONMENT_ENGINEERING(CourseCollege.URBAN_SCIENCE, "건설환경공학전공"),
    ENVIRONMENT_ENGINEERING(CourseCollege.URBAN_SCIENCE, "환경공학전공"),
    URBAN_ENGINEERING(CourseCollege.URBAN_SCIENCE, "도시공학과"),
    URBAN_ARCHITECTURE_DEPARTMENT(CourseCollege.URBAN_SCIENCE, "도시건축학부"),
    ARCHITECTURE_ENGINEERING(CourseCollege.URBAN_SCIENCE, "건축공학전공"),
    URBAN_ARCHITECTURE(CourseCollege.URBAN_SCIENCE, "도시건축학전공"),

    // 생명과학기술대학
    LIFE_SCIENCE_DEPARTMENT(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명과학부"),
    LIFE_SCIENCE(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명과학전공"),
    MOLECULAR_LIFE_SCIENCE(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "분자의생명전공"),
    BIOENGINEERING_DEPARTMENT(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명공학부"),
    BIOENGINEERING(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명공학전공"),
    NANO_BIOENGINEERING(CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "나노바이오공학전공"),

    // 단과대구분없음
    NORTHEAST_ASIAN_TRADE(CourseCollege.NONE, "동북아국제통상전공"),
    SMART_LOGISTICS_ENGINEERING(CourseCollege.NONE, "스마트물류공학전공"),
    IBE(CourseCollege.NONE, "IBE전공"),

    // 융합자유전공대학
    LIBERAL_ARTS(CourseCollege.LIBERAL_ARTS_COLLEGE, "자유전공학부"),

    // 법학부
    LAW(CourseCollege.LAW, "법학부"),

    // 교양
    GENERAL_EDUCATION(CourseCollege.GENERAL_EDUCATION, "교양"),

    // 교직
    TEACHING(CourseCollege.TEACHING, "교직"),

    // 일선
    GENERAL_ELECTIVE(CourseCollege.GENERAL_ELECTIVE, "일선"),

    // 군사학
    MILITARY(CourseCollege.MILITARY, "군사학"),

    // 기타
    OPTOELECTRONICS(CourseCollege.ETC, "광전자공학전공(연계)"),
    LOGISTICS(CourseCollege.ETC, "물류학전공(연계)"),
    INTERNATIONAL_DEVELOPMENT_COOPERATION(CourseCollege.ETC, "국제개발협력연계전공"),
    CREATIVE_DESIGN(CourseCollege.ETC, "창의적디자인연계전공"),
    HUMANITIES_CULTURE_ART_PLANNING(CourseCollege.ETC, "인문문화예술기획연계전공"),
    SOCIAL_DATA_SCIENCE(CourseCollege.ETC, "소셜데이터사이언스연계전공"),
    FUTURE_AUTOMOBILE(CourseCollege.ETC, "미래자동차연계전공"),
    FUTURE_EDUCATION_DESIGN(CourseCollege.ETC, "미래교육디자인연계전공"),
    HUSS_INCLUSIVE_SOCIETY_INITIATIVE(CourseCollege.ETC, "HUSS포용사회이니셔티브학부"),
    HUSS_OTHER_UNIVERSITY(CourseCollege.ETC, "HUSS(타대학)");

    private final CourseCollege courseCollege;
    private final String name;

    public static CourseDepartment from(final String courseDepartment) {
        try{
            return CourseDepartment.valueOf(courseDepartment.toUpperCase());
        }catch (IllegalArgumentException e){
            throw new RestApiException(INVALID_ENUM_TYPE);
        }
    }

    public static CourseDepartment from(final MemberDepartment memberDepartment){
        try{
            return CourseDepartment.valueOf(memberDepartment.name());
        }catch (IllegalArgumentException e){
            throw new RestApiException(INVALID_ENUM_TYPE);
        }
    }

    public static CourseDepartment fromInterdisciplinary(final String department){
        final CourseDepartment courseDepartment = CourseDepartment.from(department);

        if(!courseDepartment.isInterdisciplinary()){
            throw new RestApiException(INVALID_INTERDISCIPLINARY_DEPARTMENT);
        }

        return courseDepartment;
    }


    private boolean isInterdisciplinary(){
        return this == OPTOELECTRONICS ||
                this == LOGISTICS ||
                this == INTERNATIONAL_DEVELOPMENT_COOPERATION ||
                this == CREATIVE_DESIGN ||
                this == HUMANITIES_CULTURE_ART_PLANNING ||
                this == SOCIAL_DATA_SCIENCE ||
                this == FUTURE_AUTOMOBILE ||
                this == FUTURE_EDUCATION_DESIGN ||
                this == HUSS_INCLUSIVE_SOCIETY_INITIATIVE ||
                this == HUSS_OTHER_UNIVERSITY;
    }
}
