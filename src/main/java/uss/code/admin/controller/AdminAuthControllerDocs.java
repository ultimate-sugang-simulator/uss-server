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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uss.code.admin.dto.request.AdminLoginRequest;
import uss.code.admin.dto.response.AdminTokenResponse;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Admin Auth API", description = "백오피스 관리자 인증 관련 API")
public interface AdminAuthControllerDocs {

    @Operation(summary = "관리자 로그인", description = "아이디와 비밀번호로 백오피스 액세스 토큰을 발급받습니다.<br>" +
            "만료는 2시간이며 리프레시 토큰은 발급하지 않습니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 로그인 성공"),
            @ApiResponse(responseCode = "401", description = "🚨 아이디 없음 또는 비밀번호 불일치",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "로그인 실패",
                                            value = "{\"code\" : \"ADM-001\", \"message\" : \"아이디나 비밀번호가 맞지 않아요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    ResponseEntity<AdminTokenResponse> login(@Valid @RequestBody final AdminLoginRequest request);

    @Operation(summary = "관리자 토큰 재발급", description = "만료된 액세스 토큰으로 새 액세스 토큰을 발급받습니다.<br>" +
            "서명이 유효하면 만료 여부와 관계없이 발급됩니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "🚨 액세스 토큰 누락 또는 유효하지 않음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "액세스 토큰 누락",
                                            value = "{\"code\" : \"AUTH-001\", \"message\" : \"액세스 토큰이 누락됐어요.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "액세스 토큰 서명 오류",
                                            value = "{\"code\" : \"AUTH-004\", \"message\" : \"액세스 토큰 서명이 유효하지 않아요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "🚨 관리자 토큰이 아님",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "관리자 권한 없음",
                                            value = "{\"code\" : \"ADM-003\", \"message\" : \"관리자 권한이 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "🚨 토큰의 관리자가 존재하지 않음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "관리자 조회 실패",
                                            value = "{\"code\" : \"ADM-002\", \"message\" : \"관리자를 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/refresh")
    ResponseEntity<AdminTokenResponse> refresh(
            @RequestHeader(value = "access-token", required = false) final String accessToken
    );
}
