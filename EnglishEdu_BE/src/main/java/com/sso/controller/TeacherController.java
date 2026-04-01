package com.sso.controller;

import com.sso.dto.request.TeacherUpdateEnrollmentRequest;
import com.sso.dto.response.*;
import com.sso.service.CourseService;
import com.sso.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    /** Summary stats for the teacher's dashboard. */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TeacherDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getTeacherDashboard(getId(user))));
    }

    /** List all courses assigned to this teacher (paginated). */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getCourses(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getTeacherCourses(getId(user), page, size)));
    }

    /**
     * List enrollments for this teacher's courses.
     * Optionally filter by courseId and/or status.
     */
    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<Page<AdminEnrollmentResponse>>> getEnrollments(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                enrollmentService.getTeacherEnrollments(getId(user), courseId, status, page, size)));
    }

    /** Update progress, status, note or expiry for a single enrollment. */
    @PatchMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<AdminEnrollmentResponse>> updateEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody TeacherUpdateEnrollmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                enrollmentService.teacherUpdateEnrollment(id, getId(user), request)));
    }

    private Long getId(UserDetails user) {
        return Long.parseLong(user.getUsername());
    }
}
