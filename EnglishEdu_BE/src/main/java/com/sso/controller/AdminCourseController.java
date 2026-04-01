package com.sso.controller;

import com.sso.dto.request.CreateCourseRequest;
import com.sso.dto.request.UpdateCourseRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.CourseResponse;
import com.sso.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCoursesAdmin(keyword, published, category, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(courseService.createCourse(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Long id,
            @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.updateCourse(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.ok("Course deleted successfully"));
    }

    /**
     * Back-fill: creates CourseTeacher records for all courses that have a legacy
     * teacher_id set but no corresponding row in course_teachers.
     * Call once after deploying this fix to repair existing data.
     */
    @PostMapping("/backfill-course-teachers")
    public ResponseEntity<ApiResponse<String>> backfillCourseTeachers() {
        int count = courseService.backfillCourseTeachers();
        return ResponseEntity.ok(ApiResponse.ok("Backfilled " + count + " course-teacher records."));
    }
}
