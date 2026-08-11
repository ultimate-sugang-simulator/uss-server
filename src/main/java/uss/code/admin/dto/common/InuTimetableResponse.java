package uss.code.admin.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InuTimetableResponse(
        @JsonProperty("HAKSU_CODE") String haksuCode,

        @JsonProperty("DAY_CODE") String dayCode,

        @JsonProperty("LECTM_CODE") String periodCode,

        @JsonProperty("LECTM_NAME") String periodName,

        @JsonProperty("LECTM_START") String startTime,

        @JsonProperty("LECTM_END") String endTime,

        @JsonProperty("ROOM_NAME") String roomName
) {}
