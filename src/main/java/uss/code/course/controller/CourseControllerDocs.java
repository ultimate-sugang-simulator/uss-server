package uss.code.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uss.code.auth.annotation.Auth;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCoursesResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.dto.response.SearchedCoursesResponse;
import uss.code.global.annotation.ParamValidation;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Course API", description = "과목 조회 관련 API")
public interface CourseControllerDocs {

    @Operation(summary = "전공 과목 조회", description = "사용자의 전공에 해당하는 과목 목록을 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 전공 과목 조회 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 사용자 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "사용자 조회 실패",
                                            value = "{\"error\" : \"MEMBER_1010\", \"message\" : \"사용자를 찾을 수 없습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/major")
    ResponseEntity<MajorCoursesResponse> getMajorCourses(@Auth final long memberId);

    @Operation(summary = "교양 과목 조회", description = "특정 교양 영역의 과목 목록을 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 교양 과목 조회 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 유효하지 않은 교양 영역",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 교양 영역",
                                            value = "{\"error\" : \"INVALID_GENERAL_EDUCATION_AREA_2000\", \"message\" : \"유효하지 않은 교양 영역입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/general-education")
    ResponseEntity<GeneralEducationCoursesResponse> getGeneralEducationCourses(
            @ParamValidation(maxLength = 30)
            @RequestParam("course-area") final String courseArea
    );

    @Operation(summary = "타학과 과목 조회", description = "특정 학과의 과목 목록을 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 타학과 과목 조회 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 유효하지 않은 학과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 학과",
                                            value = "{\"error\" : \"INVALID_ENUM_TYPE_8888\", \"message\" : \"유효하지 않은 열거타입입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/other-department")
    ResponseEntity<MajorCoursesResponse> getOtherDepartmentCourses(
            @ParamValidation(maxLength = 40)
            @RequestParam("department") final String department
    );

    @Operation(summary = "연계전공 과목 조회", description = "특정 연계전공의 과목 목록을 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 연계전공 과목 조회 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 유효하지 않은 연계전공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 연계전공",
                                            value = "{\"error\" : \"INVALID_INTERDISCIPLINARY_DEPARTMENT_2001\", \"message\" : \"유효하지 않은 연계전공과목입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/interdisciplinary-major")
    ResponseEntity<InterdisciplinaryMajorCoursesResponse> getInterdisciplinaryMajorCourses(
            @ParamValidation(maxLength = 40)
            @RequestParam("department") final String department
    );

    @Operation(summary = "과목 검색", description = "키워드로 과목을 검색합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 과목 검색 성공")
    })
    @GetMapping("/search")
    ResponseEntity<SearchedCoursesResponse> searchCourses(
            @ParamValidation(maxLength = 70)
            @RequestParam("keyword") final String keyword
    );
}
