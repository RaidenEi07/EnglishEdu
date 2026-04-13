package com.sso.service;

import com.sso.dto.request.CreateReviewRequest;
import com.sso.dto.response.CourseReviewResponse;
import com.sso.entity.*;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private CourseReviewRepository reviewRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReviewService reviewService;

    private User buildUser(Long id) {
        return User.builder().id(id).username("u" + id).email("u" + id + "@x.com")
                .password("hashed").role("STUDENT").active(true).build();
    }

    private Course buildCourse(Long id) {
        Course c = new Course();
        c.setId(id);
        c.setName("Course " + id);
        c.setAvgRating(BigDecimal.ZERO);
        return c;
    }

    private Enrollment buildEnrollment(User user, Course course, int progress) {
        Enrollment e = new Enrollment();
        e.setUser(user);
        e.setCourse(course);
        e.setStatus("active");
        e.setProgress(progress);
        return e;
    }

    // ── createReview ──────────────────────────────────────────────────────────

    @Test
    void createReview_throwsWhenNotEnrolled() {
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());

        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment("Great!");

        assertThatThrownBy(() -> reviewService.createReview(1L, 10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("enrolled");
    }

    @Test
    void createReview_throwsWhenProgressBelow30() {
        User user = buildUser(1L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(user, course, 10);
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));

        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(4);

        assertThatThrownBy(() -> reviewService.createReview(1L, 10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("30%");
    }

    @Test
    void createReview_throwsWhenAlreadyReviewed() {
        User user = buildUser(1L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(user, course, 50);
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));
        when(reviewRepository.existsByCourseIdAndUserId(10L, 1L)).thenReturn(true);

        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);

        assertThatThrownBy(() -> reviewService.createReview(1L, 10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void createReview_success_returnsResponse() {
        User user = buildUser(1L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(user, course, 50);

        when(enrollmentRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));
        when(reviewRepository.existsByCourseIdAndUserId(10L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        CourseReview saved = CourseReview.builder()
                .id(100L).course(course).user(user).rating(5).comment("Excellent!").build();
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(saved);
        when(reviewRepository.avgRatingByCourseId(10L)).thenReturn(5.0);
        when(reviewRepository.countByCourseId(10L)).thenReturn(1L);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment("Excellent!");

        CourseReviewResponse result = reviewService.createReview(1L, 10L, req);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent!");
    }

    // ── updateReview ──────────────────────────────────────────────────────────

    @Test
    void updateReview_throwsWhenNotFound() {
        when(reviewRepository.findByCourseIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(1L, 10L, new CreateReviewRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReview_updatesRatingAndComment() {
        User user = buildUser(1L);
        Course course = buildCourse(10L);
        CourseReview review = CourseReview.builder()
                .id(100L).course(course).user(user).rating(3).comment("OK").build();

        when(reviewRepository.findByCourseIdAndUserId(10L, 1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(CourseReview.class))).thenReturn(review);
        when(reviewRepository.avgRatingByCourseId(10L)).thenReturn(4.0);
        when(reviewRepository.countByCourseId(10L)).thenReturn(1L);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(4);
        req.setComment("Good!");

        CourseReviewResponse result = reviewService.updateReview(1L, 10L, req);

        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("Good!");
    }

    // ── deleteReview ──────────────────────────────────────────────────────────

    @Test
    void deleteReview_throwsWhenNotFound() {
        when(reviewRepository.findByCourseIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteReview_deletesAndUpdatesRating() {
        User user = buildUser(1L);
        Course course = buildCourse(10L);
        CourseReview review = CourseReview.builder()
                .id(100L).course(course).user(user).rating(3).comment("OK").build();

        when(reviewRepository.findByCourseIdAndUserId(10L, 1L)).thenReturn(Optional.of(review));
        when(reviewRepository.avgRatingByCourseId(10L)).thenReturn(0.0);
        when(reviewRepository.countByCourseId(10L)).thenReturn(0L);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        reviewService.deleteReview(1L, 10L);

        verify(reviewRepository).delete(review);
        verify(courseRepository).save(course);
    }
}
