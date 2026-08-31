package uss.code.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import uss.code.auth.annotation.Auth;
import uss.code.global.exception.dto.response.ErrorResponse;
import uss.code.registration.dto.response.RegistrationCoursesResponse;

@Tag(name = "Registration API", description = "수강신청 관련 API")
public interface RegistrationControllerDocs {

    @Operation(summary = "수강신청 내역 조회", description = "사용자의 수강신청 내역을 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 수강신청 내역 조회 성공")
    })
    @GetMapping
    ResponseEntity<RegistrationCoursesResponse> getRegistrationCourse(@Auth final long memberId);

    @Operation(summary = "수강신청 추가", description = "특정 과목을 수강신청합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 수강신청 추가 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 사용자 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "사용자 조회 실패",
                                            value = "{\"code\" : \"MEM-001\", \"message\" : \"사용자를 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "🚨 과목 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "과목 조회 실패",
                                            value = "{\"code\" : \"CRS-003\", \"message\" : \"과목을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 수강 정원 마감",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "수강 정원 마감",
                                            value = "{\"code\" : \"REG-001\", \"message\" : \"수강 정원이 마감됐어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 과목 중복 신청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "과목 중복 신청",
                                            value = "{\"code\" : \"REG-003\", \"message\" : \"이미 신청된 과목이에요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 학점 제한 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "학점 제한 초과",
                                            value = "{\"code\" : \"REG-002\", \"message\" : \"최대 이수 가능 학점을 초과했어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "🚨 시간표 충돌",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "시간표 충돌",
                                            value = "{\"code\" : \"CRS-005\", \"message\" : \"과목 시간표가 겹쳐요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 과목 유형 제한 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "과목 유형 제한 초과",
                                            value = "{\"code\" : \"CRS-006\", \"message\" : \"해당 과목 유형의 등록 제한을 초과했어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{courseId}")
    ResponseEntity<Void> registerCourse(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    );

    @Operation(summary = "수강신청 취소", description = "특정 과목의 수강신청을 취소합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 수강신청 취소 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 수강신청 내역 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "수강신청 내역 조회 실패",
                                            value = "{\"code\" : \"REG-004\", \"message\" : \"수강신청한 과목을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "🚨 과목 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "과목 조회 실패",
                                            value = "{\"code\" : \"CRS-003\", \"message\" : \"과목을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{courseId}")
    ResponseEntity<Void> deleteRegisteredCourse(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    );
}
