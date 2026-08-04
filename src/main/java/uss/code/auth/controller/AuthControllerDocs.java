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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Auth API", description = "인증 관련 API")
public interface AuthControllerDocs {

    @Operation(summary = "로그인", description = "학번과 비밀번호로 로그인합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 로그인 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 사용자 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "사용자 조회 실패",
                                            value = "{\"code\" : 1010, \"message\" : \"사용자를 찾을 수 없습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "🚨 비밀번호 불일치",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "비밀번호 불일치",
                                            value = "{\"code\" : 1012, \"message\" : \"비밀번호가 일치하지 않습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody final LoginRequest request);
}