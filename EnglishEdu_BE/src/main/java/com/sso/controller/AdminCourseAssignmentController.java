package com.sso.controller;

import com.sso.dto.request.AssignCoursesRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.CourseAssignmentResponse;
import com.sso.service.CourseAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/course-assignments")
@RequiredArgsConstructor
public class AdminCourseAssignmentController {

    private final CourseAssignmentService courseAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseAssignmentResponse>>> getAssignments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long courseId) {
        List<CourseAssignmentResponse> result;
        if (userId != null) {
            result = courseAssignmentService.getAssignmentsByUser(userId);
        } else if (courseId != null) {
            result = courseAssignmentService.getAssignmentsByCourse(courseId);
        } else {
            result = List.of();
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<CourseAssignmentResponse>>> assignCourses(
            @Valid @RequestBody AssignCoursesRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Long adminId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(courseAssignmentService.assignCourses(request, adminId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unassignCourse(
            @RequestParam Long userId,
            @RequestParam Long courseId) {
        courseAssignmentService.unassignCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
