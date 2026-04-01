package com.sso.repository;

import com.sso.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByPublishedTrue(Pageable pageable);

    Page<Course> findByCategoryIgnoreCaseAndPublishedTrue(String category, Pageable pageable);

    Page<Course> findByCategoryEntityIdAndPublishedTrue(Long categoryId, Pageable pageable);

    Optional<Course> findByExternalId(Integer externalId);

    Page<Course> findByTeacherId(Long teacherId, Pageable pageable);

    long countByTeacherId(Long teacherId);

    @Query("SELECT c FROM Course c JOIN c.courseTeachers ct WHERE ct.teacher.id = :teacherId")
    Page<Course> findByTeacherIdViaCourseTeachers(Long teacherId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Course c JOIN c.courseTeachers ct WHERE ct.teacher.id = :teacherId")
    long countByTeacherIdViaCourseTeachers(Long teacherId);

    @Query("SELECT c.id FROM Course c JOIN c.courseTeachers ct WHERE ct.teacher.id = :teacherId")
    List<Long> findCourseIdsByTeacherId(Long teacherId);

    /** Admin filtered search — all parameters optional.
     *  JOIN FETCH scalar associations to avoid N+1 queries.
     *  Separate countQuery avoids Cartesian-product count issues. */
    Optional<Course> findByMoodleCourseId(Long moodleCourseId);

    @Query(
        value = """
            SELECT c FROM Course c
            LEFT JOIN FETCH c.teacher
            LEFT JOIN FETCH c.categoryEntity
            LEFT JOIN FETCH c.levelEntity
            WHERE (:keyword IS NULL OR LOWER(c.name) LIKE %:keyword% OR LOWER(c.category) LIKE %:keyword%)
              AND (:published IS NULL OR c.published = :published)
              AND (:category IS NULL OR LOWER(c.category) LIKE %:category%)
            """,
        countQuery = """
            SELECT COUNT(c) FROM Course c
            WHERE (:keyword IS NULL OR LOWER(c.name) LIKE %:keyword% OR LOWER(c.category) LIKE %:keyword%)
              AND (:published IS NULL OR c.published = :published)
              AND (:category IS NULL OR LOWER(c.category) LIKE %:category%)
            """)
    Page<Course> findAdminFiltered(
            @Param("keyword")   String keyword,
            @Param("published") Boolean published,
            @Param("category")  String category,
            Pageable pageable);
}
