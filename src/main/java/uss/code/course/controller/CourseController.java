package uss.code.course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uss.code.auth.annotation.Auth;
import uss.code.course.dto.response.CourseCategoriesResponse;
import uss.code.course.dto.response.CourseTermsResponse;
import uss.code.course.dto.response.GeneralEducationCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorCoursesResponse;
import uss.code.course.dto.response.InterdisciplinaryMajorsResponse;
import uss.code.course.dto.response.MajorCoursesResponse;
import uss.code.course.dto.response.SearchedCoursesResponse;
import uss.code.course.service.CourseService;
import uss.code.global.annotation.ParamValidation;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController implements CourseControllerDocs {

    private final CourseService courseService;

    @GetMapping("/major")
    public ResponseEntity<MajorCoursesResponse> getMajorCourses(@Auth final long memberId){
        return ResponseEntity.ok(courseService.getMajorCourses(memberId));
    }

    @GetMapping("/general-education")
    public ResponseEntity<GeneralEducationCoursesResponse> getGeneralEducationCourses(
            @ParamValidation(maxLength = 30)
            @RequestParam("course-area") final String courseArea
    ){
        return ResponseEntity.ok(courseService.getGeneralEducationCourses(courseArea));
    }

    @GetMapping("/other-department")
    public ResponseEntity<MajorCoursesResponse> getOtherDepartmentCourses(
            @ParamValidation(maxLength = 40)
            @RequestParam("department") final String department
    ){
        return ResponseEntity.ok(courseService.getOtherDepartmentCourses(department));
    }

    @GetMapping("/interdisciplinary-major")
    public ResponseEntity<InterdisciplinaryMajorCoursesResponse> getInterdisciplinaryMajorCourses(
            @ParamValidation(maxLength = 40)
            @RequestParam("department") final String department
    ){
        return ResponseEntity.ok(courseService.getInterdisciplinaryMajorCourses(department));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchedCoursesResponse> searchCourses(
            @ParamValidation(maxLength = 70)
            @RequestParam("keyword") final String keyword
    ){
        return ResponseEntity.ok(courseService.searchCourses(keyword.trim()));
    }

    @GetMapping("/huss")
    public ResponseEntity<MajorCoursesResponse> getHussCourses(){
        return ResponseEntity.ok(courseService.getHussCourses());
    }

    @GetMapping("/categories")
    public ResponseEntity<CourseCategoriesResponse> getCategories(){
        return ResponseEntity.ok(courseService.getCategories());
    }

    @GetMapping("/terms")
    public ResponseEntity<CourseTermsResponse> getTerms(){
        return ResponseEntity.ok(courseService.getTerms());
    }

    @GetMapping("/interdisciplinary-majors")
    public ResponseEntity<InterdisciplinaryMajorsResponse> getInterdisciplinaryMajors(){
        return ResponseEntity.ok(courseService.getInterdisciplinaryMajors());
    }
}
