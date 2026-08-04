package uss.code.auth.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uss.code.auth.dto.request.VerificationCodeSendRequest;
import uss.code.auth.dto.request.VerificationCodeVerifyRequest;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Email Verification Code API", description = "이메일 인증 관련 API")
public interface EmailVerificationCodeControllerDocs {

    @Operation(summary = "인증코드 전송", description = "이메일로 인증코드를 전송합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "✅ 인증코드 전송 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 이미 존재하는 사용자",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이미 존재하는 사용자",
                                            value = "{\"code\" : 1011, \"message\" : \"이미 존재하는 사용자입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 이미 인증 진행 중",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이미 인증 진행 중",
                                            value = "{\"code\" : 1015, \"message\" : \"이미 인증이 진행 중입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 이미 인증 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이미 인증 완료",
                                            value = "{\"code\" : 1017, \"message\" : \"이미 인증이 완료되었습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    ResponseEntity<Void> sendVerificationCode(@Valid @RequestBody final VerificationCodeSendRequest request);

    @Operation(summary = "인증코드 재전송", description = "이메일로 인증코드를 재전송합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "✅ 인증코드 재전송 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 이메일 인증 정보 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이메일 인증 정보 조회 실패",
                                            value = "{\"code\" : 1014, \"message\" : \"이메일 인증 정보를 찾을 수 없습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 이미 인증 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이미 인증 완료",
                                            value = "{\"code\" : 1017, \"message\" : \"이미 인증이 완료되었습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 재전송 횟수 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "재전송 횟수 초과",
                                            value = "{\"code\" : 1021, \"message\" : \"인증코드 재전송 횟수를 초과했습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/re")
    ResponseEntity<Void> resendVerificationCode(@Valid @RequestBody final VerificationCodeSendRequest request);

    @Operation(summary = "인증코드 검증", description = "이메일로 전송된 인증코드를 검증합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 인증코드 검증 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 이메일 인증 정보 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이메일 인증 정보 조회 실패",
                                            value = "{\"code\" : 1014, \"message\" : \"이메일 인증 정보를 찾을 수 없습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 이미 인증 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이미 인증 완료",
                                            value = "{\"code\" : 1017, \"message\" : \"이미 인증이 완료되었습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 인증코드 만료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "인증코드 만료",
                                            value = "{\"code\" : 1019, \"message\" : \"인증코드가 만료되었습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 인증 실패 횟수 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "인증 실패 횟수 초과",
                                            value = "{\"code\" : 1022, \"message\" : \"인증 실패 횟수를 초과했습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 인증코드 불일치",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "인증코드 불일치",
                                            value = "{\"code\" : 1020, \"message\" : \"인증코드가 일치하지 않습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping
    ResponseEntity<Void> verifyCode(@Valid @RequestBody final VerificationCodeVerifyRequest request);
}