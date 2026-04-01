package com.sso.repository;

import com.sso.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Page<CourseReview> findByCourseIdOrderByCreatedAtDesc(Long courseId, Pageable pageable);

    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM CourseReview r WHERE r.course.id = :courseId")
    double avgRatingByCourseId(Long courseId);

    long countByCourseId(Long courseId);
}
