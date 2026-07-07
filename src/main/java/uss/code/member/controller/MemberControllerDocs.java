package uss.code.member.controller;

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
import uss.code.auth.annotation.Auth;
import uss.code.global.exception.dto.response.ErrorResponse;
import uss.code.member.dto.response.MemberProfileResponse;

@Tag(name = "Member API", description = "회원 관련 API")
public interface MemberControllerDocs {

    @Operation(summary = "회원 프로필 조회", description = "사용자의 프로필 정보를 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 프로필 조회 성공"),
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
    @GetMapping("/profile")
    ResponseEntity<MemberProfileResponse> getProfile(@Auth final long memberId);
}
