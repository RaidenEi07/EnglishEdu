package com.sso.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sso.dto.response.EnrolledCourseResponse;
import com.sso.entity.Course;
import com.sso.entity.Enrollment;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.mapper.CourseMapper;
import com.sso.moodle.MoodleApiException;
import com.sso.moodle.MoodleSyncService;
import com.sso.repository.CourseAssignmentRepository;
import com.sso.repository.CourseRepository;
import com.sso.repository.EnrollmentRepository;
import com.sso.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseMapper courseMapper;
    @Mock private UserRepository userRepository;
    @Mock private NotificationPushService notificationPushService;
    @Mock private CourseAssignmentRepository courseAssignmentRepository;
    @Mock private MoodleSyncService moodleSyncService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /* ─── Helper builders ─────────────────────────────────── */

    private User buildUser(Long id, String role) {
        return User.builder()
                .id(id).username("user" + id).email("user" + id + "@test.com")
                .password("hashed").firstName("User").lastName(String.valueOf(id))
                .role(role).active(true).build();
    }

    private User buildStudent(Long id) { return buildUser(id, "STUDENT"); }
    private User buildAdmin(Long id)   { return buildUser(id, "ADMIN"); }

    private Course buildCourse(Long id) {
        return Course.builder()
                .id(id).name("Course " + id).category("IELTS").level("B1")
                .published(true).free(true).build();
    }

    private Enrollment buildEnrollment(Long id, User user, Course course) {
        return Enrollment.builder()
                .id(id).user(user).course(course).status("active").progress(0)
                .enrolledAt(Instant.now()).build();
    }

    private EnrolledCourseResponse buildEnrolledResponse(Enrollment e) {
        return EnrolledCourseResponse.builder()
                .enrollmentId(e.getId())
                .courseId(e.getCourse().getId())
                .name(e.getCourse().getName())
                .status(e.getStatus())
                .progress(e.getProgress())
                .build();
    }

    /* ─── getEnrolledCourses ──────────────────────────────── */

    @Test
    void getEnrolledCourses_student_noMoodleId_noEnrollments_returnsEmpty() {
        User student = buildStudent(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        doThrow(new MoodleApiException("Moodle unreachable"))
                .when(moodleSyncService).ensureMoodleUser(any());
        when(enrollmentRepository.findByUserIdOrderByLastAccessedDesc(1L))
                .thenReturn(Collections.emptyList());

        List<EnrolledCourseResponse> result = enrollmentService.getEnrolledCourses(1L);

        assertThat(result).isEmpty();
        verify(moodleSyncService).ensureMoodleUser(student);
    }

    @Test
    void getEnrolledCourses_student_withMoodleId_returnsEnrollments() {
        User student = buildStudent(1L);
        student.setMoodleId(100L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(1L, student, course);
        EnrolledCourseResponse expected = buildEnrolledResponse(enrollment);

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(moodleSyncService.getMoodleCourses(student)).thenReturn(objectMapper.createArrayNode());
        when(enrollmentRepository.findByUserIdOrderByLastAccessedDesc(1L))
                .thenReturn(List.of(enrollment));
        when(courseMapper.toEnrolledResponse(enrollment)).thenReturn(expected);

        List<EnrolledCourseResponse> result = enrollmentService.getEnrolledCourses(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo(10L);
    }

    @Test
    void getEnrolledCourses_student_moodleSyncFails_stillReturnsEnrollments() {
        User student = buildStudent(1L);
        student.setMoodleId(100L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(1L, student, course);
        EnrolledCourseResponse expected = buildEnrolledResponse(enrollment);

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(moodleSyncService.getMoodleCourses(student))
                .thenThrow(new MoodleApiException("Moodle connection failed"));
        when(enrollmentRepository.findByUserIdOrderByLastAccessedDesc(1L))
                .thenReturn(List.of(enrollment));
        when(courseMapper.toEnrolledResponse(enrollment)).thenReturn(expected);

        List<EnrolledCourseResponse> result = enrollmentService.getEnrolledCourses(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Course 10");
    }

    @Test
    void getEnrolledCourses_admin_returnsAllCourses() {
        User admin = buildAdmin(1L);
        Course c1 = buildCourse(1L);
        Course c2 = buildCourse(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        List<EnrolledCourseResponse> result = enrollmentService.getEnrolledCourses(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo("active");
        assertThat(result.get(1).getStatus()).isEqualTo("active");
        verifyNoInteractions(moodleSyncService);
    }

    @Test
    void getEnrolledCourses_userNotFound_throws404() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getEnrolledCourses(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getEnrolledCourses_student_ensureMoodleUserSucceeds_thenSyncs() {
        User student = buildStudent(1L);
        // moodleId is null initially
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        doAnswer(inv -> { student.setMoodleId(42L); return 42L; })
                .when(moodleSyncService).ensureMoodleUser(student);
        when(moodleSyncService.getMoodleCourses(any())).thenReturn(objectMapper.createArrayNode());
        when(enrollmentRepository.findByUserIdOrderByLastAccessedDesc(1L))
                .thenReturn(Collections.emptyList());

        List<EnrolledCourseResponse> result = enrollmentService.getEnrolledCourses(1L);

        assertThat(result).isEmpty();
        verify(moodleSyncService).ensureMoodleUser(student);
        verify(moodleSyncService).getMoodleCourses(any());
    }

    /* ─── enroll ──────────────────────────────────────────── */

    @Test
    void enroll_student_freeCourse_activatesImmediately() {
        User student = buildStudent(1L);
        Course course = buildCourse(10L);
        course.setFree(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAssignmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });
        when(courseMapper.toEnrolledResponse(any(Enrollment.class))).thenReturn(
                EnrolledCourseResponse.builder().enrollmentId(100L).status("active").build());

        EnrolledCourseResponse result = enrollmentService.enroll(1L, 10L);

        assertThat(result.getStatus()).isEqualTo("active");
        verify(notificationPushService).sendNotification(eq(1L), anyString(), any(), eq("ENROLLMENT"));
    }

    @Test
    void enroll_student_alreadyEnrolled_throws400() {
        User student = buildStudent(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Already enrolled");
    }

    @Test
    void enroll_student_notAssigned_throws400() {
        User student = buildStudent(1L);
        Course course = buildCourse(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseAssignmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chưa được chỉ định");
    }

    @Test
    void enroll_guestUser_throws400() {
        User guest = buildStudent(1L);
        guest.setGuest(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(guest));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("học sinh hoặc quản trị viên");
    }

    /* ─── updateEnrollment ────────────────────────────────── */

    @Test
    void updateEnrollment_progress100_completesEnrollment() {
        User student = buildStudent(1L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(1L, student, course);
        enrollment.setStatus("inprogress");

        var request = new com.sso.dto.request.UpdateEnrollmentRequest();
        request.setProgress(100);

        when(enrollmentRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseMapper.toEnrolledResponse(any(Enrollment.class))).thenReturn(
                EnrolledCourseResponse.builder().status("completed").progress(100).build());

        EnrolledCourseResponse result = enrollmentService.updateEnrollment(1L, 10L, request);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(enrollment.getStatus()).isEqualTo("completed");
    }

    @Test
    void updateEnrollment_progressPartial_setsInProgress() {
        User student = buildStudent(1L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(1L, student, course);
        enrollment.setStatus("active");

        var request = new com.sso.dto.request.UpdateEnrollmentRequest();
        request.setProgress(50);

        when(enrollmentRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseMapper.toEnrolledResponse(any(Enrollment.class))).thenReturn(
                EnrolledCourseResponse.builder().status("inprogress").progress(50).build());

        enrollmentService.updateEnrollment(1L, 10L, request);

        assertThat(enrollment.getStatus()).isEqualTo("inprogress");
    }

    /* ─── dashboard ───────────────────────────────────────── */

    @Test
    void getDashboardStats_returnsCorrectCounts() {
        when(enrollmentRepository.countByUserId(1L)).thenReturn(5L);
        when(enrollmentRepository.countByUserIdAndStatus(1L, "pending")).thenReturn(1L);
        when(enrollmentRepository.countByUserIdAndStatus(1L, "inprogress")).thenReturn(2L);
        when(enrollmentRepository.countCompletedByUserId(1L)).thenReturn(1L);

        var result = enrollmentService.getDashboardStats(1L);

        assertThat(result.getTotalEnrolled()).isEqualTo(5);
        assertThat(result.getPendingCount()).isEqualTo(1);
        assertThat(result.getInProgress()).isEqualTo(2);
        assertThat(result.getCompleted()).isEqualTo(1);
    }

    /* ─── admin enrollment management ─────────────────────── */

    @Test
    void approveEnrollment_setsActiveAndNotifies() {
        User student = buildStudent(1L);
        User admin = buildAdmin(2L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(1L, student, course);
        enrollment.setStatus("pending");

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        enrollmentService.approveEnrollment(1L, 2L);

        assertThat(enrollment.getStatus()).isEqualTo("active");
        assertThat(enrollment.getApprovedBy()).isEqualTo(admin);
        verify(notificationPushService).sendNotification(eq(1L), anyString(), any(), eq("ENROLLMENT"));
    }

    @Test
    void revokeEnrollment_setsRevokedAndNotifies() {
        User student = buildStudent(1L);
        Course course = buildCourse(10L);
        Enrollment enrollment = buildEnrollment(1L, student, course);
        enrollment.setStatus("active");

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        enrollmentService.revokeEnrollment(1L, "Inactive student");

        assertThat(enrollment.getStatus()).isEqualTo("revoked");
        assertThat(enrollment.getTeacherNote()).isEqualTo("Inactive student");
        verify(notificationPushService).sendNotification(eq(student.getId()), anyString(), any(), eq("ENROLLMENT"));
    }

    /* ─── directEnrollByAdmin ─────────────────────────────── */

    @Test
    void directEnrollByAdmin_newEnrollment_createsActive() {
        User admin = buildAdmin(1L);
        User student = buildStudent(2L);
        Course course = buildCourse(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(2L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });
        when(courseMapper.toEnrolledResponse(any(Enrollment.class))).thenReturn(
                EnrolledCourseResponse.builder().enrollmentId(100L).status("active").build());

        EnrolledCourseResponse result = enrollmentService.directEnrollByAdmin(1L, 2L, 10L);

        assertThat(result.getStatus()).isEqualTo("active");
        verify(notificationPushService).sendNotification(eq(2L), anyString(), any(), eq("ENROLLMENT"));
    }

    @Test
    void directEnrollByAdmin_existingRevokedEnrollment_reactivates() {
        User admin = buildAdmin(1L);
        User student = buildStudent(2L);
        Course course = buildCourse(10L);
        Enrollment existing = buildEnrollment(1L, student, course);
        existing.setStatus("revoked");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(2L, 10L)).thenReturn(true);
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 10L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseMapper.toEnrolledResponse(any(Enrollment.class))).thenReturn(
                EnrolledCourseResponse.builder().status("active").build());

        enrollmentService.directEnrollByAdmin(1L, 2L, 10L);

        assertThat(existing.getStatus()).isEqualTo("active");
        assertThat(existing.getApprovedBy()).isEqualTo(admin);
    }
}
