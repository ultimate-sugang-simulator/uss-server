package uss.code.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uss.code.global.exception.domain.RestApiException;
import uss.code.member.domain.MemberDepartment;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ENUM_TYPE;
import static uss.code.global.exception.domain.ExceptionCode.INVALID_INTERDISCIPLINARY_DEPARTMENT;

@Getter
@RequiredArgsConstructor
public enum CourseDepartment {
    // 인문대학
    KOREAN_LITERATURE("AIA1", CourseCollege.HUMANITIES, "국어국문학과", MemberDepartment.KOREAN_LITERATURE),
    ENGLISH_LITERATURE("AIB1", CourseCollege.HUMANITIES, "영어영문학과", MemberDepartment.ENGLISH_LITERATURE),
    GERMAN_STUDIES("AIE1", CourseCollege.HUMANITIES, "독어독문학과", MemberDepartment.GERMAN_STUDIES),
    FRENCH_STUDIES("AIF1", CourseCollege.HUMANITIES, "불어불문학과", MemberDepartment.FRENCH_STUDIES),
    JAPANESE_LITERATURE("0000793", CourseCollege.HUMANITIES, "일본지역문화학과", MemberDepartment.JAPANESE_LITERATURE),
    CHINESE_STUDIES("AID1", CourseCollege.HUMANITIES, "중어중국학과", MemberDepartment.CHINESE_STUDIES),

    // 자연과학대학
    MATHEMATICS("BKA1", CourseCollege.NATURAL_SCIENCES, "수학과", MemberDepartment.MATHEMATICS),
    PHYSICS("BKB1", CourseCollege.NATURAL_SCIENCES, "물리학과", MemberDepartment.PHYSICS),
    CHEMISTRY("BKC1", CourseCollege.NATURAL_SCIENCES, "화학과", MemberDepartment.CHEMISTRY),
    FASHION_INDUSTRY("BLB1", CourseCollege.NATURAL_SCIENCES, "패션산업학과", MemberDepartment.FASHION_INDUSTRY),
    MARINE_SCIENCE("0000189", CourseCollege.NATURAL_SCIENCES, "해양학과", MemberDepartment.MARINE_SCIENCE),

    // 사회과학대학
    SOCIAL_WELFARE("0000144", CourseCollege.SOCIAL_SCIENCES, "사회복지학과", MemberDepartment.SOCIAL_WELFARE),
    MEDIA_COMMUNICATION("0000794", CourseCollege.SOCIAL_SCIENCES, "미디어커뮤니케이션학과", MemberDepartment.MEDIA_COMMUNICATION),
    LIBRARY_INFO("0000053", CourseCollege.SOCIAL_SCIENCES, "문헌정보학과", MemberDepartment.LIBRARY_INFO),
    CREATIVE_HRD("0000054", CourseCollege.SOCIAL_SCIENCES, "창의인재개발학과", MemberDepartment.CREATIVE_HRD),

    // 글로벌정경대학
    PUBLIC_ADMINISTRATION("0000698", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "행정학과", MemberDepartment.PUBLIC_ADMINISTRATION),
    POLITICS_DIPLOMACY("0000699", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "정치외교학과", MemberDepartment.POLITICS_DIPLOMACY),
    ECONOMICS("0000700", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "경제학과", MemberDepartment.ECONOMICS),
    ECONOMICS_NIGHT("0000701", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "경제학과(야)", MemberDepartment.ECONOMICS_NIGHT),
    TRADE("", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "무역학부", MemberDepartment.GLOBAL_TRADE_SERVICE),
    TRADE_NIGHT("0000703", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "무역학부(야)", MemberDepartment.TRADE_NIGHT),
    GLOBAL_TRADE_SERVICE("0000913", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "Global Trade & Service학부", MemberDepartment.GLOBAL_TRADE_SERVICE),
    CONSUMER_SCIENCE("0000704", CourseCollege.COMMERCE_PUBLIC_AFFAIRS, "소비자학과", MemberDepartment.CONSUMER_SCIENCE),

