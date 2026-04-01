package com.sso.repository;

import com.sso.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Page<Enrollment> findByStatus(String status, Pageable pageable);

    boolean existsByCourseId(Long courseId);

    List<Enrollment> findByUserIdOrderByLastAccessedDesc(Long userId);

    @Query("SELECT e FROM Enrollment e WHERE e.user.id = :userId AND e.status = :status ORDER BY e.lastAccessed DESC")
    List<Enrollment> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT e FROM Enrollment e WHERE e.user.id = :userId ORDER BY e.lastAccessed DESC NULLS LAST")
    List<Enrollment> findRecentByUserId(Long userId);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.user.id = :userId AND e.progress = 100")
    long countCompletedByUserId(Long userId);

    /* ── Teacher-scoped ────────────────────────────────── */

    Page<Enrollment> findByCourseTeacherId(Long teacherId, Pageable pageable);

    Page<Enrollment> findByCourseTeacherIdAndStatus(Long teacherId, String status, Pageable pageable);

    Page<Enrollment> findByCourseIdAndCourseTeacherId(Long courseId, Long teacherId, Pageable pageable);

    Page<Enrollment> findByCourseIdAndCourseTeacherIdAndStatus(Long courseId, Long teacherId, String status, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.status = :status")
    long countByTeacherIdAndStatus(Long teacherId, String status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.teacher.id = :teacherId")
    long countByTeacherId(Long teacherId);

    /* ── Teacher-scoped via course_teachers (many-to-many) ────── */

    @Query("SELECT e FROM Enrollment e JOIN e.course.courseTeachers ct WHERE ct.teacher.id = :teacherId")
    Page<Enrollment> findByTeacherIdViaCourseTeachers(Long teacherId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN e.course.courseTeachers ct WHERE ct.teacher.id = :teacherId AND e.status = :status")
    Page<Enrollment> findByTeacherIdAndStatusViaCourseTeachers(Long teacherId, String status, Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN e.course.courseTeachers ct WHERE e.course.id = :courseId AND ct.teacher.id = :teacherId")
    Page<Enrollment> findByCourseIdAndTeacherIdViaCourseTeachers(Long courseId, Long teacherId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN e.course.courseTeachers ct WHERE e.course.id = :courseId AND ct.teacher.id = :teacherId AND e.status = :status")
    Page<Enrollment> findByCourseIdAndTeacherIdAndStatusViaCourseTeachers(Long courseId, Long teacherId, String status, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e JOIN e.course.courseTeachers ct WHERE ct.teacher.id = :teacherId AND e.status = :status")
    long countByTeacherIdAndStatusViaCourseTeachers(Long teacherId, String status);

    @Query("SELECT COUNT(e) FROM Enrollment e JOIN e.course.courseTeachers ct WHERE ct.teacher.id = :teacherId")
    long countByTeacherIdViaCourseTeachers(Long teacherId);
}
