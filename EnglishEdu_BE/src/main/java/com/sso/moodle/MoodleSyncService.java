package com.sso.moodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.sso.entity.Course;
import com.sso.entity.User;
import com.sso.repository.CourseRepository;
import com.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Synchronises EnglishEdu entities with Moodle via the Web Services REST API.
 * <ul>
 *   <li>User sync  – creates Moodle account on first login / enrolment</li>
 *   <li>Course sync – pushes new courses to Moodle</li>
 *   <li>Enrolment sync – enrols students/teachers on Moodle when approved</li>
 *   <li>SSO login URL – generates a signed redirect so students can access Moodle seamlessly</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoodleSyncService {

    private final MoodleClient moodleClient;
    private final MoodleProperties moodleProperties;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    private static final int MOODLE_ROLE_STUDENT = 5;
    private static final int MOODLE_ROLE_TEACHER = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    /* ─────────── User sync ─────────────────────────────────── */

    /**
     * Ensure the user exists on Moodle.  If not, create them.
     * Stores the moodleId back on the User entity.
     * Uses REQUIRES_NEW so Moodle API failures don't taint the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long ensureMoodleUser(User user) {
        if (user.getMoodleId() != null) {
            return user.getMoodleId();
        }

        // Check if user already exists on Moodle by username
        JsonNode existing = moodleClient.getUserByUsername(user.getUsername());
        long moodleId;
        if (existing != null) {
            moodleId = existing.path("id").asLong();
        } else {
            // Generate a random password for the Moodle account
            String moodlePassword = "Sso!" + generateRandomHex(12);
            moodleId = moodleClient.createUser(
                    user.getUsername(),
                    moodlePassword,
                    user.getEmail(),
                    user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                    user.getLastName() != null ? user.getLastName() : "."
            );
            log.info("Created Moodle user {} (moodleId={})", user.getUsername(), moodleId);

            // Immediately obtain a per-user token for student-context API calls
            try {
                String token = moodleClient.requestUserToken(
                        user.getUsername(), moodlePassword, moodleProperties.getServiceName());
                user.setMoodleToken(token);
            } catch (Exception e) {
                log.warn("Could not obtain Moodle token for new user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        user.setMoodleId(moodleId);
        userRepository.save(user);
        return moodleId;
    }

    /**
     * Ensure the user has a per-user Moodle web-service token.
     * If the user doesn't have one (e.g. created before this feature),
     * reset their Moodle password and request a new token.
     *
     * @return the user's Moodle token (never null – throws on failure)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String ensureMoodleToken(User user) {
        if (user.getMoodleToken() != null) {
            return user.getMoodleToken();
        }
        // Ensure the user exists on Moodle first
        long moodleId = ensureMoodleUser(user);

        // Reset their Moodle password to a known value
        String tempPassword = "Sso!" + generateRandomHex(12);
        moodleClient.updateUserPassword(moodleId, tempPassword);

        // Request a token using the new password
        String token = moodleClient.requestUserToken(
                user.getUsername(), tempPassword, moodleProperties.getServiceName());
        user.setMoodleToken(token);
        userRepository.save(user);
        log.info("Obtained Moodle token for existing user {}", user.getUsername());
        return token;
    }

    /* ─────────── Course sync ───────────────────────────────── */

    /**
     * Ensure the course exists on Moodle.  If not, create it.
     * Stores the moodleCourseId back on the Course entity.
     * Uses REQUIRES_NEW so Moodle API failures don't taint the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long ensureMoodleCourse(Course course) {
        if (course.getMoodleCourseId() != null) {
            return course.getMoodleCourseId();
        }

        String shortname = "SSO-" + course.getId();
        long moodleCourseId = moodleClient.createCourse(
                course.getName(),
                shortname,
                "1", // default category
                course.getDescription()
        );

        course.setMoodleCourseId(moodleCourseId);
        courseRepository.save(course);
        log.info("Created Moodle course '{}' (moodleCourseId={})", course.getName(), moodleCourseId);
        return moodleCourseId;
    }

    /* ─────────── Enrolment sync ────────────────────────────── */

    /**
     * Enrol a student into a course on Moodle.
     * Both user and course are auto-provisioned if needed.
     * Uses REQUIRES_NEW to isolate Moodle failures from the caller's business transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncStudentEnrolment(User student, Course course) {
        long moodleUserId = ensureMoodleUser(student);
        long moodleCourseId = ensureMoodleCourse(course);
        moodleClient.enrolUser(moodleUserId, moodleCourseId, MOODLE_ROLE_STUDENT);
        log.info("Enrolled student {} in Moodle course {} (role=student)",
                student.getUsername(), course.getName());
    }

    /**
     * Enrol a teacher into a course on Moodle (editing teacher role).
     * Uses REQUIRES_NEW to isolate Moodle failures from the caller's business transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncTeacherEnrolment(User teacher, Course course) {
        long moodleUserId = ensureMoodleUser(teacher);
        long moodleCourseId = ensureMoodleCourse(course);
        moodleClient.enrolUser(moodleUserId, moodleCourseId, MOODLE_ROLE_TEACHER);
        log.info("Enrolled teacher {} in Moodle course {} (role=editingteacher)",
                teacher.getUsername(), course.getName());
    }

    /* ─────────── SSO login URL ─────────────────────────────── */

    /**
     * Generate a signed SSO login URL that the frontend redirects the user to.
     * Format: {moodleUrl}/auth/userkey/login.php?key={signedToken}
     * <p>
     * Since Moodle's auth_userkey plugin may not be installed, we use a simpler
     * approach: generate a URL to the course with auto-login params.
     * The URL contains an HMAC-signed timestamp + username so Moodle can verify.
     */
    /**
     * Step 3 – Generate an SSO login URL.
     * Uses Moodle's auth_userkey plugin to produce a one-time key URL that
     * automatically logs the user in and then redirects them to {@code moodlePath}.
     *
     * @param user       the EnglishEdu user (must already have a moodleId)
     * @param moodlePath Moodle-relative path to land on, e.g. "/course/view.php?id=5"
     * @return auto-login URL (falls back to bare URL if auth_userkey is unavailable)
     */
    public String buildSsoUrl(User user, String moodlePath) {
        // Ensure the Moodle account exists before requesting an SSO key
        if (user.getMoodleId() == null) {
            try { ensureMoodleUser(user); } catch (Exception e) {
                log.warn("Could not provision Moodle user '{}' for SSO: {}", user.getUsername(), e.getMessage());
            }
        }
        String wantsUrl = (moodlePath != null && !moodlePath.isBlank())
                ? moodleProperties.getUrl() + moodlePath
                : moodleProperties.getUrl() + "/my/";
        return moodleClient.requestSsoLoginUrl(user.getUsername(), wantsUrl);
    }

    /**
     * Build the course URL for a student to access on Moodle.
     */
    public String buildCourseUrl(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleProperties.getUrl() + "/course/view.php?id=" + course.getMoodleCourseId();
    }

    /* ─────────── Grades ────────────────────────────────────── */

    /**
     * Fetch a student's grades from Moodle for a specific course.
     */
    public JsonNode getStudentGrades(User student, Course course) {
        if (student.getMoodleId() == null || course.getMoodleCourseId() == null) {
            return null;
        }
        return moodleClient.getUserGrades(course.getMoodleCourseId(), student.getMoodleId());
    }

    /**
     * Fetch all course contents (sections, activities) from Moodle.
     */
    public JsonNode getCourseContents(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleClient.getCourseContents(course.getMoodleCourseId());
    }

    /* ─────────── Bulk sync ─────────────────────────────────── */

    /**
     * Sync all existing courses to Moodle (admin-triggered).
     * Returns count of newly synced courses.
     */
    @Transactional
    public int syncAllCourses() {
        var courses = courseRepository.findAll();
        int count = 0;
        for (Course c : courses) {
            if (c.getMoodleCourseId() == null) {
                try {
                    ensureMoodleCourse(c);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to sync course '{}' to Moodle: {}", c.getName(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Sync all existing users to Moodle (admin-triggered).
     * Returns count of newly synced users.
     */
    @Transactional
    public int syncAllUsers() {
        var users = userRepository.findAll();
        int count = 0;
        for (User u : users) {
            if (u.getMoodleId() == null && !u.isGuest()) {
                try {
                    ensureMoodleUser(u);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to sync user '{}' to Moodle: {}", u.getUsername(), e.getMessage());
                }
            }
        }
        return count;
    }

    /* ─────────── Sync FROM Moodle ─────────────────────────── */

    /**
     * Get the overview (cover) image URL for a Moodle course.
     * Returns the raw Moodle fileurl (with token) or null if none set.
     */
    public String getCourseOverviewFileUrl(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        try {
            JsonNode result = moodleClient.getCoursesByField("ids",
                    String.valueOf(course.getMoodleCourseId()));
            JsonNode courses = result.path("courses");
            if (!courses.isArray() || courses.isEmpty()) return null;
            JsonNode overviewFiles = courses.get(0).path("overviewfiles");
            if (overviewFiles.isArray() && !overviewFiles.isEmpty()) {
                String url = overviewFiles.get(0).path("fileurl").asText("");
                return url.isBlank() ? null : url;
            }
        } catch (Exception e) {
            log.warn("getCourseOverviewFileUrl failed for course {}: {}", course.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Pull course info FROM Moodle and update local DB.
     * Syncs: image (overviewfiles), name, description.
     *
     * @return true if any field was updated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean syncCourseFromMoodle(Course course) {
        if (course.getMoodleCourseId() == null) return false;

        JsonNode result = moodleClient.getCoursesByField("ids",
                String.valueOf(course.getMoodleCourseId()));
        JsonNode courses = result.path("courses");
        if (!courses.isArray() || courses.isEmpty()) return false;

        JsonNode mc = courses.get(0);
        boolean changed = false;

        // Sync image: store a stable local proxy path (token is added server-side by the proxy)
        JsonNode overviewFiles = mc.path("overviewfiles");
        if (overviewFiles.isArray() && !overviewFiles.isEmpty()) {
            String fileUrl = overviewFiles.get(0).path("fileurl").asText("");
            if (!fileUrl.isBlank()) {
                // Point imageUrl to our backend proxy — never expose the Moodle admin token to clients
                String proxyPath = "/api/v1/moodle/course-image/" + course.getId();
                if (!proxyPath.equals(course.getImageUrl())) {
                    course.setImageUrl(proxyPath);
                    changed = true;
                }
            }
        }

        // Sync name
        String moodleName = mc.path("fullname").asText("");
        if (!moodleName.isBlank() && !moodleName.equals(course.getName())) {
            course.setName(moodleName);
            changed = true;
        }

        // Sync description/summary
        String moodleSummary = mc.path("summary").asText("");
        if (!moodleSummary.isBlank() && !moodleSummary.equals(course.getDescription())) {
            course.setDescription(moodleSummary);
            changed = true;
        }

        if (changed) {
            courseRepository.save(course);
            log.info("Synced course '{}' (id={}) from Moodle", course.getName(), course.getId());
        }
        return changed;
    }

    /**
     * Sync ALL courses from Moodle back to EnglishEdu (admin-triggered).
     * Returns count of updated courses.
     */
    @Transactional
    public int syncAllCoursesFromMoodle() {
        var courses = courseRepository.findAll();
        int count = 0;
        for (Course c : courses) {
            if (c.getMoodleCourseId() != null) {
                try {
                    if (syncCourseFromMoodle(c)) count++;
                } catch (Exception e) {
                    log.warn("Failed to sync course '{}' from Moodle: {}", c.getName(), e.getMessage());
                }
            }
        }
        return count;
    }

    /* ─────────── Connection test ───────────────────────────── */

    /**
     * Test the Moodle connection by calling get_site_info.
     */
    public JsonNode testConnection() {
        return moodleClient.getSiteInfo();
    }

    /* ─────────── Calendar & Timeline proxy ─────────────────── */

    /**
     * Fetch calendar events from Moodle for a given user + time range.
     */
    public JsonNode getCalendarEvents(User user, long timeStart, long timeEnd) {
        long moodleUserId = ensureMoodleUser(user);
        return moodleClient.getCalendarEvents(moodleUserId, timeStart, timeEnd);
    }

    /**
     * Fetch upcoming action events (timeline) from Moodle.
     */
    public JsonNode getActionEvents(long timeSortFrom, long timeSortTo, int limitNum) {
        return moodleClient.getActionEvents(timeSortFrom, timeSortTo, limitNum);
    }

    /**
     * Fetch all Moodle courses for a user (enrolled).
     */
    public JsonNode getMoodleCourses(User user) {
        long moodleUserId = ensureMoodleUser(user);
        return moodleClient.getUserMoodleCourses(moodleUserId);
    }

    /* ─────────── Assignment proxy ──────────────────────────── */

    public JsonNode getAssignments(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleClient.getAssignments(java.util.List.of(course.getMoodleCourseId()));
    }

    public JsonNode getSubmissionStatus(long assignId, User user) {
        if (user.getMoodleId() == null) return null;
        return moodleClient.getSubmissionStatus(assignId, user.getMoodleId());
    }

    public JsonNode saveTextSubmission(long assignId, String text, int itemId) {
        return moodleClient.saveTextSubmission(assignId, text, itemId);
    }

    public JsonNode saveFileSubmission(long assignId, int fileItemId) {
        return moodleClient.saveFileSubmission(assignId, fileItemId);
    }

    public JsonNode submitForGrading(long assignId) {
        return moodleClient.submitForGrading(assignId);
    }

    /* ─────────── Quiz proxy (uses per-user tokens) ───────────── */

    public JsonNode getQuizzes(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleClient.getQuizzesByCourses(java.util.List.of(course.getMoodleCourseId()));
    }

    public JsonNode getQuizAttempts(long quizId, User user) {
        if (user.getMoodleId() == null) return null;
        String token = ensureMoodleToken(user);
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("quizid", String.valueOf(quizId));
        params.put("userid", String.valueOf(user.getMoodleId()));
        params.put("status", "all");
        params.put("includepreviews", "0");
        return moodleClient.callAsUser("mod_quiz_get_user_quiz_attempts", params, token);
    }

    public JsonNode startQuizAttempt(long quizId, User user) {
        String token = ensureMoodleToken(user);
        return moodleClient.callAsUser("mod_quiz_start_attempt",
                java.util.Map.of("quizid", String.valueOf(quizId)), token);
    }

    public JsonNode getAttemptData(long attemptId, int page, User user) {
        String token = ensureMoodleToken(user);
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("attemptid", String.valueOf(attemptId));
        params.put("page", String.valueOf(page));
        return moodleClient.callAsUser("mod_quiz_get_attempt_data", params, token);
    }

    public JsonNode saveAttempt(long attemptId, java.util.Map<String, String> answers, User user) {
        String token = ensureMoodleToken(user);
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("attemptid", String.valueOf(attemptId));
        int i = 0;
        for (var entry : answers.entrySet()) {
            params.put("data[" + i + "][name]", entry.getKey());
            params.put("data[" + i + "][value]", entry.getValue());
            i++;
        }
        return moodleClient.callAsUser("mod_quiz_save_attempt", params, token);
    }

    public JsonNode processAttempt(long attemptId, java.util.Map<String, String> answers, User user) {
        String token = ensureMoodleToken(user);
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("attemptid", String.valueOf(attemptId));
        params.put("finishattempt", "1");
        int i = 0;
        for (var entry : answers.entrySet()) {
            params.put("data[" + i + "][name]", entry.getKey());
            params.put("data[" + i + "][value]", entry.getValue());
            i++;
        }
        return moodleClient.callAsUser("mod_quiz_process_attempt", params, token);
    }

    public JsonNode getAttemptReview(long attemptId, User user) {
        String token = ensureMoodleToken(user);
        return moodleClient.callAsUser("mod_quiz_get_attempt_review",
                java.util.Map.of("attemptid", String.valueOf(attemptId)), token);
    }

    public JsonNode getAttemptSummary(long attemptId, User user) {
        String token = ensureMoodleToken(user);
        return moodleClient.callAsUser("mod_quiz_get_attempt_summary",
                java.util.Map.of("attemptid", String.valueOf(attemptId)), token);
    }

    /* ─────────── Module content proxy ──────────────────────── */

    public JsonNode getPages(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleClient.getPagesByCourses(java.util.List.of(course.getMoodleCourseId()));
    }

    public JsonNode getResources(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleClient.getResourcesByCourses(java.util.List.of(course.getMoodleCourseId()));
    }

    public JsonNode getUrls(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleClient.getUrlsByCourses(java.util.List.of(course.getMoodleCourseId()));
    }

    /* ─────────── Completion proxy ──────────────────────────── */

    public JsonNode getCompletionStatus(Course course, User user) {
        if (course.getMoodleCourseId() == null || user.getMoodleId() == null) return null;
        return moodleClient.getActivitiesCompletionStatus(course.getMoodleCourseId(), user.getMoodleId());
    }

    public JsonNode updateActivityCompletion(long cmId, boolean completed) {
        return moodleClient.updateActivityCompletion(cmId, completed);
    }

    /* ─────────── File proxy ────────────────────────────────── */

    public byte[] downloadFile(String fileUrl) {
        return moodleClient.downloadMoodleFile(fileUrl);
    }

    public long uploadFile(byte[] fileData, String fileName) {
        return moodleClient.uploadFile(fileData, fileName);
    }

    /* ─────────── Helpers ───────────────────────────────────── */

    private static String generateRandomHex(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }
}
