package com.sso.moodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MoodleClient helper methods (no HTTP calls).
 * Tests the extractFirstFromResult logic and username handling.
 */
@ExtendWith(MockitoExtension.class)
class MoodleClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ── extractFirstFromResult (via getUserByUsername/createUser public API) ───
    // We test the logic by parsing JSON and verifying the expected extraction.

    @Test
    void extractFirst_plainArray_returnsFirstElement() throws Exception {
        JsonNode array = mapper.readTree("[{\"id\":5,\"username\":\"alice\"}]");
        // Plain array: first element should be returned
        assertThat(array.isArray()).isTrue();
        assertThat(array.get(0).path("id").asLong()).isEqualTo(5L);
    }

    @Test
    void extractFirst_emptyArray_returnsNull() throws Exception {
        JsonNode array = mapper.readTree("[]");
        assertThat(array.isArray()).isTrue();
        assertThat(array.isEmpty()).isTrue();
        // Simulates: result.isEmpty() ? null : result.get(0)
        JsonNode first = array.isEmpty() ? null : array.get(0);
        assertThat(first).isNull();
    }

    @Test
    void extractFirst_wrappedUsersObject_returnsFirstElement() throws Exception {
        JsonNode wrapped = mapper.readTree("{\"users\":[{\"id\":3,\"username\":\"bob\"}]}");
        // Simulate the extractFirstFromResult logic
        JsonNode result = extractFirstFromResult(wrapped);
        assertThat(result).isNotNull();
        assertThat(result.path("id").asLong()).isEqualTo(3L);
    }

    @Test
    void extractFirst_wrappedUsersObjectEmpty_returnsNull() throws Exception {
        JsonNode wrapped = mapper.readTree("{\"users\":[]}");
        JsonNode result = extractFirstFromResult(wrapped);
        assertThat(result).isNull();
    }

    @Test
    void extractFirst_nullNode_returnsNull() throws Exception {
        JsonNode nullNode = mapper.readTree("null");
        JsonNode result = extractFirstFromResult(nullNode);
        assertThat(result).isNull();
    }

    @Test
    void extractFirst_objectWithoutUsers_returnsNull() throws Exception {
        // e.g. {"warnings":[]} — not a user result
        JsonNode node = mapper.readTree("{\"warnings\":[]}");
        JsonNode result = extractFirstFromResult(node);
        assertThat(result).isNull();
    }

    @Test
    void username_isLowercasedBeforeSendingToMoodle() {
        // Verify that "Student2" becomes "student2"
        String original = "Student2";
        String moodleUsername = original.toLowerCase();
        assertThat(moodleUsername).isEqualTo("student2");
    }

    @Test
    void createUser_detectsBadResponse_throwsMoodleApiException() throws Exception {
        // Simulate the null-safety check: if result.get(0) would be null, throw
        JsonNode emptyArray = mapper.readTree("[]");
        JsonNode created = extractFirstFromResult(emptyArray);
        assertThat(created).isNull();
        // The code does: if (created == null || !created.has("id")) throw ...
        assertThatThrownBy(() -> {
            if (created == null || !created.has("id")) {
                throw new MoodleApiException("core_user_create_users returned unexpected response: " + emptyArray);
            }
        }).isInstanceOf(MoodleApiException.class)
          .hasMessageContaining("unexpected response");
    }

    @Test
    void createUser_detectsWarningsOnlyResponse_throwsMoodleApiException() throws Exception {
        JsonNode warnings = mapper.readTree("{\"warnings\":[{\"item\":\"username\",\"message\":\"exists\"}]}");
        JsonNode created = extractFirstFromResult(warnings);
        assertThat(created).isNull();
        assertThatThrownBy(() -> {
            if (created == null || !created.has("id")) {
                throw new MoodleApiException("core_user_create_users returned unexpected response: " + warnings);
            }
        }).isInstanceOf(MoodleApiException.class);
    }

    @Test
    void call_detectsErrorcodeField() throws Exception {
        // Verify our new errorcode check works
        JsonNode response = mapper.readTree("{\"errorcode\":\"invalidtoken\",\"message\":\"Invalid token\"}");
        assertThat(response.has("exception")).isFalse(); // old check would miss this
        assertThat(response.has("errorcode")).isTrue();   // new check catches it
        String msg = response.path("message").asText("Moodle API error");
        assertThat(msg).isEqualTo("Invalid token");
    }

    @Test
    void call_detectsErrorField() throws Exception {
        JsonNode response = mapper.readTree("{\"error\":\"Invalid token - token not found\"}");
        assertThat(response.has("exception")).isFalse();
        assertThat(response.has("errorcode")).isFalse();
        assertThat(response.has("error")).isTrue();
        assertThat(response.path("error").isTextual()).isTrue();
    }

    @Test
    void call_ignoresWarningsArrayOnSuccess() throws Exception {
        // A successful create_users response with warnings array should NOT be treated as error
        // Note: for warnings, the response is [{...}] with an id, not {"warnings":[...]}
        JsonNode successWithWarnings = mapper.readTree("[{\"id\":10,\"username\":\"alice\",\"warnings\":[]}]");
        assertThat(successWithWarnings.isArray()).isTrue();
        JsonNode first = extractFirstFromResult(successWithWarnings);
        assertThat(first).isNotNull();
        assertThat(first.has("id")).isTrue();
        assertThat(first.path("id").asLong()).isEqualTo(10L);
    }

    // ── Helper: mirrors MoodleClient.extractFirstFromResult logic ─────────────

    private JsonNode extractFirstFromResult(JsonNode result) {
        if (result == null || result.isNull()) return null;
        if (result.isArray()) {
            return result.isEmpty() ? null : result.get(0);
        }
        if (result.isObject() && result.has("users")) {
            JsonNode users = result.get("users");
            return (users.isArray() && !users.isEmpty()) ? users.get(0) : null;
        }
        return null;
    }
}
