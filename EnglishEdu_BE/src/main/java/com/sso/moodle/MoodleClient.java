package com.sso.moodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Low-level REST client for Moodle Web Services API.
 * Uses JDK {@link HttpClient} (not Spring RestClient) for the core {@code call()}
 * method to guarantee the exact form body reaches Moodle without any framework
 * interference — identical to a plain {@code curl -d ...} invocation.
 * <p>
 * Spring {@link RestClient} is kept only for binary file download/upload operations.
 */
@Slf4j
@Component
public class MoodleClient {

    private final MoodleProperties props;
    private final HttpClient httpClient;
    private final RestClient restClient;      // kept for file download/upload only
    private final ObjectMapper objectMapper;

    public MoodleClient(MoodleProperties props) {
        this.props = props;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.restClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .build();
    }

    /**
     * Call a Moodle web service function and return the parsed JSON tree.
     * <p>
     * Uses JDK {@link HttpClient} directly so that the form-encoded body
     * is byte-identical to what {@code curl -X POST -d "wstoken=…&…"} sends.
     * This avoids any Spring {@code HttpMessageConverter} quirks that can
     * silently corrupt or drop the body on Spring Boot 4 / Framework 7.
     */
    public JsonNode call(String wsFunction, Map<String, String> params) {
        // Build form body exactly like curl
        Map<String, String> allParams = new LinkedHashMap<>();
        allParams.put("wstoken", props.getToken());
        allParams.put("wsfunction", wsFunction);
        allParams.put("moodlewsrestformat", "json");
        allParams.putAll(params);

        String formBody = allParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        String endpoint = props.getUrl() + "/webservice/rest/server.php";

        log.info("Moodle API → POST func={} params={}", wsFunction, params.keySet());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> httpResponse =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            String responseBody = httpResponse.body();
            log.debug("Moodle API ← status={} body_len={}", httpResponse.statusCode(),
                    responseBody != null ? responseBody.length() : 0);

            JsonNode node = objectMapper.readTree(responseBody);
            if (node.has("exception")) {
                String msg = node.path("message").asText("Moodle API error");
                log.error("Moodle API error: func={} errorcode={} msg={} debuginfo={}",
                        wsFunction, node.path("errorcode").asText(), msg,
                        node.path("debuginfo").asText(""));
                throw new MoodleApiException(msg);
            }
            return node;
        } catch (MoodleApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Moodle call failed for func={}: {}", wsFunction, e.getMessage());
            throw new MoodleApiException("Moodle call failed: " + e.getMessage());
        }
    }

    /* ── High-level helpers ─────────────────────────────────── */

    public JsonNode getSiteInfo() {
        return call("core_webservice_get_site_info", Map.of());
    }

    /**
     * Create a user on Moodle. Returns the moodle user id.
     */
    public long createUser(String username, String password, String email,
                           String firstName, String lastName) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("users[0][username]", username);
        params.put("users[0][password]", password);
        params.put("users[0][email]", email);
        params.put("users[0][firstname]", firstName != null ? firstName : username);
        params.put("users[0][lastname]", lastName != null ? lastName : ".");
        params.put("users[0][auth]", "manual");

        JsonNode result = call("core_user_create_users", params);
        return result.get(0).path("id").asLong();
    }

    /**
     * Get Moodle user by username. Returns null if not found.
     */
    public JsonNode getUserByUsername(String username) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("criteria[0][key]", "username");
        params.put("criteria[0][value]", username);
        JsonNode result = call("core_user_get_users", params);
        JsonNode users = result.path("users");
        return users.isArray() && !users.isEmpty() ? users.get(0) : null;
    }

    /**
     * Create a course on Moodle. Returns the moodle course id.
     */
    public long createCourse(String fullname, String shortname, String categoryId,
                             String summary) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("courses[0][fullname]", fullname);
        params.put("courses[0][shortname]", shortname);
        params.put("courses[0][categoryid]", categoryId != null ? categoryId : "1");
        if (summary != null) params.put("courses[0][summary]", summary);
        params.put("courses[0][format]", "topics");
        params.put("courses[0][visible]", "1");

        JsonNode result = call("core_course_create_courses", params);
        return result.get(0).path("id").asLong();
    }

    /**
     * Enrol a user into a course using the manual enrolment plugin.
     * roleid: 5 = student, 3 = teacher (editingteacher)
     */
    public void enrolUser(long moodleUserId, long moodleCourseId, int roleId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("enrolments[0][roleid]", String.valueOf(roleId));
        params.put("enrolments[0][userid]", String.valueOf(moodleUserId));
        params.put("enrolments[0][courseid]", String.valueOf(moodleCourseId));
        call("enrol_manual_enrol_users", params);
    }

    /**
     * Get enrolled users for a course.
     */
    public JsonNode getEnrolledUsers(long moodleCourseId) {
        return call("core_enrol_get_enrolled_users", Map.of(
                "courseid", String.valueOf(moodleCourseId)
        ));
    }

    /**
     * Get courses from Moodle by field (e.g. "ids", "shortname").
     * Returns JSON with "courses" array, each containing overviewfiles for image sync.
     */
    public JsonNode getCoursesByField(String field, String value) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("field", field);
        params.put("value", value);
        return call("core_course_get_courses_by_field", params);
    }

    /**
     * Get course contents (sections, activities, resources).
     */
    public JsonNode getCourseContents(long moodleCourseId) {
        return call("core_course_get_contents", Map.of(
                "courseid", String.valueOf(moodleCourseId)
        ));
    }

    /**
     * Get a user's grades for a course.
     */
    public JsonNode getUserGrades(long moodleCourseId, long moodleUserId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("courseid", String.valueOf(moodleCourseId));
        params.put("userid", String.valueOf(moodleUserId));
        return call("gradereport_user_get_grade_items", params);
    }

    /**
     * Get calendar events for a user within a time range.
     * Uses core_calendar_get_calendar_events (requires moodle user id).
     */
    public JsonNode getCalendarEvents(long moodleUserId, long timeStart, long timeEnd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("options[userevents]", "1");
        params.put("options[siteevents]", "1");
        params.put("options[timestart]", String.valueOf(timeStart));
        params.put("options[timeend]", String.valueOf(timeEnd));
        params.put("options[ignorehidden]", "1");
        return call("core_calendar_get_calendar_events", params);
    }

    /**
     * Get upcoming action events sorted by time.
     * Uses core_calendar_get_action_events_by_timesort.
     */
    public JsonNode getActionEvents(long timeSortFrom, long timeSortTo, int limitNum) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("timesortfrom", String.valueOf(timeSortFrom));
        params.put("timesortto", String.valueOf(timeSortTo));
        params.put("limitnum", String.valueOf(limitNum));
        return call("core_calendar_get_action_events_by_timesort", params);
    }

    /**
     * Get all courses a user is enrolled in on Moodle.
     */
    public JsonNode getUserMoodleCourses(long moodleUserId) {
        return call("core_enrol_get_users_courses", Map.of(
                "userid", String.valueOf(moodleUserId)
        ));
    }

    /**
     * Request a one-time SSO login URL via Moodle's auth_userkey plugin.
     * The returned URL auto-logs the user in when visited in a browser.
     * After login, Moodle redirects to {@code wantsUrl} (if provided).
     * <p>
     * Falls back to the bare {@code wantsUrl} (or Moodle homepage) when the
     * auth_userkey plugin is not installed or the call fails.
     *
     * @param username  Moodle username of the target user
     * @param wantsUrl  full URL to redirect to after login (e.g. course page); may be null
     * @return auto-login URL ready for browser redirect
     */
    public String requestSsoLoginUrl(String username, String wantsUrl) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("user[username]", username);
            JsonNode result = call("auth_userkey_request_login_url", params);
            String loginUrl = result.path("loginurl").asText(null);
            if (loginUrl != null && !loginUrl.isBlank()) {
                // Append wantsurl so Moodle redirects to the course after login
                if (wantsUrl != null && !wantsUrl.isBlank()) {
                    loginUrl += "&wantsurl=" + encode(wantsUrl);
                }
                log.info("SSO URL generated for user {}", username);
                return loginUrl;
            }
        } catch (Exception e) {
            log.warn("auth_userkey SSO failed for user '{}' (plugin installed?): {}", username, e.getMessage());
        }
        // Fallback: direct URL — user must log in manually on Moodle
        return wantsUrl != null ? wantsUrl : props.getUrl() + "/my/";
    }

    /* ── Assignment functions ──────────────────────────────── */

    /**
     * Get assignments for given course IDs.
     * Returns { courses: [ { id, assignments: [...] } ] }
     */
    public JsonNode getAssignments(List<Long> courseIds) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            params.put("courseids[" + i + "]", String.valueOf(courseIds.get(i)));
        }
        return call("mod_assign_get_assignments", params);
    }

    /**
     * Get submission status for a specific assignment + user.
     */
    public JsonNode getSubmissionStatus(long assignId, long moodleUserId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("assignid", String.valueOf(assignId));
        params.put("userid", String.valueOf(moodleUserId));
        return call("mod_assign_get_submission_status", params);
    }

    /**
     * Save an online-text submission for an assignment.
     */
    public JsonNode saveTextSubmission(long assignId, String text, int itemId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("assignmentid", String.valueOf(assignId));
        params.put("plugindata[onlinetext_editor][text]", text);
        params.put("plugindata[onlinetext_editor][format]", "1"); // HTML
        params.put("plugindata[onlinetext_editor][itemid]", String.valueOf(itemId));
        return call("mod_assign_save_submission", params);
    }

    /**
     * Save a file submission for an assignment (using itemId from file upload).
     */
    public JsonNode saveFileSubmission(long assignId, int fileItemId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("assignmentid", String.valueOf(assignId));
        params.put("plugindata[files_filemanager]", String.valueOf(fileItemId));
        return call("mod_assign_save_submission", params);
    }

    /**
     * Submit assignment for grading.
     */
    public JsonNode submitForGrading(long assignId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("assignmentid", String.valueOf(assignId));
        params.put("acceptsubmissionstatement", "1");
        return call("mod_assign_submit_for_grading", params);
    }

    /* ── Quiz functions ────────────────────────────────────── */

    /**
     * Get quizzes for given course IDs.
     */
    public JsonNode getQuizzesByCourses(List<Long> courseIds) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            params.put("courseids[" + i + "]", String.valueOf(courseIds.get(i)));
        }
        return call("mod_quiz_get_quizzes_by_courses", params);
    }

    /**
     * Get user attempts for a quiz.
     */
    public JsonNode getQuizUserAttempts(long quizId, long moodleUserId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("quizid", String.valueOf(quizId));
        params.put("userid", String.valueOf(moodleUserId));
        params.put("status", "all");
        params.put("includepreviews", "0");
        return call("mod_quiz_get_user_quiz_attempts", params);
    }

    /**
     * Start a new quiz attempt.
     */
    public JsonNode startQuizAttempt(long quizId) {
        return call("mod_quiz_start_attempt", Map.of(
                "quizid", String.valueOf(quizId)
        ));
    }

    /**
     * Get attempt data (questions) for a specific page.
     */
    public JsonNode getAttemptData(long attemptId, int page) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("attemptid", String.valueOf(attemptId));
        params.put("page", String.valueOf(page));
        return call("mod_quiz_get_attempt_data", params);
    }

    /**
     * Save answers for a quiz attempt (without finishing).
     */
    public JsonNode saveAttempt(long attemptId, Map<String, String> answers) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("attemptid", String.valueOf(attemptId));
        int i = 0;
        for (var entry : answers.entrySet()) {
            params.put("data[" + i + "][name]", entry.getKey());
            params.put("data[" + i + "][value]", entry.getValue());
            i++;
        }
        return call("mod_quiz_save_attempt", params);
    }

    /**
     * Finish/submit a quiz attempt.
     */
    public JsonNode processAttempt(long attemptId, Map<String, String> answers) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("attemptid", String.valueOf(attemptId));
        params.put("finishattempt", "1");
        int i = 0;
        for (var entry : answers.entrySet()) {
            params.put("data[" + i + "][name]", entry.getKey());
            params.put("data[" + i + "][value]", entry.getValue());
            i++;
        }
        return call("mod_quiz_process_attempt", params);
    }

    /**
     * Review a finished quiz attempt (see correct answers, feedback).
     */
    public JsonNode getAttemptReview(long attemptId) {
        return call("mod_quiz_get_attempt_review", Map.of(
                "attemptid", String.valueOf(attemptId)
        ));
    }

    /**
     * Get summary of all questions in an attempt.
     */
    public JsonNode getAttemptSummary(long attemptId) {
        return call("mod_quiz_get_attempt_summary", Map.of(
                "attemptid", String.valueOf(attemptId)
        ));
    }

    /* ── Module content functions ──────────────────────────── */

    /**
     * Get page module contents for courses.
     */
    public JsonNode getPagesByCourses(List<Long> courseIds) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            params.put("courseids[" + i + "]", String.valueOf(courseIds.get(i)));
        }
        return call("mod_page_get_pages_by_courses", params);
    }

    /**
     * Get resource module contents for courses.
     */
    public JsonNode getResourcesByCourses(List<Long> courseIds) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            params.put("courseids[" + i + "]", String.valueOf(courseIds.get(i)));
        }
        return call("mod_resource_get_resources_by_courses", params);
    }

    /**
     * Get URL module contents for courses.
     */
    public JsonNode getUrlsByCourses(List<Long> courseIds) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            params.put("courseids[" + i + "]", String.valueOf(courseIds.get(i)));
        }
        return call("mod_url_get_urls_by_courses", params);
    }

    /* ── Completion functions ──────────────────────────────── */

    /**
     * Get activity completion status for all activities in a course.
     */
    public JsonNode getActivitiesCompletionStatus(long courseId, long moodleUserId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("courseid", String.valueOf(courseId));
        params.put("userid", String.valueOf(moodleUserId));
        return call("core_completion_get_activities_completion_status", params);
    }

    /**
     * Manually mark an activity as complete/incomplete.
     */
    public JsonNode updateActivityCompletion(long cmId, boolean completed) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cmid", String.valueOf(cmId));
        params.put("completed", completed ? "1" : "0");
        return call("core_completion_update_activity_completion_status_manually", params);
    }

    /* ── File handling ─────────────────────────────────────── */

    /**
     * Download a file from Moodle by proxying the pluginfile URL.
     * Returns the raw bytes.
     */
    public byte[] downloadMoodleFile(String fileUrl) {
        // Ensure we're using the webservice pluginfile endpoint with our token
        String url = fileUrl;
        if (url.contains("/pluginfile.php/")) {
            url = url.replace("/pluginfile.php/", "/webservice/pluginfile.php/");
        }
        if (!url.contains("token=")) {
            url += (url.contains("?") ? "&" : "?") + "token=" + props.getToken();
        }

        return restClient.get()
                .uri(url.replace(props.getUrl(), ""))
                .retrieve()
                .body(byte[].class);
    }

    /**
     * Upload a file to Moodle's draft area. Returns the itemId.
     */
    public long uploadFile(byte[] fileData, String fileName) {
        // Moodle file upload is done via /webservice/upload.php
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String crlf = "\r\n";

        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append(crlf);
        sb.append("Content-Disposition: form-data; name=\"token\"").append(crlf).append(crlf);
        sb.append(props.getToken()).append(crlf);
        sb.append("--").append(boundary).append(crlf);
        sb.append("Content-Disposition: form-data; name=\"filearea\"").append(crlf).append(crlf);
        sb.append("draft").append(crlf);
        sb.append("--").append(boundary).append(crlf);
        sb.append("Content-Disposition: form-data; name=\"itemid\"").append(crlf).append(crlf);
        sb.append("0").append(crlf);

        byte[] preamble = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileHeader = "--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + crlf
                + "Content-Type: application/octet-stream" + crlf + crlf;
        String epilogue = crlf + "--" + boundary + "--" + crlf;

        byte[] headerBytes = fileHeader.getBytes(StandardCharsets.UTF_8);
        byte[] epilogueBytes = epilogue.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[preamble.length + headerBytes.length + fileData.length + epilogueBytes.length];
        System.arraycopy(preamble, 0, body, 0, preamble.length);
        System.arraycopy(headerBytes, 0, body, preamble.length, headerBytes.length);
        System.arraycopy(fileData, 0, body, preamble.length + headerBytes.length, fileData.length);
        System.arraycopy(epilogueBytes, 0, body, preamble.length + headerBytes.length + fileData.length, epilogueBytes.length);

        String response = restClient.post()
                .uri("/webservice/upload.php")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(response);
            if (node.isArray() && !node.isEmpty()) {
                return node.get(0).path("itemid").asLong();
            }
            throw new MoodleApiException("File upload failed: unexpected response");
        } catch (MoodleApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MoodleApiException("File upload failed: " + e.getMessage());
        }
    }

    /**
     * Call a Moodle web service function using a specific user's token
     * (instead of the admin token). Used for operations that must execute
     * in the context of a particular user, e.g. quiz attempts.
     */
    public JsonNode callAsUser(String wsFunction, Map<String, String> params, String userToken) {
        Map<String, String> allParams = new LinkedHashMap<>();
        allParams.put("wstoken", userToken);
        allParams.put("wsfunction", wsFunction);
        allParams.put("moodlewsrestformat", "json");
        allParams.putAll(params);

        String formBody = allParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        String endpoint = props.getUrl() + "/webservice/rest/server.php";
        log.info("Moodle API (user-token) → POST func={} params={}", wsFunction, params.keySet());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> httpResponse =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            String responseBody = httpResponse.body();
            log.debug("Moodle API ← status={} body_len={}", httpResponse.statusCode(),
                    responseBody != null ? responseBody.length() : 0);

            JsonNode node = objectMapper.readTree(responseBody);
            if (node.has("exception")) {
                String msg = node.path("message").asText("Moodle API error");
                log.error("Moodle API error (user-token): func={} errorcode={} msg={}",
                        wsFunction, node.path("errorcode").asText(), msg);
                throw new MoodleApiException(msg);
            }
            return node;
        } catch (MoodleApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Moodle callAsUser failed for func={}: {}", wsFunction, e.getMessage());
            throw new MoodleApiException("Moodle call failed: " + e.getMessage());
        }
    }

    /**
     * Request a web-service token for a specific Moodle user via /login/token.php.
     * Requires the user's password and the service short name.
     *
     * @return the token string
     */
    public String requestUserToken(String username, String password, String serviceName) {
        String endpoint = props.getUrl() + "/login/token.php";
        String formBody = "username=" + encode(username)
                + "&password=" + encode(password)
                + "&service=" + encode(serviceName);

        log.info("Requesting Moodle user token for username={} service={}", username, serviceName);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> httpResponse =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            JsonNode node = objectMapper.readTree(httpResponse.body());
            if (node.has("token")) {
                String token = node.path("token").asText();
                log.info("Obtained Moodle token for user {}", username);
                return token;
            }
            String error = node.path("error").asText("Unknown error");
            throw new MoodleApiException("Cannot get user token: " + error);
        } catch (MoodleApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MoodleApiException("Token request failed: " + e.getMessage());
        }
    }

    /**
     * Reset a Moodle user's password using the admin token (core_user_update_users).
     */
    public void updateUserPassword(long moodleUserId, String newPassword) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("users[0][id]", String.valueOf(moodleUserId));
        params.put("users[0][password]", newPassword);
        call("core_user_update_users", params);
        log.info("Reset Moodle password for moodleUserId={}", moodleUserId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
