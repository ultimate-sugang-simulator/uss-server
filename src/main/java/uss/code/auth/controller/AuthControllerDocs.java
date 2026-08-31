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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.dto.response.EmailAvailabilityResponse;
import uss.code.global.annotation.ParamValidation;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Auth API", description = "인증 관련 API")
public interface AuthControllerDocs {

    @Operation(summary = "회원가입", description = "이메일, 비밀번호와 프로필을 받아 회원을 생성하고 액세스 토큰을 발급합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "✅ 회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 유효하지 않은 입력",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "존재하지 않는 열거값 (단과대학, 학과, 학년, 학적 상태)",
                                            value = "{\"code\" : \"GLB-002\", \"message\" : \"유효하지 않은 열거타입이에요.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "학과와 단과대학 불일치",
                                            value = "{\"code\" : \"MEM-004\", \"message\" : \"학과의 소속 단과대학과 일치하지 않아요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "🚨 이메일 중복",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이메일 중복",
                                            value = "{\"code\" : \"MEM-003\", \"message\" : \"이미 사용 중인 이메일이에요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/sign-up")
    ResponseEntity<AuthTokenResponse> signUp(@Valid @RequestBody final SignUpRequest request);

    @Operation(summary = "이메일 사용 가능 여부 조회", description = "이메일이 이미 가입에 쓰이고 있는지 확인합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 이메일 사용 가능 여부 조회 성공")
    })
    @GetMapping("/email-availability")
    ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(
            @ParamValidation(maxLength = 255)
            @RequestParam("email") final String email
    );

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.<br>" +
            "🔓 <strong>Jwt 불필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 로그인 성공"),
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
            @ApiResponse(responseCode = "401", description = "🚨 비밀번호 불일치",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "비밀번호 불일치",
                                            value = "{\"code\" : \"MEM-002\", \"message\" : \"비밀번호가 일치하지 않아요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody final LoginRequest request);

    @Operation(summary = "액세스 토큰 재발급", description = "만료된 액세스 토큰으로 새 액세스 토큰을 발급받습니다.<br>" +
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
                                            name = "액세스 토큰 유효하지 않음",
                                            value = "{\"code\" : \"AUTH-002\", \"message\" : \"액세스 토큰이 유효하지 않아요.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "액세스 토큰 형식 오류",
                                            value = "{\"code\" : \"AUTH-003\", \"message\" : \"액세스 토큰 형식이 올바르지 않아요.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "액세스 토큰 서명 오류",
                                            value = "{\"code\" : \"AUTH-004\", \"message\" : \"액세스 토큰 서명이 유효하지 않아요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "🚨 사용자 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "사용자 조회 실패",
                                            value = "{\"code\" : \"MEM-001\", \"message\" : \"사용자를 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/re-issue")
    ResponseEntity<AuthTokenResponse> reIssue(
            @RequestHeader(value = "access-token", required = false) final String accessToken
    );
}