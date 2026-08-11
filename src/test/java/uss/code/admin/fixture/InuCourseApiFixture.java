package uss.code.admin.fixture;

import uss.code.admin.dto.common.InuCourseResponse;
import uss.code.admin.dto.common.InuTimetableResponse;

public class InuCourseApiFixture {

    public static final String YEAR = "2026";
    public static final String TERM_CODE = "20";

    private static final String DEFAULT_TITLE_EN = "Data Structure";

    private static final String COLLEGE_CODE = "I000";
    private static final String DEPARTMENT_CODE = "0000077";
    private static final String GRADE_CODE = "2";
    private static final String GRADE_NAME = "2";
    private static final String CLASSIFICATION_CODE = "31";
    private static final String CLASSIFICATION_NAME = "전공핵심";
    private static final String AREA_CODE = "34";
    private static final String AREA_NAME = "전공핵심";
    private static final int CREDITS = 3;
    private static final String NOT_ENGLISH_YN = "N";
    private static final String NOT_ENGLISH_CODE = "0";
    private static final String NOT_ENGLISH_NAME = "비대상";
    private static final String TYPE_CODE = "1";
    private static final String TYPE_NAME = "강의(이론)";
    private static final String CONCENTRATION_CODE = "0";
    private static final String CONCENTRATION_NAME = "일반(1~15주)";
    private static final String NOT_HUSS_YN = "N";

    private static final String DAY_CODE = "1";
    private static final String PERIOD_CODE = "B01";
    private static final String PERIOD_NAME = "1-2A";
    private static final String START_TIME = "09:00";
    private static final String END_TIME = "10:15";
    private static final String ROOM_NAME = "제7호관 정보기술대학-407 강의실";

    public static InuCourseResponse createCourse(
            final String haksuCode,
            final String titleKr
    ) {
        return createCourse(haksuCode, titleKr, CLASSIFICATION_NAME, CREDITS, COLLEGE_CODE, DEPARTMENT_CODE, AREA_CODE);
    }

    public static InuCourseResponse createCourseWithClassification(
            final String haksuCode,
            final String titleKr,
            final String classificationName
    ) {
        return createCourse(haksuCode, titleKr, classificationName, CREDITS, COLLEGE_CODE, DEPARTMENT_CODE, AREA_CODE);
    }

    public static InuCourseResponse createCourseWithCredits(
            final String haksuCode,
            final String titleKr,
            final int credits
    ) {
        return createCourse(haksuCode, titleKr, CLASSIFICATION_NAME, credits, COLLEGE_CODE, DEPARTMENT_CODE, AREA_CODE);
    }

    public static InuCourseResponse createCourseWithDepartmentCode(
            final String haksuCode,
            final String titleKr,
            final String departmentCode
    ) {
        return createCourse(haksuCode, titleKr, CLASSIFICATION_NAME, CREDITS, COLLEGE_CODE, departmentCode, AREA_CODE);
    }

    public static InuCourseResponse createCourseWithAreaCode(
            final String haksuCode,
            final String titleKr,
            final String areaCode
    ) {
        return createCourse(haksuCode, titleKr, CLASSIFICATION_NAME, CREDITS, COLLEGE_CODE, DEPARTMENT_CODE, areaCode);
    }

    public static InuCourseResponse createCourse(
            final String haksuCode,
            final String titleKr,
            final String classificationName,
            final int credits,
            final String collegeCode,
            final String departmentCode,
            final String areaCode
    ) {
        return new InuCourseResponse(
                YEAR,
                TERM_CODE,
                haksuCode.substring(0, 7),
                haksuCode,
                titleKr,
                DEFAULT_TITLE_EN,
                collegeCode,
                departmentCode,
                GRADE_CODE,
                GRADE_NAME,
                CLASSIFICATION_CODE,
                classificationName,
                areaCode,
                AREA_NAME,
                credits,
                NOT_ENGLISH_YN,
                NOT_ENGLISH_CODE,
                NOT_ENGLISH_NAME,
                TYPE_CODE,
                TYPE_NAME,
                CONCENTRATION_CODE,
                CONCENTRATION_NAME,
                NOT_HUSS_YN
        );
    }

    public static InuTimetableResponse createTimetable(final String haksuCode) {
        return createTimetable(haksuCode, DAY_CODE, ROOM_NAME);
    }

    public static InuTimetableResponse createTimetable(
            final String haksuCode,
            final String dayCode,
            final String roomName
    ) {
        return new InuTimetableResponse(
                haksuCode,
                dayCode,
                PERIOD_CODE,
                PERIOD_NAME,
                START_TIME,
                END_TIME,
                roomName
        );
    }

    public static InuTimetableResponse createTimetableWithTime(
            final String haksuCode,
            final String startTime,
            final String endTime
    ) {
        return new InuTimetableResponse(
                haksuCode,
                DAY_CODE,
                PERIOD_CODE,
                PERIOD_NAME,
                startTime,
                endTime,
                ROOM_NAME
        );
    }
}
