package uss.code.cart.controller;

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
import uss.code.cart.dto.response.CartedCoursesResponse;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Cart API", description = "장바구니 관련 API")
public interface CartControllerDocs {

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니에 담긴 과목 목록을 조회합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 장바구니 조회 성공")
    })
    @GetMapping
    ResponseEntity<CartedCoursesResponse> getCartedCourse(@Auth final long memberId);

    @Operation(summary = "장바구니 추가", description = "특정 과목을 장바구니에 추가합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 장바구니 추가 성공"),
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
            @ApiResponse(responseCode = "404", description = "🚨 과목 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "과목 조회 실패",
                                            value = "{\"code\" : 2002, \"message\" : \"과목을 찾을 수 없습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 장바구니 제한 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "장바구니 제한 초과",
                                            value = "{\"code\" : 3001, \"message\" : \"장바구니는 최대 10개의 과목을 담을 수 있습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 장바구니 중복 과목",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "장바구니 중복 과목",
                                            value = "{\"code\" : 3004, \"message\" : \"이미 장바구니에 담긴 과목입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "🚨 시간표 충돌",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "시간표 충돌",
                                            value = "{\"code\" : 3002, \"message\" : \"과목 시간표가 겹칩니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "🚨 과목 유형 제한 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "과목 유형 제한 초과",
                                            value = "{\"code\" : 3003, \"message\" : \"해당 과목 유형의 등록 제한을 초과했습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{courseId}")
    ResponseEntity<Void> addCart(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    );

    @Operation(summary = "장바구니 삭제", description = "특정 과목을 장바구니에서 삭제합니다.<br>" +
            "🔐 <strong>Jwt 필요</strong><br>")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "✅ 장바구니 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 장바구니 과목 조회 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "장바구니 과목 조회 실패",
                                            value = "{\"code\" : 3000, \"message\" : \"장바구니에 담은 과목을 찾을 수 없습니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{courseId}")
    ResponseEntity<Void> deleteCartedCourse(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    );
}
