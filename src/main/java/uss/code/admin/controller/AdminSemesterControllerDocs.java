package uss.code.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uss.code.admin.dto.request.SystemSemesterRequest;
import uss.code.admin.dto.response.SystemSemesterResponse;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Admin Semester API", description = "백오피스 표시 학기 관련 API")
public interface AdminSemesterControllerDocs {

    @Operation(summary = "표시 학기 조회", description = "프론트엔드에 노출할 학기를 조회합니다.<br>" +
            "적재된 강의 학기와는 무관합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 표시 학기 조회 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 표시 학기 설정 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "표시 학기 설정 조회 실패",
                                            value = "{\"code\" : 5100, \"message\" : \"표시 학기 설정을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    ResponseEntity<SystemSemesterResponse> getSystemSemester();

    @Operation(summary = "표시 학기 변경", description = "프론트엔드에 노출할 학기를 변경합니다.<br>" +
            "강의 데이터에는 어떤 영향도 주지 않습니다.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 표시 학기 변경 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 유효하지 않은 학기 값",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 열거타입",
                                            value = "{\"code\" : 8888, \"message\" : \"유효하지 않은 열거타입입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "🚨 표시 학기 설정 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "표시 학기 설정 조회 실패",
                                            value = "{\"code\" : 5100, \"message\" : \"표시 학기 설정을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping
    ResponseEntity<SystemSemesterResponse> changeSystemSemester(
            @Valid @RequestBody final SystemSemesterRequest request
    );
}