    // 공과대학
    MECHANICAL_ENGINEERING("0000459", CourseCollege.ENGINEERING, "기계공학과", MemberDepartment.MECHANICAL_ENGINEERING),
    ELECTRICAL_ENGINEERING("EPB1", CourseCollege.ENGINEERING, "전기공학과", MemberDepartment.ELECTRICAL_ENGINEERING),
    ELECTRONICS_ENGINEERING("EPC1", CourseCollege.ENGINEERING, "전자공학과", MemberDepartment.ELECTRONICS_ENGINEERING_SCHOOL),
    ELECTRONICS_ENGINEERING_SCHOOL("0000813", CourseCollege.ENGINEERING, "전자공학부", MemberDepartment.ELECTRONICS_ENGINEERING_SCHOOL),
    ELECTRONICS_ENGINEERING_MAJOR("0000828", CourseCollege.ENGINEERING, "전자공학전공", MemberDepartment.ELECTRONICS_ENGINEERING_SCHOOL),
    SEMICONDUCTOR_CONVERGENCE_MAJOR("0000829", CourseCollege.ENGINEERING, "반도체융합전공", MemberDepartment.ELECTRONICS_ENGINEERING_SCHOOL),
    INDUSTRIAL_MANAGEMENT("EPG1", CourseCollege.ENGINEERING, "산업경영공학과", MemberDepartment.INDUSTRIAL_MANAGEMENT),
    MATERIAL_SCIENCE("0000076", CourseCollege.ENGINEERING, "신소재공학과", MemberDepartment.MATERIAL_SCIENCE),
    SAFETY_ENGINEERING("0000075", CourseCollege.ENGINEERING, "안전공학과", MemberDepartment.SAFETY_ENGINEERING),
    ENERGY_CHEMICAL("0000055", CourseCollege.ENGINEERING, "에너지화학공학과", MemberDepartment.ENERGY_CHEMICAL),
    BIO_ROBOTICS_ENGINEERING("0000814", CourseCollege.ENGINEERING, "바이오-로봇시스템공학과", MemberDepartment.BIO_ROBOTICS_ENGINEERING),

    // 정보기술대학
    COMPUTER_ENGINEERING("0000077", CourseCollege.INFORMATION_TECHNOLOGY, "컴퓨터공학부", MemberDepartment.COMPUTER_ENGINEERING),
    INFORMATION_COMMUNICATION_ENGINEERING("IAB1", CourseCollege.INFORMATION_TECHNOLOGY, "정보통신공학과", MemberDepartment.INFORMATION_COMMUNICATION_ENGINEERING),
    EMBEDDED_SYSTEM("0000042", CourseCollege.INFORMATION_TECHNOLOGY, "임베디드시스템공학과", MemberDepartment.EMBEDDED_SYSTEM),

    // 경영대학
    BUSINESS_ADMINISTRATION("JA01", CourseCollege.BUSINESS, "경영학부", MemberDepartment.BUSINESS_ADMINISTRATION),
    DATA_SCIENCE("0000812", CourseCollege.BUSINESS, "데이터과학과", MemberDepartment.DATA_SCIENCE),
    TAX_ACCOUNTING("0000057", CourseCollege.BUSINESS, "세무회계학과", MemberDepartment.TAX_ACCOUNTING),

    // 예술체육대학
    FINE_ARTS_SCHOOL("0000192", CourseCollege.ARTS_PHYSICAL_EDUCATION, "조형예술학부", MemberDepartment.FINE_ARTS_SCHOOL),
    KOREAN_PAINTING_MAJOR("0000193", CourseCollege.ARTS_PHYSICAL_EDUCATION, "한국화전공", MemberDepartment.FINE_ARTS_SCHOOL),
    WESTERN_PAINTING_MAJOR("0000194", CourseCollege.ARTS_PHYSICAL_EDUCATION, "서양화전공", MemberDepartment.FINE_ARTS_SCHOOL),
    DESIGN("0000195", CourseCollege.ARTS_PHYSICAL_EDUCATION, "디자인학부", MemberDepartment.DESIGN),
    PERFORMING_ART("0000196", CourseCollege.ARTS_PHYSICAL_EDUCATION, "공연예술학과", MemberDepartment.PERFORMING_ART),
    SPORTS_SCIENCE("0000815", CourseCollege.ARTS_PHYSICAL_EDUCATION, "스포츠과학부", MemberDepartment.SPORTS_SCIENCE),
    HEALTH_EXERCISE("0000191", CourseCollege.ARTS_PHYSICAL_EDUCATION, "운동건강학부", MemberDepartment.HEALTH_EXERCISE),

