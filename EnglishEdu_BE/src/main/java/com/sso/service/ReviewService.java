package com.sso.service;

import com.sso.dto.request.CreateReviewRequest;
import com.sso.dto.response.CourseReviewResponse;
import com.sso.entity.Course;
import com.sso.entity.CourseReview;
import com.sso.entity.Enrollment;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.repository.CourseRepository;
import com.sso.repository.CourseReviewRepository;
import com.sso.repository.EnrollmentRepository;
import com.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<CourseReviewResponse> getReviews(Long courseId, int page, int size) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    public CourseReviewResponse createReview(Long userId, Long courseId, CreateReviewRequest req) {
        // Must be enrolled and have some progress
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new BadRequestException("You must be enrolled to review this course"));
        if (enrollment.getProgress() < 30) {
            throw new BadRequestException("You need at least 30% progress to review this course");
        }

        if (reviewRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw new BadRequestException("You have already reviewed this course");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        CourseReview review = CourseReview.builder()
                .course(course)
                .user(user)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        review = reviewRepository.save(review);

        // Update cached avg rating
        updateCourseRating(course);

        return toResponse(review);
    }

    @Transactional
    public CourseReviewResponse updateReview(Long userId, Long courseId, CreateReviewRequest req) {
        CourseReview review = reviewRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (req.getRating() != null) review.setRating(req.getRating());
        if (req.getComment() != null) review.setComment(req.getComment());
        review = reviewRepository.save(review);

        updateCourseRating(review.getCourse());
        return toResponse(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long courseId) {
        CourseReview review = reviewRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        Course course = review.getCourse();
        reviewRepository.delete(review);
        updateCourseRating(course);
    }

    private void updateCourseRating(Course course) {
        double avg = reviewRepository.avgRatingByCourseId(course.getId());
        long count = reviewRepository.countByCourseId(course.getId());
        course.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        course.setReviewCount((int) count);
        courseRepository.save(course);
    }

    private CourseReviewResponse toResponse(CourseReview r) {
        User u = r.getUser();
        return CourseReviewResponse.builder()
                .id(r.getId())
                .userId(u.getId())
                .username(u.getUsername())
                .userFullName(u.getFullName())
                .userAvatarUrl(u.getAvatarUrl())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
