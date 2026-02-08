package uss.code.registration.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.registration.dto.response.RegistrationCoursesResponse;
import uss.code.registration.service.RegistrationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    @GetMapping
    public ResponseEntity<RegistrationCoursesResponse> getRegistrationCourse(@Auth final long memberId){
        return ResponseEntity.ok(registrationService.getRegistrationCourse(memberId));
    }
}