    // 사범대학
    KOREAN_EDUCATION("0000064", CourseCollege.EDUCATION, "국어교육과", MemberDepartment.KOREAN_EDUCATION),
    ENGLISH_EDUCATION("0000065", CourseCollege.EDUCATION, "영어교육과", MemberDepartment.ENGLISH_EDUCATION),
    JAPANESE_EDUCATION("0000066", CourseCollege.EDUCATION, "일어교육과", MemberDepartment.JAPANESE_EDUCATION),
    MATH_EDUCATION("0000067", CourseCollege.EDUCATION, "수학교육과", MemberDepartment.MATH_EDUCATION),
    PHYSICAL_EDUCATION("0000068", CourseCollege.EDUCATION, "체육교육과", MemberDepartment.PHYSICAL_EDUCATION),
    EARLY_CHILDHOOD_EDUCATION("0000069", CourseCollege.EDUCATION, "유아교육과", MemberDepartment.EARLY_CHILDHOOD_EDUCATION),
    HISTORY_EDUCATION("0000070", CourseCollege.EDUCATION, "역사교육과", MemberDepartment.HISTORY_EDUCATION),
    ETHICS_EDUCATION("0000071", CourseCollege.EDUCATION, "윤리교육과", MemberDepartment.ETHICS_EDUCATION),

    // 도시과학대학
    URBAN_ADMINISTRATION("0000073", CourseCollege.URBAN_SCIENCE, "도시행정학과", MemberDepartment.URBAN_ADMINISTRATION),
    URBAN_ENVIRONMENT_ENGINEERING_SCHOOL("0000034", CourseCollege.URBAN_SCIENCE, "도시환경공학부", MemberDepartment.URBAN_ENVIRONMENT_ENGINEERING_SCHOOL),
    CIVIL_ENVIRONMENT_ENGINEERING_MAJOR("0000156", CourseCollege.URBAN_SCIENCE, "건설환경공학전공", MemberDepartment.URBAN_ENVIRONMENT_ENGINEERING_SCHOOL),
    ENVIRONMENT_ENGINEERING_MAJOR("0000157", CourseCollege.URBAN_SCIENCE, "환경공학전공", MemberDepartment.URBAN_ENVIRONMENT_ENGINEERING_SCHOOL),
    URBAN_ENGINEERING("0000463", CourseCollege.URBAN_SCIENCE, "도시공학과", MemberDepartment.URBAN_ENGINEERING),
    URBAN_ARCHITECTURE_SCHOOL("0000038", CourseCollege.URBAN_SCIENCE, "도시건축학부", MemberDepartment.URBAN_ARCHITECTURE_SCHOOL),
    ARCHITECTURE_ENGINEERING_MAJOR("0000160", CourseCollege.URBAN_SCIENCE, "건축공학전공", MemberDepartment.URBAN_ARCHITECTURE_SCHOOL),
    URBAN_ARCHITECTURE_MAJOR("0000464", CourseCollege.URBAN_SCIENCE, "도시건축학전공", MemberDepartment.URBAN_ARCHITECTURE_SCHOOL),

