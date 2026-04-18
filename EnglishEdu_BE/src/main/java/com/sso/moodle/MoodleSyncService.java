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
    private final com.sso.repository.EnrollmentRepository enrollmentRepository;

    private static final int MOODLE_ROLE_STUDENT = 5;
    private static final int MOODLE_ROLE_TEACHER = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    /* ─────────── User sync ─────────────────────────────────── */

    /**
     * Provision user on Moodle (create if absent) and obtain a per-user token.
     * Does NOT persist the moodleId/moodleToken – the caller's transaction does that.
     * This avoids the REQUIRES_NEW deadlock when the caller already holds a lock on the user row.
     *
     * @return the Moodle user id
     */
    public long provisionMoodleUser(User user) {
        if (user.getMoodleId() != null) {
            return user.getMoodleId();
        }

        // Guard: skip if Moodle is not configured
        String moodleUrl = moodleProperties.getUrl();
        String moodleToken = moodleProperties.getToken();
        if (moodleUrl == null || moodleUrl.isBlank()) {
            throw new MoodleApiException("Moodle URL is not configured — cannot sync user");
        }
        if (moodleToken == null || moodleToken.isBlank()) {
            throw new MoodleApiException("Moodle token is not configured — set MOODLE_TOKEN env var");
        }

        log.info("[MoodleSync] Provisioning user '{}' (email={}) on Moodle…", user.getUsername(), user.getEmail());

        // Step 1: Try to find existing user on Moodle by username, then by email
        JsonNode existing = null;
        try {
            existing = moodleClient.getUserByUsername(user.getUsername());
        } catch (Exception e) {
            log.warn("[MoodleSync] getUserByUsername('{}') failed: {}", user.getUsername(), e.getMessage());
        }
        if (existing == null && user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                existing = moodleClient.getUserByEmail(user.getEmail());
                if (existing != null) {
                    log.info("[MoodleSync] User '{}' not found by username but found by email on Moodle", user.getUsername());
                }
            } catch (Exception e) {
                log.warn("[MoodleSync] getUserByEmail('{}') failed: {}", user.getEmail(), e.getMessage());
            }
        }

        long moodleId;
        if (existing != null) {
            moodleId = existing.path("id").asLong();
            if (moodleId == 0) {
                throw new MoodleApiException("Moodle user found for '" + user.getUsername()
                        + "' but has no valid id: " + existing);
            }
            log.info("[MoodleSync] User '{}' already exists on Moodle (moodleId={})", user.getUsername(), moodleId);
        } else {
            // Step 2: Create the user on Moodle
            String moodlePassword = "Sso!" + generateRandomHex(12);
            try {
                moodleId = moodleClient.createUser(
                        user.getUsername(),
                        moodlePassword,
                        user.getEmail(),
                        user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                        user.getLastName() != null ? user.getLastName() : "."
                );
            } catch (MoodleApiException e) {
                // Creation failed — user might already exist (duplicate email/username).
                // Try one more lookup before giving up.
                log.warn("[MoodleSync] createUser failed for '{}': {}. Retrying lookup…",
                        user.getUsername(), e.getMessage());
                JsonNode retryLookup = moodleClient.getUserByUsername(user.getUsername());
                if (retryLookup == null && user.getEmail() != null) {
                    retryLookup = moodleClient.getUserByEmail(user.getEmail());
                }
                if (retryLookup != null && retryLookup.path("id").asLong() > 0) {
                    moodleId = retryLookup.path("id").asLong();
                    log.info("[MoodleSync] Found existing user '{}' on retry (moodleId={})",
                            user.getUsername(), moodleId);
                } else {
                    throw e; // truly cannot create or find user
                }
                // skip token request since we don't know the password
                user.setMoodleId(moodleId);
                return moodleId;
            }
            log.info("[MoodleSync] Created Moodle user '{}' (moodleId={})", user.getUsername(), moodleId);

            try {
                String token = moodleClient.requestUserToken(
                        user.getUsername(), moodlePassword, moodleProperties.getServiceName());
                user.setMoodleToken(token);
            } catch (Exception e) {
                log.warn("[MoodleSync] Could not obtain Moodle token for '{}': {}", user.getUsername(), e.getMessage());
            }
        }

        user.setMoodleId(moodleId);
        return moodleId;
    }

    /**
     * Ensure the user exists on Moodle.  If not, create them.
     * Stores the moodleId back on the User entity.
     * Uses REQUIRES_NEW so Moodle API failures don't taint the caller's transaction.
     *
     * WARNING: Do NOT call from a method that already holds a write-lock on the same User row
     * (e.g. createUser / register). Use {@link #provisionMoodleUser(User)} instead.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long ensureMoodleUser(User user) {
        long moodleId = provisionMoodleUser(user);
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

    /**
     * Unenrol a student from a course on Moodle.
     * Silently ignored if user or course is not yet provisioned on Moodle.
     */
    public void unenrolStudent(User student, Course course) {
        if (student.getMoodleId() == null || course.getMoodleCourseId() == null) return;
        moodleClient.unenrolUser(student.getMoodleId(), course.getMoodleCourseId());
        log.info("Unenrolled student {} from Moodle course {}", student.getUsername(), course.getName());
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
                ? moodleProperties.getPublicUrl() + moodlePath
                : moodleProperties.getPublicUrl() + "/my/";
        return moodleClient.requestSsoLoginUrl(user.getUsername(), wantsUrl);
    }

    /**
     * Build the course URL for a student to access on Moodle.
     */
    public String buildCourseUrl(Course course) {
        if (course.getMoodleCourseId() == null) return null;
        return moodleProperties.getPublicUrl() + "/course/view.php?id=" + course.getMoodleCourseId();
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
     * Uses provisionMoodleUser (no REQUIRES_NEW) to avoid deadlocks.
     * Returns a result map with synced count, errors, and total.
     */
    @Transactional
    public java.util.Map<String, Object> syncAllUsersDetailed() {
        var users = userRepository.findAll();
        int synced = 0;
        int skipped = 0;
        var errors = new java.util.ArrayList<String>();

        // Quick connection pre-check
        try {
            moodleClient.getSiteInfo();
        } catch (Exception e) {
            log.error("[MoodleSync] Cannot connect to Moodle: {}", e.getMessage());
            return java.util.Map.of(
                    "synced", 0,
                    "total", users.size(),
                    "error", "Cannot connect to Moodle: " + e.getMessage()
            );
        }

        for (User u : users) {
            if (u.getMoodleId() != null || u.isGuest()) {
                skipped++;
                continue;
            }
            try {
                provisionMoodleUser(u);
                userRepository.save(u);
                synced++;
                log.info("[MoodleSync] Synced user '{}' → moodleId={}", u.getUsername(), u.getMoodleId());
            } catch (Exception e) {
                String msg = u.getUsername() + ": " + e.getMessage();
                errors.add(msg);
                log.error("[MoodleSync] FAILED to sync user '{}': {}", u.getUsername(), e.getMessage(), e);
            }
        }

        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("synced", synced);
        result.put("skipped", skipped);
        result.put("total", users.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return result;
    }

    /**
     * Simple version for backward compatibility.
     */
    @Transactional
    public int syncAllUsers() {
        var result = syncAllUsersDetailed();
        return (int) result.getOrDefault("synced", 0);
    }

    /**
     * Import users from Moodle into EnglishEdu.
     * Users that already exist (matched by username or email) are linked but not duplicated.
     * Returns a result map with imported count, linked count, and errors.
     */
    @Transactional
    public java.util.Map<String, Object> importUsersFromMoodle() {
        int imported = 0;
        int linked = 0;
        var errors = new java.util.ArrayList<String>();

        JsonNode moodleUsers;
        try {
            moodleUsers = moodleClient.getAllUsers();
        } catch (Exception e) {
            log.error("[MoodleSync] Cannot fetch users from Moodle: {}", e.getMessage());
            return java.util.Map.of("imported", 0, "error", "Cannot fetch Moodle users: " + e.getMessage());
        }

        if (moodleUsers == null || !moodleUsers.isArray()) {
            return java.util.Map.of("imported", 0, "error", "Moodle returned no user data");
        }

        for (JsonNode mu : moodleUsers) {
            long moodleId = mu.path("id").asLong();
            String username = mu.path("username").asText("").trim();
            String email = mu.path("email").asText("").trim();
            String firstName = mu.path("firstname").asText("");
            String lastName = mu.path("lastname").asText("");

            // Skip system users
            if (moodleId <= 1 || username.isEmpty() || "guest".equalsIgnoreCase(username)
                    || username.startsWith("guest_")) {
                continue;
            }

            try {
                // Try to find existing user by username or email
                User existing = userRepository.findByUsername(username).orElse(null);
                if (existing == null && !email.isEmpty()) {
                    existing = userRepository.findByEmail(email).orElse(null);
                }

                if (existing != null) {
                    if (existing.getMoodleId() == null) {
                        existing.setMoodleId(moodleId);
                        userRepository.save(existing);
                        linked++;
                        log.info("[MoodleSync] Linked existing user '{}' → moodleId={}", existing.getUsername(), moodleId);
                    }
                    // Already linked, skip
                } else {
                    // Create new local user for this Moodle user
                    User newUser = User.builder()
                            .username(username)
                            .email(email.isEmpty() ? username + "@moodle.local" : email)
                            .password("$2a$10$MOODLE_IMPORTED_NO_LOCAL_LOGIN") // not a valid bcrypt, can't login locally
                            .firstName(firstName.isEmpty() ? username : firstName)
                            .lastName(lastName.isEmpty() ? "." : lastName)
                            .role("STUDENT")
                            .active(true)
                            .moodleId(moodleId)
                            .build();
                    userRepository.save(newUser);
                    imported++;
                    log.info("[MoodleSync] Imported Moodle user '{}' (moodleId={})", username, moodleId);
                }
            } catch (Exception e) {
                String msg = username + ": " + e.getMessage();
                errors.add(msg);
                log.warn("[MoodleSync] Failed to import Moodle user '{}': {}", username, e.getMessage());
            }
        }

        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("imported", imported);
        result.put("linked", linked);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return result;
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

    /**
     * Import ALL courses from Moodle into the local EnglishEdu database.
     * Courses that already have a matching moodleCourseId are skipped.
     * Returns count of newly imported courses.
     */
    @Transactional
    public int importMoodleCourses() {
        JsonNode allMoodleCourses = moodleClient.getAllCourses();
        if (allMoodleCourses == null || !allMoodleCourses.isArray()) return 0;
        int count = 0;
        for (JsonNode mc : allMoodleCourses) {
            long moodleCourseId = mc.path("id").asLong();
            if (moodleCourseId <= 1) continue; // skip id=0 (error) and id=1 (Moodle "Site" course)
            if (courseRepository.findByMoodleCourseId(moodleCourseId).isPresent()) continue;

            String fullname = mc.path("fullname").asText("").trim();
            if (fullname.isEmpty()) continue;

            Course course = Course.builder()
                    .name(fullname)
                    .description(mc.path("summary").asText(""))
                    .moodleCourseId(moodleCourseId)
                    .published(true)
                    .free(true)
                    .build();
            courseRepository.save(course);
            count++;
            log.info("Imported Moodle course: moodleId={} name='{}'", moodleCourseId, fullname);
        }
        return count;
    }

    /**
     * For all students in the local DB that have a moodleId, query Moodle
     * for their enrolled courses and auto-create missing local enrollment records.
     * Also auto-imports courses from Moodle that don't exist locally.
     * Returns total number of new enrollment records created.
     */
    @Transactional
    public int syncAllEnrollmentsFromMoodle() {
        var users = userRepository.findAll();
        int count = 0;
        for (User user : users) {
            if (user.isGuest() || !"STUDENT".equalsIgnoreCase(user.getRole())) continue;
            // Auto-provision moodleId if missing
            if (user.getMoodleId() == null) {
                try {
                    ensureMoodleUser(user);
                    user = userRepository.findById(user.getId()).orElse(user);
                } catch (Exception e) {
                    log.warn("Could not provision Moodle user for {}: {}", user.getUsername(), e.getMessage());
                    continue;
                }
            }
            if (user.getMoodleId() == null) continue;

            try {
                JsonNode moodleCourses = getMoodleCourses(user);
                if (moodleCourses == null || !moodleCourses.isArray()) continue;
                for (JsonNode mc : moodleCourses) {
                    long moodleCourseId = mc.path("id").asLong();
                    if (moodleCourseId <= 1) continue;

                    Course course = courseRepository.findByMoodleCourseId(moodleCourseId).orElse(null);
                    if (course == null) {
                        String fullname = mc.path("fullname").asText("").trim();
                        if (fullname.isEmpty()) continue;
                        course = Course.builder()
                                .name(fullname)
                                .description(mc.path("summary").asText(""))
                                .moodleCourseId(moodleCourseId)
                                .published(true)
                                .free(true)
                                .build();
                        course = courseRepository.save(course);
                        log.info("Auto-imported Moodle course: id={} name='{}'", moodleCourseId, fullname);
                    }

                    if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                        com.sso.entity.Enrollment e = com.sso.entity.Enrollment.builder()
                                .user(user)
                                .course(course)
                                .status("active")
                                .requestDate(java.time.Instant.now())
                                .approvedAt(java.time.Instant.now())
                                .build();
                        enrollmentRepository.save(e);
                        count++;
                        log.info("Synced Moodle enrollment: user={} course={}", user.getUsername(), course.getName());
                    }
                }
            } catch (Exception e) {
                log.warn("Enrollment sync failed for user {}: {}", user.getUsername(), e.getMessage());
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

    /**
     * Comprehensive Moodle configuration diagnostic.
     * Tests token, connection, available functions, and user create capability.
     * Returns a detailed map of results for admin troubleshooting.
     */
    public java.util.Map<String, Object> diagnosticCheck() {
        var result = new java.util.LinkedHashMap<String, Object>();

        // 1. Check local config
        String moodleUrl = moodleProperties.getUrl();
        String moodleToken = moodleProperties.getToken();
        result.put("moodle_url", moodleUrl != null ? moodleUrl : "(not set)");
        result.put("token_configured", moodleToken != null && !moodleToken.isBlank());
        result.put("service_name", moodleProperties.getServiceName());

        if (moodleToken == null || moodleToken.isBlank()) {
            result.put("status", "FAIL");
            result.put("error", "MOODLE_TOKEN is not set. Set it in your .env or environment variables.");
            return result;
        }
        if (moodleUrl == null || moodleUrl.isBlank()) {
            result.put("status", "FAIL");
            result.put("error", "MOODLE_URL is not set.");
            return result;
        }

        // 2. Test connection via get_site_info
        JsonNode siteInfo;
        try {
            siteInfo = moodleClient.getSiteInfo();
            result.put("connection", "OK");
            result.put("moodle_sitename", siteInfo.path("sitename").asText("?"));
            result.put("moodle_version", siteInfo.path("release").asText("?"));
            result.put("auth_user", siteInfo.path("username").asText("?"));
        } catch (Exception e) {
            result.put("status", "FAIL");
            result.put("connection", "FAILED: " + e.getMessage());
            result.put("hint", "Check MOODLE_URL and MOODLE_TOKEN. Ensure Moodle is running and reachable from the backend container.");
            return result;
        }

        // 3. Check available functions
        JsonNode functions = siteInfo.path("functions");
        var availableFunctions = new java.util.HashSet<String>();
        if (functions.isArray()) {
            for (JsonNode fn : functions) {
                availableFunctions.add(fn.path("name").asText());
            }
        }

        String[] requiredFunctions = {
            "core_user_create_users",
            "core_user_get_users_by_field",
            "core_user_get_users",
            "core_user_update_users",
            "core_webservice_get_site_info",
            "core_course_create_courses",
            "core_course_get_courses_by_field",
            "core_course_get_contents",
            "enrol_manual_enrol_users",
            "core_enrol_get_enrolled_users",
            "core_enrol_get_users_courses"
        };

        var missing = new java.util.ArrayList<String>();
        var present = new java.util.ArrayList<String>();
        for (String fn : requiredFunctions) {
            if (availableFunctions.contains(fn)) {
                present.add(fn);
            } else {
                missing.add(fn);
            }
        }

        result.put("functions_available", present);
        result.put("functions_missing", missing);
        result.put("total_functions_in_service", availableFunctions.size());

        if (!missing.isEmpty()) {
            result.put("status", "FAIL");
            result.put("error", "Missing " + missing.size() + " required function(s) in the Moodle external service. "
                    + "Go to Moodle > Site administration > Server > External services > "
                    + "edit your service and add the missing functions.");
            result.put("hint", "The token's external service must include ALL required functions. "
                    + "Also ensure the service is ENABLED (checkbox) and the authorized user has the necessary capabilities.");
            return result;
        }

        // 4. Test user lookup
        try {
            JsonNode testLookup = moodleClient.getUserByUsername("admin");
            result.put("user_lookup_test", testLookup != null ? "OK (found admin user)" : "OK (admin not found, but function works)");
        } catch (Exception e) {
            result.put("user_lookup_test", "FAILED: " + e.getMessage());
        }

        result.put("status", "OK");
        result.put("message", "All " + requiredFunctions.length + " required functions are available. Moodle integration should work correctly.");
        return result;
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
