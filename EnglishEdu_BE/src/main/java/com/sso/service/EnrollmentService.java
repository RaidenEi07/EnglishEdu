package com.sso.service;

import com.sso.dto.request.TeacherUpdateEnrollmentRequest;
import com.sso.dto.request.UpdateEnrollmentRequest;
import com.sso.dto.response.AdminEnrollmentResponse;
import com.sso.dto.response.DashboardResponse;
import com.sso.dto.response.EnrolledCourseResponse;
import com.sso.dto.response.TeacherDashboardResponse;
import com.sso.entity.Course;
import com.sso.entity.Enrollment;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.mapper.CourseMapper;
import com.sso.repository.CourseAssignmentRepository;
import com.sso.repository.CourseRepository;
import com.sso.repository.EnrollmentRepository;
import com.sso.repository.UserRepository;
import com.sso.moodle.MoodleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final UserRepository userRepository;
    private final NotificationPushService notificationPushService;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final MoodleSyncService moodleSyncService;

    @Transactional
    public List<EnrolledCourseResponse> getEnrolledCourses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            // Admins can access all courses – return every course with status "active"
            return courseRepository.findAll().stream()
                    .map(course -> EnrolledCourseResponse.builder()
                            .courseId(course.getId())
                            .externalId(course.getExternalId())
                            .name(course.getName())
                            .category(course.getCategory())
                            .level(course.getLevel())
                            .imageUrl(course.getImageUrl())
                            .status("active")
                            .progress(0)
                            .build())
                    .toList();
        }
        // For students: lazily sync any Moodle enrollments that were created outside EnglishEdu
        if ("STUDENT".equalsIgnoreCase(user.getRole()) && user.getMoodleId() != null) {
            syncMoodleEnrollmentsToLocal(user);
        }
        return enrollmentRepository.findByUserIdOrderByLastAccessedDesc(userId).stream()
                .map(courseMapper::toEnrolledResponse)
                .toList();
    }

    /**
     * Queries Moodle for the student's enrolled courses and auto-creates local
     * enrollment records for any that are missing in the EnglishEdu database.
     * This handles the case where an admin manually enrolled a student directly in Moodle.
     */
    private void syncMoodleEnrollmentsToLocal(User user) {
        try {
            com.fasterxml.jackson.databind.JsonNode moodleCourses = moodleSyncService.getMoodleCourses(user);
            if (moodleCourses == null || !moodleCourses.isArray()) return;
            for (com.fasterxml.jackson.databind.JsonNode mc : moodleCourses) {
                long moodleCourseId = mc.path("id").asLong();
                if (moodleCourseId == 0) continue;
                courseRepository.findByMoodleCourseId(moodleCourseId).ifPresent(course -> {
                    if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                        Enrollment e = Enrollment.builder()
                                .user(user)
                                .course(course)
                                .status("active")
                                .requestDate(Instant.now())
                                .approvedAt(Instant.now())
                                .build();
                        enrollmentRepository.save(e);
                        log.info("Auto-synced Moodle enrollment: user={} course={}", user.getUsername(), course.getName());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Moodle enrollment sync failed for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * Admin-initiated direct enrollment: creates an active enrollment for any user
     * in any course and syncs to Moodle. Use this instead of assigning directly in Moodle UI.
     */
    @Transactional
    public EnrolledCourseResponse directEnrollByAdmin(Long adminId, Long targetUserId, Long courseId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (enrollmentRepository.existsByUserIdAndCourseId(targetUserId, courseId)) {
            Enrollment existing = enrollmentRepository.findByUserIdAndCourseId(targetUserId, courseId)
                    .orElseThrow();
            if (!"active".equals(existing.getStatus()) && !"inprogress".equals(existing.getStatus())) {
                existing.setStatus("active");
                existing.setApprovedAt(Instant.now());
                existing.setApprovedBy(userRepository.getReferenceById(adminId));
                enrollmentRepository.save(existing);
            }
            try { moodleSyncService.syncStudentEnrolment(targetUser, course); } catch (Exception e) { log.warn("Moodle sync on direct enroll: {}", e.getMessage()); }
            return courseMapper.toEnrolledResponse(existing);
        }

        Enrollment enrollment = Enrollment.builder()
                .user(targetUser)
                .course(course)
                .status("active")
                .requestDate(Instant.now())
                .approvedAt(Instant.now())
                .approvedBy(userRepository.getReferenceById(adminId))
                .build();
        enrollment = enrollmentRepository.save(enrollment);

        notificationPushService.sendNotification(targetUserId,
                "Bạn đã được ghi danh vào khóa học: " + course.getName(),
                null, "ENROLLMENT");

        try { moodleSyncService.syncStudentEnrolment(targetUser, course); } catch (Exception e) { log.warn("Moodle sync on direct enroll: {}", e.getMessage()); }

        return courseMapper.toEnrolledResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrolledCourseResponse> getRecentCourses(Long userId) {
        return enrollmentRepository.findRecentByUserId(userId).stream()
                .limit(5)
                .map(courseMapper::toEnrolledResponse)
                .toList();
    }

    public DashboardResponse getDashboardStats(Long userId) {
        long total      = enrollmentRepository.countByUserId(userId);
        long pending    = enrollmentRepository.countByUserIdAndStatus(userId, "pending");
        long inProgress = enrollmentRepository.countByUserIdAndStatus(userId, "inprogress");
        long completed  = enrollmentRepository.countCompletedByUserId(userId);

        return DashboardResponse.builder()
                .totalEnrolled(total)
                .pendingCount(pending)
                .inProgress(inProgress)
                .completed(completed)
                .build();
    }

    @Transactional
    public EnrolledCourseResponse enroll(Long userId, Long courseId) {
        User enrollingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String role = enrollingUser.getRole();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (enrollingUser.isGuest() || (!("STUDENT".equalsIgnoreCase(role) || isAdmin))) {
            throw new BadRequestException("Chỉ tài khoản học sinh hoặc quản trị viên mới có thể đăng ký khóa học.");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BadRequestException("Already enrolled in this course");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Check course assignment – students can only enroll in assigned courses (admins bypass)
        if (!isAdmin && !courseAssignmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new BadRequestException("Bạn chưa được chỉ định khóa học này. Vui lòng liên hệ quản trị viên.");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(enrollingUser)
                .course(course)
                .requestDate(Instant.now())
                .build();

        // Auto-activate enrollment for free courses
        if (course.isFree()) {
            enrollment.setStatus("active");
            enrollment.setApprovedAt(Instant.now());
        }

        enrollment = enrollmentRepository.save(enrollment);

        if (course.isFree()) {
            notificationPushService.sendNotification(userId,
                    "Bạn đã được tự động kích hoạt khóa học: " + course.getName(),
                    null,
                    "ENROLLMENT");
            // Auto-sync to Moodle for free (auto-activated) courses
            try { moodleSyncService.syncStudentEnrolment(enrollingUser, course); } catch (Exception e) { log.warn("Moodle enrolment sync failed: {}", e.getMessage()); }
        }

        return courseMapper.toEnrolledResponse(enrollment);
    }

    @Transactional
    public EnrolledCourseResponse updateEnrollment(Long userId, Long courseId, UpdateEnrollmentRequest request) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (request.getProgress() != null) {
            enrollment.setProgress(request.getProgress());
            // Auto-transition status based on progress
            String s = enrollment.getStatus();
            if (("active".equals(s) || "inprogress".equals(s)) && request.getProgress() == 100) {
                enrollment.setStatus("completed");
            } else if ("active".equals(s) && request.getProgress() > 0) {
                enrollment.setStatus("inprogress");
            }
        }
        enrollment.setLastAccessed(Instant.now());

        return courseMapper.toEnrolledResponse(enrollmentRepository.save(enrollment));
    }

    /* â”€â”€ Admin â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

    @Transactional(readOnly = true)
    public Page<AdminEnrollmentResponse> getEnrollmentsAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Enrollment> enrollments = (status != null && !status.isBlank())
                ? enrollmentRepository.findByStatus(status.toLowerCase(), pageable)
                : enrollmentRepository.findAll(pageable);
        return enrollments.map(this::toAdminResponse);
    }

    @Transactional
    public AdminEnrollmentResponse approveEnrollment(Long enrollmentId, Long adminId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollment.setStatus("active");
        enrollment.setApprovedAt(Instant.now());
        enrollment.setApprovedBy(userRepository.getReferenceById(adminId));
        Enrollment saved = enrollmentRepository.save(enrollment);

        notificationPushService.sendNotification(enrollment.getUser().getId(),
                "Khóa học \"" + enrollment.getCourse().getName() + "\" đã được duyệt. Bạn có thể bắt đầu học ngay!",
                null,
                "ENROLLMENT");

        // Sync to Moodle on approval
        try { moodleSyncService.syncStudentEnrolment(enrollment.getUser(), enrollment.getCourse()); } catch (Exception e) { log.warn("Moodle enrolment sync on approve failed: {}", e.getMessage()); }

        return toAdminResponse(saved);
    }

    @Transactional
    public AdminEnrollmentResponse revokeEnrollment(Long enrollmentId, String note) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollment.setStatus("revoked");
        if (note != null && !note.isBlank()) enrollment.setTeacherNote(note);
        Enrollment saved = enrollmentRepository.save(enrollment);

        notificationPushService.sendNotification(enrollment.getUser().getId(),
                "Quyền truy cập khóa học \"" + enrollment.getCourse().getName() + "\" đã bị thu hồi.",
                null,
                "ENROLLMENT");

        return toAdminResponse(saved);
    }

    private AdminEnrollmentResponse toAdminResponse(Enrollment e) {
        User student    = e.getUser();
        Course course   = e.getCourse();
        User approvedBy = e.getApprovedBy();
        return AdminEnrollmentResponse.builder()
                .id(e.getId())
                .courseId(course   != null ? course.getId()              : null)
                .courseName(course != null ? course.getName()            : null)
                .studentId(student != null ? student.getId()             : null)
                .studentUsername(student  != null ? student.getUsername()  : null)
                .studentFirstName(student != null ? student.getFirstName() : null)
                .studentLastName(student  != null ? student.getLastName()  : null)
                .studentFullName(student  != null ? student.getFullName()  : null)
                .status(e.getStatus())
                .progress(e.getProgress())
                .requestDate(e.getRequestDate())
                .enrolledAt(e.getEnrolledAt())
                .approvedAt(e.getApprovedAt())
                .approvedByUsername(approvedBy != null ? approvedBy.getUsername() : null)
                .teacherNote(e.getTeacherNote())
                .expiryDate(e.getExpiryDate())
                .build();
    }

    /* â”€â”€ Teacher â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(Long teacherId) {
        long courses  = courseRepository.countByTeacherIdViaCourseTeachers(teacherId);
        long students = enrollmentRepository.countByTeacherIdViaCourseTeachers(teacherId);
        long pending  = enrollmentRepository.countByTeacherIdAndStatusViaCourseTeachers(teacherId, "pending");
        long active   = enrollmentRepository.countByTeacherIdAndStatusViaCourseTeachers(teacherId, "active");
        return TeacherDashboardResponse.builder()
                .totalCourses(courses)
                .totalStudents(students)
                .pendingStudents(pending)
                .activeStudents(active)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminEnrollmentResponse> getTeacherEnrollments(Long teacherId, Long courseId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        final String normalizedStatus = (status != null && !status.isBlank()) ? status.toLowerCase() : null;
        Page<Enrollment> enrollments;
        if (courseId != null && normalizedStatus != null) {
            enrollments = enrollmentRepository.findByCourseIdAndTeacherIdAndStatusViaCourseTeachers(courseId, teacherId, normalizedStatus, pageable);
        } else if (courseId != null) {
            enrollments = enrollmentRepository.findByCourseIdAndTeacherIdViaCourseTeachers(courseId, teacherId, pageable);
        } else if (normalizedStatus != null) {
            enrollments = enrollmentRepository.findByTeacherIdAndStatusViaCourseTeachers(teacherId, normalizedStatus, pageable);
        } else {
            enrollments = enrollmentRepository.findByTeacherIdViaCourseTeachers(teacherId, pageable);
        }
        return enrollments.map(this::toAdminResponse);
    }

    @Transactional
    public AdminEnrollmentResponse teacherUpdateEnrollment(Long enrollmentId, Long teacherId, TeacherUpdateEnrollmentRequest req) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        // verify the enrollment belongs to one of this teacher's courses via course_teachers
        Course course = enrollment.getCourse();
        boolean isTeacher = course != null && course.getCourseTeachers() != null &&
                course.getCourseTeachers().stream().anyMatch(ct -> ct.getTeacher().getId().equals(teacherId));
        if (!isTeacher) {
            throw new BadRequestException("You are not a teacher of this course");
        }
        if (req.getProgress() != null)   enrollment.setProgress(req.getProgress());
        if (req.getStatus() != null)     enrollment.setStatus(req.getStatus());
        if (req.getTeacherNote() != null) enrollment.setTeacherNote(req.getTeacherNote());
        if (req.getExpiryDate() != null) enrollment.setExpiryDate(req.getExpiryDate());
        return toAdminResponse(enrollmentRepository.save(enrollment));
    }
}
