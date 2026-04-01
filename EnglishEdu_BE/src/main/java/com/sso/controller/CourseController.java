package com.sso.controller;

import com.sso.dto.request.UpdateEnrollmentRequest;
import com.sso.dto.response.*;
import com.sso.service.CourseAssignmentService;
import com.sso.service.CourseService;
import com.sso.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final CourseAssignmentService courseAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getCourses(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourses(category, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourse(id)));
    }

    @GetMapping("/enrolled")
    public ResponseEntity<ApiResponse<List<EnrolledCourseResponse>>> getEnrolledCourses(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getEnrolledCourses(getUserId(user))));
    }

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAssignedCourses(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(courseAssignmentService.getAssignedCourses(getUserId(user))));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<EnrolledCourseResponse>>> getRecentCourses(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getRecentCourses(getUserId(user))));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getDashboardStats(getUserId(user))));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<ApiResponse<EnrolledCourseResponse>> enroll(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.enroll(getUserId(user), id)));
    }

    @PatchMapping("/{id}/enrollment")
    public ResponseEntity<ApiResponse<EnrolledCourseResponse>> updateEnrollment(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnrollmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.updateEnrollment(getUserId(user), id, request)));
    }

    private Long getUserId(UserDetails user) {
        return Long.parseLong(user.getUsername());
    }
}
