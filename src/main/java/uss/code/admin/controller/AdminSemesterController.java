package uss.code.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.admin.dto.request.SystemSemesterRequest;
import uss.code.admin.dto.response.SystemSemesterResponse;
import uss.code.admin.service.SystemSemesterService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/semesters")
public class AdminSemesterController implements AdminSemesterControllerDocs {

    private final SystemSemesterService systemSemesterService;

    @GetMapping
    public ResponseEntity<SystemSemesterResponse> getSystemSemester() {
        return ResponseEntity.ok(systemSemesterService.getSystemSemester());
    }

    @PutMapping
    public ResponseEntity<SystemSemesterResponse> changeSystemSemester(
            @Valid @RequestBody final SystemSemesterRequest request
    ) {
        return ResponseEntity.ok(systemSemesterService.changeSystemSemester(request));
    }
}
