package uss.code.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.dto.request.VerificationCodeSendRequest;
import uss.code.auth.service.EmailVerificationCodeService;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email-verification-codes")
public class EmailVerificationCodeController {

    private final EmailVerificationCodeService emailVerificationCodeService;

    @PostMapping
    public ResponseEntity<Void> sendVerificationCode(@Valid@RequestBody final VerificationCodeSendRequest request){
        emailVerificationCodeService.sendVerificationCode(request);
        return ResponseEntity.status(CREATED).build();
    }

//    @PatchMapping
//    public ResponseEntity<Void> verifyCode(@Valid@RequestBody final VerificationCodeVerifyRequest request) {
//        emailVerificationCodeService.verifyCode(request);
//        return ResponseEntity.status(OK).build();
//    }
}
