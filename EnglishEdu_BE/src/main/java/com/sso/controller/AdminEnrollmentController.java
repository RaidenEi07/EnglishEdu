package com.sso.controller;

import com.sso.dto.response.AdminEnrollmentResponse;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.EnrolledCourseResponse;
import com.sso.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/enrollments")
@RequiredArgsConstructor
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminEnrollmentResponse>>> getEnrollments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.getEnrollmentsAdmin(status, page, size)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AdminEnrollmentResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        Long adminId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.approveEnrollment(id, adminId)));
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<AdminEnrollmentResponse>> revoke(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.revokeEnrollment(id, note)));
    }

    /**
     * Directly enrolls a user into a course with "active" status and syncs to Moodle.
     * Use this instead of assigning students directly in the Moodle UI.
     */
    @PostMapping("/direct-enroll")
    public ResponseEntity<ApiResponse<EnrolledCourseResponse>> directEnroll(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @AuthenticationPrincipal UserDetails admin) {
        Long adminId = Long.parseLong(admin.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.directEnrollByAdmin(adminId, userId, courseId)));
    }
}
