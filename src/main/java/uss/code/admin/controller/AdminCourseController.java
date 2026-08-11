package uss.code.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uss.code.admin.dto.response.CourseSummaryResponse;
import uss.code.admin.service.AdminCourseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseController implements AdminCourseControllerDocs {

    private final AdminCourseService adminCourseService;

    @GetMapping("/summary")
    public ResponseEntity<CourseSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminCourseService.getSummary());
    }
}
