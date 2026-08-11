package uss.code.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uss.code.admin.domain.SyncChangeType;
import uss.code.admin.dto.request.SyncJobCreateRequest;
import uss.code.admin.dto.request.SyncPreflightRequest;
import uss.code.admin.dto.response.SyncChangeResponse;
import uss.code.admin.dto.response.SyncJobCreatedResponse;
import uss.code.admin.dto.response.SyncJobDetailResponse;
import uss.code.admin.dto.response.SyncJobResponse;
import uss.code.admin.dto.response.SyncPreflightResponse;
import uss.code.admin.service.CourseSyncService;
import uss.code.auth.annotation.AdminAuth;
import uss.code.global.annotation.EnumValidation;
import uss.code.global.dto.response.PageResponse;

import static org.springframework.http.HttpStatus.ACCEPTED;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sync")
public class AdminSyncController implements AdminSyncControllerDocs {

    private static final String DEFAULT_PAGE = "1";

    private final CourseSyncService courseSyncService;

    @PostMapping("/preflight")
    public ResponseEntity<SyncPreflightResponse> preflight(@Valid @RequestBody final SyncPreflightRequest request) {
        return ResponseEntity.ok(courseSyncService.preflight(request));
    }

    @PostMapping("/jobs")
    public ResponseEntity<SyncJobCreatedResponse> createJob(
            @AdminAuth final long adminId,
            @Valid @RequestBody final SyncJobCreateRequest request
    ) {
        return ResponseEntity.status(ACCEPTED).body(courseSyncService.createJob(adminId, request));
    }

    @GetMapping("/jobs")
    public ResponseEntity<PageResponse<SyncJobResponse>> getJobs(
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            @RequestParam(value = "page", defaultValue = DEFAULT_PAGE) final int page
    ) {
        return ResponseEntity.ok(courseSyncService.getJobs(page));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<SyncJobDetailResponse> getJob(@PathVariable("jobId") final long jobId) {
        return ResponseEntity.ok(courseSyncService.getJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/details")
    public ResponseEntity<PageResponse<SyncChangeResponse>> getJobDetails(
            @PathVariable("jobId") final long jobId,

            @EnumValidation(target = SyncChangeType.class)
            @RequestParam("changeType") final String changeType,

            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            @RequestParam(value = "page", defaultValue = DEFAULT_PAGE) final int page
    ) {
        return ResponseEntity.ok(
                courseSyncService.getJobDetails(jobId, SyncChangeType.valueOf(changeType.toUpperCase()), page)
        );
    }
}
