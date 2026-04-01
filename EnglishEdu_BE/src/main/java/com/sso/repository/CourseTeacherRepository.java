package com.sso.repository;

import com.sso.entity.CourseTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {

    List<CourseTeacher> findByCourseId(Long courseId);

    List<CourseTeacher> findByTeacherId(Long teacherId);

    Optional<CourseTeacher> findByCourseIdAndTeacherId(Long courseId, Long teacherId);

    boolean existsByCourseIdAndTeacherId(Long courseId, Long teacherId);

    void deleteByCourseIdAndTeacherId(Long courseId, Long teacherId);

    void deleteByCourseId(Long courseId);

    @Query("SELECT ct.course.id FROM CourseTeacher ct WHERE ct.teacher.id = :teacherId")
    List<Long> findCourseIdsByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId);
}
