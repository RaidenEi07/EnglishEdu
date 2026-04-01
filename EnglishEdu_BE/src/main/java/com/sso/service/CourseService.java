package com.sso.service;

import com.sso.dto.request.CreateCourseRequest;
import com.sso.dto.request.UpdateCourseRequest;
import com.sso.dto.response.CourseResponse;
import com.sso.entity.Course;
import com.sso.entity.CourseTeacher;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.mapper.CourseMapper;
import com.sso.repository.CategoryRepository;
import com.sso.repository.CourseRepository;
import com.sso.repository.CourseTeacherRepository;
import com.sso.repository.EnrollmentRepository;
import com.sso.repository.LevelRepository;
import com.sso.repository.UserRepository;
import com.sso.moodle.MoodleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CategoryRepository categoryRepository;
    private final LevelRepository levelRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final MoodleSyncService moodleSyncService;

    @Transactional(readOnly = true)
    public Page<CourseResponse> getCourses(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Course> courses;
        if (category != null && !category.isBlank()) {
            courses = courseRepository.findByCategoryIgnoreCaseAndPublishedTrue(category, pageable);
        } else {
            courses = courseRepository.findByPublishedTrue(pageable);
        }

        return courses.map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        // Lazy-sync course image/info from Moodle (non-blocking: failures are ignored)
        if (course.getMoodleCourseId() != null) {
            try { moodleSyncService.syncCourseFromMoodle(course); } catch (Exception e) {
                log.debug("Moodle course sync skipped for id={}: {}", id, e.getMessage());
            }
        }
        return courseMapper.toResponse(course);
    }

    /* ── Admin ───────────────────────────────────────── */

    @Transactional(readOnly = true)
    public Page<CourseResponse> getCoursesAdmin(String keyword, Boolean published, String category, int page, int size) {
        String kw  = (keyword  != null && !keyword.isBlank())  ? keyword.toLowerCase()  : null;
        String cat = (category != null && !category.isBlank()) ? category.toLowerCase() : null;
        return courseRepository.findAdminFiltered(kw, published, cat, PageRequest.of(page, size))
                .map(courseMapper::toResponse);
    }

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest req) {
        Course course = Course.builder()
                .name(req.getName())
                .category(req.getCategory())
                .level(req.getLevel())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .externalId(req.getExternalId())
                .published(req.isPublished())
                .free(req.isFree())
                .price(req.getPrice())
                .build();
        if (req.getTeacherId() != null) {
            userRepository.findById(req.getTeacherId()).ifPresent(course::setTeacher);
        }
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(course::setCategoryEntity);
        }
        if (req.getLevelId() != null) {
            levelRepository.findById(req.getLevelId()).ifPresent(course::setLevelEntity);
        }
        Course saved = courseRepository.save(course);
        if (saved.getTeacher() != null) {
            syncLegacyTeacher(saved, saved.getTeacher());
        }
        try { moodleSyncService.ensureMoodleCourse(saved); } catch (Exception e) { log.warn("Moodle course sync failed: {}", e.getMessage()); }
        return courseMapper.toResponse(saved);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, UpdateCourseRequest req) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (req.getName() != null)        course.setName(req.getName());
        if (req.getCategory() != null)    course.setCategory(req.getCategory());
        if (req.getLevel() != null)       course.setLevel(req.getLevel());
        if (req.getDescription() != null) course.setDescription(req.getDescription());
        if (req.getImageUrl() != null)    course.setImageUrl(req.getImageUrl());
        if (req.getPublished() != null)   course.setPublished(req.getPublished());
        if (req.getFree() != null)        course.setFree(req.getFree());
        if (req.getPrice() != null)       course.setPrice(req.getPrice());
        if (req.getTeacherId() != null) {
            if (req.getTeacherId() == 0L) {
                course.setTeacher(null);
                // Remove all legacy-teacher CourseTeacher records (those added via this mechanism)
                courseTeacherRepository.deleteByCourseId(course.getId());
            } else {
                userRepository.findById(req.getTeacherId()).ifPresent(newTeacher -> {
                    course.setTeacher(newTeacher);
                    syncLegacyTeacher(course, newTeacher);
                });
            }
        }
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(course::setCategoryEntity);
        }
        if (req.getLevelId() != null) {
            levelRepository.findById(req.getLevelId()).ifPresent(course::setLevelEntity);
        }
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found");
        }
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new BadRequestException("Cannot delete a course that has active enrollments");
        }
        courseRepository.deleteById(id);
    }

    /* ── Teacher ─────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public Page<CourseResponse> getTeacherCourses(Long teacherId, int page, int size) {
        return courseRepository.findByTeacherIdViaCourseTeachers(teacherId, PageRequest.of(page, size))
                .map(courseMapper::toResponse);
    }

    /**
     * Ensures a CourseTeacher record exists for the given teacher on this course
     * (synchronising the legacy courses.teacher_id assignment with the course_teachers table)
     * and enrolls the teacher in Moodle.
     */
    private void syncLegacyTeacher(Course course, com.sso.entity.User teacher) {
        if (!courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacher.getId())) {
            CourseTeacher ct = CourseTeacher.builder()
                    .course(course)
                    .teacher(teacher)
                    .role("PRIMARY")
                    .build();
            courseTeacherRepository.save(ct);
            log.info("Created CourseTeacher record: teacher={} course={}", teacher.getUsername(), course.getId());
        }
        try {
            moodleSyncService.syncTeacherEnrolment(teacher, course);
        } catch (Exception e) {
            log.warn("Moodle teacher enrolment sync failed for teacher={} course={}: {}", teacher.getUsername(), course.getId(), e.getMessage());
        }
    }

    /**
     * Back-fills CourseTeacher records for all courses with a legacy teacher_id
     * that don't already have a corresponding course_teachers row.
     * @return number of records created
     */
    @Transactional
    public int backfillCourseTeachers() {
        int count = 0;
        for (Course course : courseRepository.findAll()) {
            if (course.getTeacher() != null) {
                syncLegacyTeacher(course, course.getTeacher());
                count++;
            }
        }
        log.info("backfillCourseTeachers: processed {} courses", count);
        return count;
    }
}
