package uss.code.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.request.SignUpRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.dto.response.EmailAvailabilityResponse;
import uss.code.auth.service.AuthService;
import uss.code.global.annotation.ParamValidation;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private static final String ACCESS_TOKEN_HEADER = "access-token";

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<AuthTokenResponse> signUp(@Valid @RequestBody final SignUpRequest request){
        return ResponseEntity.status(CREATED).body(authService.signUp(request));
    }

    @GetMapping("/email-availability")
    public ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(
            @ParamValidation(maxLength = 255)
            @RequestParam("email") final String email
    ){
        return ResponseEntity.ok(authService.checkEmailAvailability(email));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody final LoginRequest request){
        return ResponseEntity.status(OK).body(authService.login(request));
    }

    @PostMapping("/re-issue")
    public ResponseEntity<AuthTokenResponse> reIssue(
            @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) final String accessToken
    ){
        return ResponseEntity.status(OK).body(authService.reIssue(accessToken));
    }
}
