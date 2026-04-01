package com.sso.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sso.dto.response.ApiResponse;
import com.sso.entity.Course;
import com.sso.entity.User;
import com.sso.exception.ResourceNotFoundException;
import com.sso.moodle.MoodleSyncService;
import com.sso.repository.CourseRepository;
import com.sso.repository.CourseTeacherRepository;
import com.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MoodleController {

    private final MoodleSyncService moodleSyncService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final com.sso.moodle.MoodleProperties moodleProperties;

    /* ─────── Admin endpoints (/api/v1/admin/moodle/**) ─── */

    @PostMapping("/admin/moodle/sync-courses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncAllCourses() {
        int count = moodleSyncService.syncAllCourses();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("synced", count)));
    }

    /**
     * Pull course info (image, name, description) FROM Moodle back to EnglishEdu.
     * Call this after updating course images/details in Moodle.
     */
    @PostMapping("/admin/moodle/sync-courses-from-moodle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncAllCoursesFromMoodle() {
        int count = moodleSyncService.syncAllCoursesFromMoodle();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", count)));
    }

    @PostMapping("/admin/moodle/sync-users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncAllUsers() {
        int count = moodleSyncService.syncAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("synced", count)));
    }

    @GetMapping("/admin/moodle/status")
    public ResponseEntity<ApiResponse<JsonNode>> getMoodleStatus() {
        JsonNode info = moodleSyncService.testConnection();
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    /* ─────── Authenticated endpoints ────────────────────── */

    /**
     * Step 3 – SSO launch into a Moodle course (student view).
     * Provisions user + course on Moodle if not yet done, then returns a
     * one-time auto-login URL that drops the user directly into the course.
     */
    @GetMapping("/moodle/launch")
    public ResponseEntity<ApiResponse<Map<String, String>>> launchMoodle(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long courseId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Step 1 guarantee: ensure both exist on Moodle
        moodleSyncService.ensureMoodleUser(user);
        moodleSyncService.ensureMoodleCourse(course);

        // If user is a teacher assigned to this course, enrol them in Moodle with teacher role
        if ("TEACHER".equalsIgnoreCase(user.getRole()) &&
                courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), user.getId())) {
            try {
                moodleSyncService.syncTeacherEnrolment(user, course);
            } catch (Exception e) {
                log.warn("Moodle teacher enrol on launch failed for user={} course={}: {}",
                        user.getUsername(), course.getId(), e.getMessage());
            }
        }

        // Build SSO URL – redirects straight into the course after auto-login
        String moodlePath = course.getMoodleCourseId() != null
                ? "/course/view.php?id=" + course.getMoodleCourseId()
                : null;
        String ssoUrl = moodleSyncService.buildSsoUrl(user, moodlePath);

        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", ssoUrl)));
    }

    /**
     * Teacher SSO launch – "Soạn giáo án chi tiết".
     * Takes the teacher directly into the Moodle course editing page so they
     * can author quizzes, upload audio, configure assignments, grade essays, etc.
     * <p>
     * Only TEACHER (assigned to this course) and ADMIN roles are allowed.
     * The teacher is auto-enrolled on Moodle with the editing-teacher role (3).
     */
    @GetMapping("/moodle/teacher-launch")
    public ResponseEntity<ApiResponse<Map<String, String>>> teacherLaunchMoodle(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long courseId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        // Only TEACHER or ADMIN may use this endpoint
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isAssignedTeacher = "TEACHER".equalsIgnoreCase(user.getRole()) &&
                courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), user.getId());

        if (!isAdmin && !isAssignedTeacher) {
            return ResponseEntity.status(403).body(
                    ApiResponse.error("Bạn không có quyền soạn giáo án cho khóa học này."));
        }

        // Provision Moodle user + course
        moodleSyncService.ensureMoodleUser(user);
        moodleSyncService.ensureMoodleCourse(course);

        // Enrol as editing teacher (role 3) on Moodle
        try {
            moodleSyncService.syncTeacherEnrolment(user, course);
        } catch (Exception e) {
            log.warn("Moodle teacher enrol failed for user={} course={}: {}",
                    user.getUsername(), course.getId(), e.getMessage());
        }

        // Build SSO URL – land on course page (editing teacher sees edit controls)
        String moodlePath = course.getMoodleCourseId() != null
                ? "/course/view.php?id=" + course.getMoodleCourseId()
                : null;
        String ssoUrl = moodleSyncService.buildSsoUrl(user, moodlePath);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "url", ssoUrl,
                "moodleBaseUrl", moodleProperties.getUrl()
        )));
    }

    /**
     * Get Moodle grades for the authenticated user in a specific course.
     */
    @GetMapping("/moodle/grades")
    public ResponseEntity<ApiResponse<JsonNode>> getGrades(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long courseId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        JsonNode grades = moodleSyncService.getStudentGrades(user, course);
        return ResponseEntity.ok(ApiResponse.ok(grades));
    }

    /**
     * Get Moodle course contents (sections, activities).
     */
    @GetMapping("/moodle/course-contents")
    public ResponseEntity<ApiResponse<JsonNode>> getCourseContents(@RequestParam Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        JsonNode contents = moodleSyncService.getCourseContents(course);
        return ResponseEntity.ok(ApiResponse.ok(contents));
    }

    /* ─────── Calendar proxy (replaces CalendarController) ─── */

    /**
     * Get calendar events from Moodle for the current user.
     * Query params: year, month (defaults to current month).
     */
    @GetMapping("/moodle/calendar")
    public ResponseEntity<ApiResponse<JsonNode>> getCalendarEvents(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        long timeStart = LocalDate.of(y, m, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long timeEnd = LocalDate.of(y, m, 1).plusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);

        JsonNode events = moodleSyncService.getCalendarEvents(user, timeStart, timeEnd);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    /* ─────── Timeline proxy (replaces TimelineController) ─── */

    /**
     * Get upcoming action events from Moodle (timeline).
     * Query params: days (default 7), limit (default 50).
     */
    @GetMapping("/moodle/timeline")
    public ResponseEntity<ApiResponse<JsonNode>> getTimeline(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "50") int limit) {
        long now = Instant.now().getEpochSecond();
        long end = Instant.now().plus(days, ChronoUnit.DAYS).getEpochSecond();
        JsonNode events = moodleSyncService.getActionEvents(now, end, limit);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    /* ─────── Moodle courses for user ────────────────────── */

    /**
     * Get all Moodle courses the authenticated user is enrolled in.
     */
    @GetMapping("/moodle/my-courses")
    public ResponseEntity<ApiResponse<JsonNode>> getMyCourses(
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JsonNode courses = moodleSyncService.getMoodleCourses(user);
        return ResponseEntity.ok(ApiResponse.ok(courses));
    }

    /* ─────── Assignment endpoints ───────────────────────── */

    @GetMapping("/moodle/assignments")
    public ResponseEntity<ApiResponse<JsonNode>> getAssignments(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        JsonNode assignments = moodleSyncService.getAssignments(course);
        return ResponseEntity.ok(ApiResponse.ok(assignments));
    }

    @GetMapping("/moodle/assignment-status")
    public ResponseEntity<ApiResponse<JsonNode>> getAssignmentStatus(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long assignmentId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Admins and teachers do not submit assignments — skip Moodle call to avoid permission errors
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || "TEACHER".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        JsonNode status = moodleSyncService.getSubmissionStatus(assignmentId, user);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/moodle/assignment-submit-text")
    public ResponseEntity<ApiResponse<JsonNode>> submitAssignmentText(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, Object> body) {
        long assignmentId = ((Number) body.get("assignmentId")).longValue();
        String text = (String) body.get("text");
        int itemId = body.containsKey("itemId") ? ((Number) body.get("itemId")).intValue() : 0;
        JsonNode result = moodleSyncService.saveTextSubmission(assignmentId, text, itemId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/moodle/assignment-submit-file")
    public ResponseEntity<ApiResponse<JsonNode>> submitAssignmentFile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // Upload file to Moodle draft area
        long itemId = moodleSyncService.uploadFile(file.getBytes(), file.getOriginalFilename());
        // Attach to assignment submission
        JsonNode result = moodleSyncService.saveFileSubmission(assignmentId, (int) itemId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/moodle/assignment-submit-grading")
    public ResponseEntity<ApiResponse<JsonNode>> submitForGrading(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long assignmentId) {
        JsonNode result = moodleSyncService.submitForGrading(assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /* ─────── Quiz endpoints ─────────────────────────────── */

    @GetMapping("/moodle/quizzes")
    public ResponseEntity<ApiResponse<JsonNode>> getQuizzes(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        JsonNode quizzes = moodleSyncService.getQuizzes(course);
        return ResponseEntity.ok(ApiResponse.ok(quizzes));
    }

    @GetMapping("/moodle/quiz/attempts")
    public ResponseEntity<ApiResponse<JsonNode>> getQuizAttempts(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long quizId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Teachers and admins do not attempt quizzes;
        // return an empty attempts wrapper to avoid Moodle "Access control exception".
        if ("TEACHER".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
            com.fasterxml.jackson.databind.node.ObjectNode empty =
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            empty.putArray("attempts");
            return ResponseEntity.ok(ApiResponse.ok(empty));
        }
        JsonNode attempts = moodleSyncService.getQuizAttempts(quizId, user);
        return ResponseEntity.ok(ApiResponse.ok(attempts));
    }

    @PostMapping("/moodle/quiz/start")
    public ResponseEntity<ApiResponse<JsonNode>> startQuiz(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long quizId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JsonNode result = moodleSyncService.startQuizAttempt(quizId, user);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/moodle/quiz/attempt-data")
    public ResponseEntity<ApiResponse<JsonNode>> getAttemptData(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long attemptId,
            @RequestParam(defaultValue = "0") int page) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JsonNode data = moodleSyncService.getAttemptData(attemptId, page, user);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/moodle/quiz/save")
    public ResponseEntity<ApiResponse<JsonNode>> saveQuizAnswers(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long attemptId = ((Number) body.get("attemptId")).longValue();
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) body.get("answers");
        JsonNode result = moodleSyncService.saveAttempt(attemptId, answers != null ? answers : Map.of(), user);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/moodle/quiz/submit")
    public ResponseEntity<ApiResponse<JsonNode>> submitQuiz(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long attemptId = ((Number) body.get("attemptId")).longValue();
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) body.get("answers");
        JsonNode result = moodleSyncService.processAttempt(attemptId, answers != null ? answers : Map.of(), user);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/moodle/quiz/review")
    public ResponseEntity<ApiResponse<JsonNode>> reviewQuiz(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long attemptId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JsonNode review = moodleSyncService.getAttemptReview(attemptId, user);
        return ResponseEntity.ok(ApiResponse.ok(review));
    }

    @GetMapping("/moodle/quiz/summary")
    public ResponseEntity<ApiResponse<JsonNode>> getQuizSummary(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long attemptId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        JsonNode summary = moodleSyncService.getAttemptSummary(attemptId, user);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /* ─────── Module content endpoints ───────────────────── */

    @GetMapping("/moodle/pages")
    public ResponseEntity<ApiResponse<JsonNode>> getPages(@RequestParam Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        JsonNode pages = moodleSyncService.getPages(course);
        return ResponseEntity.ok(ApiResponse.ok(pages));
    }

    @GetMapping("/moodle/resources")
    public ResponseEntity<ApiResponse<JsonNode>> getResources(@RequestParam Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        JsonNode resources = moodleSyncService.getResources(course);
        return ResponseEntity.ok(ApiResponse.ok(resources));
    }

    /* ─────── Completion endpoints ───────────────────────── */

    @GetMapping("/moodle/completion")
    public ResponseEntity<ApiResponse<JsonNode>> getCompletionStatus(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long courseId) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Admins are not enrolled as students in Moodle courses — skip completion API to avoid errors
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        JsonNode completion = moodleSyncService.getCompletionStatus(course, user);
        return ResponseEntity.ok(ApiResponse.ok(completion));
    }

    @PostMapping("/moodle/completion/toggle")
    public ResponseEntity<ApiResponse<JsonNode>> toggleCompletion(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, Object> body) {
        long cmId = ((Number) body.get("cmId")).longValue();
        boolean completed = (Boolean) body.get("completed");
        JsonNode result = moodleSyncService.updateActivityCompletion(cmId, completed);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /* ─────── Moodle user token for direct file access ───── */

    @GetMapping("/moodle/user-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUserMoodleToken(
            @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String token = moodleSyncService.ensureMoodleToken(user);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("token", token, "baseUrl", moodleProperties.getUrl())));
    }

    /* ─────── Course image proxy (public, no token exposed) ─── */

    /**
     * Proxy a Moodle course overview image (cover photo) through the backend.
     * The Moodle admin token is added server-side; clients never see it.
     * URL is stable even if the image is replaced in Moodle.
     */
    @GetMapping("/moodle/course-image/{courseId}")
    public ResponseEntity<byte[]> getCourseImage(@PathVariable Long courseId) {
        com.sso.entity.Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getMoodleCourseId() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String fileUrl = moodleSyncService.getCourseOverviewFileUrl(course);
            if (fileUrl == null || fileUrl.isBlank()) {
                return ResponseEntity.notFound().build();
            }
            byte[] data = moodleSyncService.downloadFile(fileUrl);
            // Detect content type from URL extension
            String lower = fileUrl.toLowerCase().replaceAll("\\?.*", "");
            org.springframework.http.MediaType mediaType =
                    lower.endsWith(".png")  ? org.springframework.http.MediaType.IMAGE_PNG
                  : lower.endsWith(".gif")  ? org.springframework.http.MediaType.IMAGE_GIF
                  : lower.endsWith(".webp") ? org.springframework.http.MediaType.parseMediaType("image/webp")
                  :                           org.springframework.http.MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(data);
        } catch (Exception e) {
            log.warn("Course image proxy failed for courseId={}: {}", courseId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /* ─────── File proxy endpoint (fallback) ──────────────── */

    @GetMapping("/moodle/file")
    public ResponseEntity<byte[]> proxyMoodleFile(@RequestParam String url) {
        // Validate URL to prevent SSRF - only allow Moodle pluginfile URLs
        String moodleBase = moodleProperties.getUrl();
        boolean isMoodleUrl = url.startsWith(moodleBase + "/");
        boolean isPluginFile = url.contains("/pluginfile.php/") || url.contains("/webservice/pluginfile.php/");
        if (!isMoodleUrl || !isPluginFile) {
            return ResponseEntity.badRequest().build();
        }
        byte[] data = moodleSyncService.downloadFile(url);
        String fileName = url.substring(url.lastIndexOf('/') + 1).replaceAll("\\?.*", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/moodle/file-upload")
    public ResponseEntity<ApiResponse<Map<String, Long>>> uploadFile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) throws IOException {
        long itemId = moodleSyncService.uploadFile(file.getBytes(), file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("itemId", itemId)));
    }
}
