package uss.code.course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.service.CourseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/major")
    public ResponseEntity<MajorCoursesResponse> getMajorCourses(@Auth final long memberId){
        return ResponseEntity.ok(courseService.getMajorCourses(memberId));
    }

    @GetMapping("/general-education")
    public ResponseEntity<GeneralEducationCoursesResponse> getGeneralEducationCourses(@RequestParam("course-area") final String courseArea){
        return ResponseEntity.ok(courseService.getGeneralEducationCourses(courseArea));
    }
}
