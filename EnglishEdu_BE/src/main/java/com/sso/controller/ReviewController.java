package com.sso.controller;

import com.sso.dto.request.CreateReviewRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.CourseReviewResponse;
import com.sso.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseReviewResponse>>> getReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviews(courseId, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseReviewResponse>> createReview(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(reviewService.createReview(userId, courseId, request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        reviewService.deleteReview(userId, courseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
