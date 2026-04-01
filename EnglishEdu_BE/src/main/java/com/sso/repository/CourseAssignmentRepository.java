package com.sso.repository;

import com.sso.entity.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<CourseAssignment> findByUserId(Long userId);

    List<CourseAssignment> findByCourseId(Long courseId);

    @Modifying
    void deleteByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT ca.course.id FROM CourseAssignment ca WHERE ca.user.id = :userId")
    List<Long> findCourseIdsByUserId(Long userId);

    long countByUserId(Long userId);

    long countByCourseId(Long courseId);
}
