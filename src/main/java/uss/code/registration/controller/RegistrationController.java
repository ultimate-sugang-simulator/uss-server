package uss.code.registration.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.registration.dto.response.RegistrationCoursesResponse;
import uss.code.registration.service.RegistrationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registration")
public class RegistrationController implements RegistrationControllerDocs {

    private final RegistrationService registrationService;

    @GetMapping
    public ResponseEntity<RegistrationCoursesResponse> getRegistrationCourse(@Auth final long memberId){
        return ResponseEntity.ok(registrationService.getRegistrationCourse(memberId));
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<Void> registerCourse(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    ){
        registrationService.registerCourse(memberId, courseId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteRegisteredCourse(
            @Auth final long memberId,
            @PathVariable("courseId") final long courseId
    ){
        registrationService.deleteRegisteredCourse(memberId, courseId);
        return ResponseEntity.ok().build();
    }
}
