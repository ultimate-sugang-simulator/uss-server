package uss.code.admin.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InuCourseResponse(
        @JsonProperty("YEAR") String year,

        @JsonProperty("TERM_CODE") String termCode,

        @JsonProperty("COURSE_CODE") String courseCode,

        @JsonProperty("HAKSU_CODE") String haksuCode,

        @JsonProperty("COURSE_NM_KOR") String titleKr,

        @JsonProperty("COURSE_NM_ENG") String titleEn,

        @JsonProperty("COLLEGE_CODE") String collegeCode,

        @JsonProperty("DEPT_CODE") String departmentCode,

        @JsonProperty("HY_CODE") String gradeCode,

        @JsonProperty("HY_NAME") String gradeName,

        @JsonProperty("ISU_CODE") String classificationCode,

        @JsonProperty("ISU_NAME") String classificationName,

        @JsonProperty("ISU_FLD_CODE") String areaCode,

        @JsonProperty("ISU_FLD_NAME") String areaName,

        @JsonProperty("CREDIT") Integer credits,

        @JsonProperty("ENGLISH_YN") String englishYn,

        @JsonProperty("ENGLISH_CODE") String englishCode,

        @JsonProperty("ENGLISH_NAME") String englishName,

        @JsonProperty("SUUP_TYPE_CODE") String typeCode,

        @JsonProperty("SUUP_TYPE_NAME") String typeName,

        @JsonProperty("CNCTR_ISU_CODE") String concentrationCode,

        @JsonProperty("CNCTR_ISU_NAME") String concentrationName,

        @JsonProperty("HUSS_COURSE_YN") String hussCourseYn
) {}
