package uss.code.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.dto.request.LoginRequest;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.auth.service.AuthService;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid@RequestBody final LoginRequest request){
        return ResponseEntity.status(OK).body(authService.login(request));
    }
}