    // 생명과학기술대학
    LIFE_SCIENCE_SCHOOL("0000183", CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명과학부", MemberDepartment.LIFE_SCIENCE_SCHOOL),
    LIFE_SCIENCE_MAJOR("0000184", CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명과학전공", MemberDepartment.LIFE_SCIENCE_SCHOOL),
    MOLECULAR_LIFE_SCIENCE_MAJOR("0000185", CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "분자의생명전공", MemberDepartment.LIFE_SCIENCE_SCHOOL),
    BIOENGINEERING_SCHOOL("0000186", CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명공학부", MemberDepartment.BIOENGINEERING_SCHOOL),
    BIOENGINEERING_MAJOR("0000187", CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "생명공학전공", MemberDepartment.BIOENGINEERING_SCHOOL),
    NANO_BIOENGINEERING_MAJOR("0000833", CourseCollege.LIFE_SCIENCES_BIOENGINEERING, "나노바이오공학전공", MemberDepartment.BIOENGINEERING_SCHOOL),

    // 단과대구분없음
    NORTHEAST_ASIAN_TRADE_MAJOR("0000817", CourseCollege.NONE, "동북아국제통상전공", MemberDepartment.NORTHEAST_ASIAN_TRADE_MAJOR),
    SMART_LOGISTICS_ENGINEERING_MAJOR("0000818", CourseCollege.NONE, "스마트물류공학전공", MemberDepartment.SMART_LOGISTICS_ENGINEERING_MAJOR),
    IBE_MAJOR("0000832", CourseCollege.NONE, "IBE전공", MemberDepartment.IBE_MAJOR),

    // 융합자유전공대학
    LIBERAL_ARTS("0000838", CourseCollege.LIBERAL_ARTS_COLLEGE, "자유전공학부", MemberDepartment.LIBERAL_ARTS),

    // 법학부
    LAW("0000707", CourseCollege.LAW, "법학부", MemberDepartment.LAW),

    // 교양
    GENERAL_EDUCATION("XAA0", CourseCollege.GENERAL_EDUCATION, "교양"),

    // 교직
    TEACHING("YAA0", CourseCollege.TEACHING, "교직"),

    // 일선
    GENERAL_ELECTIVE("WAA0", CourseCollege.GENERAL_ELECTIVE, "일선"),

    // 군사학
    MILITARY("ZAA0", CourseCollege.MILITARY, "군사학"),

    // 기타
    OPTOELECTRONICS("VAB1", CourseCollege.ETC, "광전자공학전공(연계)"),
    LOGISTICS("VAC1", CourseCollege.ETC, "물류학전공(연계)"),
    INTERNATIONAL_DEVELOPMENT_COOPERATION("", CourseCollege.ETC, "국제개발협력연계전공"),
    CREATIVE_DESIGN("0000616", CourseCollege.ETC, "창의적디자인연계전공"),
    HUMANITIES_CULTURE_ART_PLANNING("0000677", CourseCollege.ETC, "인문문화예술기획연계전공"),
    SOCIAL_DATA_SCIENCE("0000678", CourseCollege.ETC, "소셜데이터사이언스연계전공"),
    FUTURE_AUTOMOBILE("0000789", CourseCollege.ETC, "미래자동차연계전공"),
    FUTURE_EDUCATION_DESIGN("0000849", CourseCollege.ETC, "미래교육디자인연계전공"),
    HUSS_INCLUSIVE_SOCIETY_INITIATIVE("VE00", CourseCollege.ETC, "HUSS포용사회이니셔티브학부"),
    HUSS_OTHER_UNIVERSITY("VEA1", CourseCollege.ETC, "HUSS(타대학)"),
    INTELLIGENT_ROBOT_SYSTEM("0000912", CourseCollege.ETC, "지능형로봇시스템연계전공");

    private final String code;
    private final CourseCollege courseCollege;
    private final String name;
    private final MemberDepartment owner;

    CourseDepartment(
            final String code,
            final CourseCollege courseCollege,
            final String name
    ) {
        this(code, courseCollege, name, null);
    }

    public static Optional<CourseDepartment> tryFromCode(final String code) {
        return Arrays.stream(values())
                .filter(department -> !department.code.isBlank())
                .filter(department -> department.code.equals(code))
                .findFirst();
    }

    public static CourseDepartment fromCode(final String code) {
        return tryFromCode(code)
                .orElseThrow(() -> new RestApiException(INVALID_ENUM_TYPE));
    }

    public static CourseDepartment from(final String courseDepartment) {
        try{
            return CourseDepartment.valueOf(courseDepartment.toUpperCase());
        }catch (IllegalArgumentException e){
            throw new RestApiException(INVALID_ENUM_TYPE);
        }
    }

    public static List<CourseDepartment> ownedBy(final MemberDepartment memberDepartment) {
        return Arrays.stream(values())
                .filter(department -> department.owner == memberDepartment)
                .toList();
    }

    public static List<CourseDepartment> interdisciplinaryValues() {
        return Arrays.stream(values())
                .filter(CourseDepartment::isInterdisciplinary)
                .toList();
    }

    public static CourseDepartment fromInterdisciplinary(final String department){
        final CourseDepartment courseDepartment = CourseDepartment.from(department);

        if(!courseDepartment.isInterdisciplinary()){
            throw new RestApiException(INVALID_INTERDISCIPLINARY_DEPARTMENT);
        }

        return courseDepartment;
    }

    public boolean hasOwner() {
        return owner != null;
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
                this == HUSS_OTHER_UNIVERSITY ||
                this == INTELLIGENT_ROBOT_SYSTEM;
    }
}
