package uss.code.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uss.code.admin.domain.SyncChangeType;
import uss.code.admin.dto.request.SyncJobCreateRequest;
import uss.code.admin.dto.request.SyncPreflightRequest;
import uss.code.admin.dto.response.SyncChangeResponse;
import uss.code.admin.dto.response.SyncJobCreatedResponse;
import uss.code.admin.dto.response.SyncJobDetailResponse;
import uss.code.admin.dto.response.SyncJobResponse;
import uss.code.admin.dto.response.SyncPreflightResponse;
import uss.code.auth.annotation.AdminAuth;
import uss.code.global.annotation.EnumValidation;
import uss.code.global.dto.response.PageResponse;
import uss.code.global.exception.dto.response.ErrorResponse;

@Tag(name = "Admin Sync API", description = "백오피스 강의 동기화 관련 API")
public interface AdminSyncControllerDocs {

    @Operation(summary = "적재 전략 판정", description = "대상 학기를 적재하면 어떤 전략이 적용되고 무엇이 지워지는지 알려줍니다.<br>" +
            "DB를 변경하지 않으며 매 호출마다 다시 셉니다.<br>" +
            "강의가 없으면 INITIAL, 적재 학기와 같으면 UPSERT, 다르면 REPLACE입니다.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 전략 판정 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 유효하지 않은 학기 값",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 열거타입",
                                            value = "{\"code\" : 8888, \"message\" : \"유효하지 않은 열거타입입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/preflight")
    ResponseEntity<SyncPreflightResponse> preflight(@Valid @RequestBody final SyncPreflightRequest request);

    @Operation(summary = "동기화 작업 생성", description = "학교 연계 API에서 강의를 수집해 적재하는 작업을 비동기로 시작합니다.<br>" +
            "expectedStrategy는 판정 API 응답을 그대로 돌려보내는 대조용 값입니다. 서버가 전략을 다시 판정합니다.<br>" +
            "409는 클라이언트가 자동 재시도하지 않습니다. 적재 현황을 다시 조회하세요.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "✅ 작업 생성 성공"),
            @ApiResponse(responseCode = "409", description = "🚨 진행 중 작업 존재 또는 전략 불일치",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "이미 진행 중",
                                            value = "{\"code\" : 5200, \"message\" : \"이미 업데이트가 진행 중이에요.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "전략 불일치",
                                            value = "{\"code\" : 5201, \"message\" : \"데이터가 변경됐어요. 다시 확인해주세요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/jobs")
    ResponseEntity<SyncJobCreatedResponse> createJob(
            @AdminAuth final long adminId,
            @Valid @RequestBody final SyncJobCreateRequest request
    );

    @Operation(summary = "동기화 작업 이력 조회", description = "동기화 작업 이력을 시작 시각 내림차순으로 조회합니다.<br>" +
            "페이지 크기는 10으로 고정입니다. 건수는 성공한 작업에서만 채워집니다.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 이력 조회 성공")
    })
    @GetMapping("/jobs")
    ResponseEntity<PageResponse<SyncJobResponse>> getJobs(
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            @RequestParam(value = "page", defaultValue = "1") final int page
    );

    @Operation(summary = "동기화 작업 상세 조회", description = "작업 상세와 진행 상황을 조회합니다. 진행률 폴링에 씁니다.<br>" +
            "진행 중일 때만 progress가 채워지며 필드는 phase 하나입니다.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 작업 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "🚨 작업 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "작업 조회 실패",
                                            value = "{\"code\" : 5202, \"message\" : \"업데이트 작업을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/jobs/{jobId}")
    ResponseEntity<SyncJobDetailResponse> getJob(@PathVariable("jobId") final long jobId);

    @Operation(summary = "변경 항목 목록 조회", description = "작업이 만든 변경 항목을 학수번호 오름차순으로 조회합니다.<br>" +
            "changeType은 필수입니다. changedFields는 UPDATED에서만, reason은 WARNING에서만 채워집니다.<br>" +
            "🔐 <strong>Jwt 필요</strong> (관리자)<br>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "✅ 변경 항목 조회 성공"),
            @ApiResponse(responseCode = "400", description = "🚨 변경 유형 누락 또는 잘못된 값",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 입력 파라미터",
                                            value = "{\"code\" : 7777, \"message\" : \"유효하지 않은 입력 파라미터입니다.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "🚨 작업 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "작업 조회 실패",
                                            value = "{\"code\" : 5202, \"message\" : \"업데이트 작업을 찾을 수 없어요.\"}"
                                    )
                            },
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/jobs/{jobId}/details")
    ResponseEntity<PageResponse<SyncChangeResponse>> getJobDetails(
            @PathVariable("jobId") final long jobId,

            @EnumValidation(target = SyncChangeType.class)
            @RequestParam("changeType") final String changeType,

            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            @RequestParam(value = "page", defaultValue = "1") final int page
    );
}
