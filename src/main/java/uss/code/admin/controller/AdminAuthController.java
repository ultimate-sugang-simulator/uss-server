package uss.code.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.admin.dto.request.AdminLoginRequest;
import uss.code.admin.dto.response.AdminTokenResponse;
import uss.code.admin.service.AdminAuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController implements AdminAuthControllerDocs {

    private static final String ACCESS_TOKEN_HEADER = "access-token";

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AdminTokenResponse> login(@Valid @RequestBody final AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AdminTokenResponse> refresh(
            @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) final String accessToken
    ) {
        return ResponseEntity.ok(adminAuthService.reIssue(accessToken));
    }
}
