package com.sso.moodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sso.entity.User;
import com.sso.repository.CourseRepository;
import com.sso.repository.EnrollmentRepository;
import com.sso.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MoodleSyncServiceTest {

    @Mock private MoodleClient moodleClient;
    @Mock private MoodleProperties moodleProperties;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    private MoodleSyncService moodleSyncService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        moodleSyncService = new MoodleSyncService(
                moodleClient, moodleProperties, userRepository,
                courseRepository, enrollmentRepository);
        // Default: Moodle is configured
        when(moodleProperties.getToken()).thenReturn("valid-admin-token");
        when(moodleProperties.getUrl()).thenReturn("http://moodle.local");
        when(moodleProperties.getServiceName()).thenReturn("moodle_mobile_app");
    }

    // ── provisionMoodleUser — skip if already provisioned ────────────────────

    @Test
    void provisionMoodleUser_alreadyHasMoodleId_returnsEarlyWithoutApiCalls() {
        User user = buildUser(1L, "alice", "alice@x.com");
        user.setMoodleId(42L);

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(42L);
        verifyNoInteractions(moodleClient);
    }

    // ── provisionMoodleUser — found by username on Moodle ────────────────────

    @Test
    void provisionMoodleUser_foundByUsername_setsMoodleId() throws Exception {
        User user = buildUser(1L, "alice", "alice@x.com");
        JsonNode moodleUser = mapper.readTree("{\"id\":100,\"username\":\"alice\"}");
        when(moodleClient.getUserByUsername("alice")).thenReturn(moodleUser);

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(100L);
        assertThat(user.getMoodleId()).isEqualTo(100L);
        verify(moodleClient).getUserByUsername("alice");
        verify(moodleClient, never()).getUserByEmail(any());
        verify(moodleClient, never()).createUser(any(), any(), any(), any(), any());
    }

    // ── provisionMoodleUser — username lookup fails, found by email ──────────

    @Test
    void provisionMoodleUser_usernameNotFound_foundByEmail_setsMoodleId() throws Exception {
        User user = buildUser(1L, "alice", "alice@x.com");
        JsonNode moodleUser = mapper.readTree("{\"id\":101,\"username\":\"alice\",\"email\":\"alice@x.com\"}");
        when(moodleClient.getUserByUsername("alice")).thenReturn(null);
        when(moodleClient.getUserByEmail("alice@x.com")).thenReturn(moodleUser);

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(101L);
        assertThat(user.getMoodleId()).isEqualTo(101L);
        verify(moodleClient).getUserByEmail("alice@x.com");
        verify(moodleClient, never()).createUser(any(), any(), any(), any(), any());
    }

    @Test
    void provisionMoodleUser_usernameThrows_foundByEmail_setsMoodleId() throws Exception {
        User user = buildUser(1L, "alice", "alice@x.com");
        JsonNode moodleUser = mapper.readTree("{\"id\":102,\"username\":\"alice\",\"email\":\"alice@x.com\"}");
        when(moodleClient.getUserByUsername("alice"))
                .thenThrow(new MoodleApiException("connection refused"));
        when(moodleClient.getUserByEmail("alice@x.com")).thenReturn(moodleUser);

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(102L);
        assertThat(user.getMoodleId()).isEqualTo(102L);
    }

    // ── provisionMoodleUser — new user (not found anywhere) ──────────────────

    @Test
    void provisionMoodleUser_notFound_createsUser_setsMoodleId() throws Exception {
        User user = buildUser(1L, "alice", "alice@x.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");

        when(moodleClient.getUserByUsername("alice")).thenReturn(null);
        when(moodleClient.getUserByEmail("alice@x.com")).thenReturn(null);
        when(moodleClient.createUser(eq("alice"), any(), eq("alice@x.com"), eq("Alice"), eq("Smith")))
                .thenReturn(200L);
        when(moodleClient.requestUserToken(eq("alice"), any(), eq("moodle_mobile_app")))
                .thenReturn("user-token-abc");

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(200L);
        assertThat(user.getMoodleId()).isEqualTo(200L);
        assertThat(user.getMoodleToken()).isEqualTo("user-token-abc");
    }

    @Test
    void provisionMoodleUser_notFound_createsUser_tokenFailsNonFatally() throws Exception {
        User user = buildUser(1L, "alice", "alice@x.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");

        when(moodleClient.getUserByUsername("alice")).thenReturn(null);
        when(moodleClient.getUserByEmail("alice@x.com")).thenReturn(null);
        when(moodleClient.createUser(any(), any(), any(), any(), any())).thenReturn(203L);
        when(moodleClient.requestUserToken(any(), any(), any()))
                .thenThrow(new MoodleApiException("token service unavailable"));

        // Should not throw — token failure is non-fatal
        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(203L);
        assertThat(user.getMoodleId()).isEqualTo(203L);
        assertThat(user.getMoodleToken()).isNull(); // token not set
    }

    // ── provisionMoodleUser — createUser fails, retry finds user ─────────────

    @Test
    void provisionMoodleUser_createFails_retryFindsUser_setsMoodleId() throws Exception {
        User user = buildUser(1L, "bob", "bob@x.com");
        JsonNode retryUser = mapper.readTree("{\"id\":300,\"username\":\"bob\"}");

        when(moodleClient.getUserByUsername("bob")).thenReturn(null);
        when(moodleClient.getUserByEmail("bob@x.com")).thenReturn(null);
        when(moodleClient.createUser(any(), any(), any(), any(), any()))
                .thenThrow(new MoodleApiException("Username already exists"));
        // Retry lookup finds the user
        when(moodleClient.getUserByUsername("bob")).thenReturn(null).thenReturn(retryUser);
        // First call returns null (initial lookup), second call returns retryUser
        // but Mockito needs careful setup for consecutive calls:
        when(moodleClient.getUserByUsername("bob"))
                .thenReturn(null)          // first call: initial lookup
                .thenReturn(retryUser);    // second call: after createUser fails

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(300L);
        assertThat(user.getMoodleId()).isEqualTo(300L);
    }

    @Test
    void provisionMoodleUser_createFails_retryByEmail_setsMoodleId() throws Exception {
        User user = buildUser(1L, "bob", "bob@x.com");
        JsonNode retryUser = mapper.readTree("{\"id\":301,\"username\":\"bob\"}");

        when(moodleClient.getUserByUsername("bob")).thenReturn(null); // initial + retry both fail
        when(moodleClient.getUserByEmail("bob@x.com"))
                .thenReturn(null)          // initial lookup
                .thenReturn(retryUser);    // after createUser fails, retry by email finds it
        when(moodleClient.createUser(any(), any(), any(), any(), any()))
                .thenThrow(new MoodleApiException("Email already exists"));

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(301L);
        assertThat(user.getMoodleId()).isEqualTo(301L);
    }

    // ── provisionMoodleUser — all paths fail → throws ─────────────────────────

    @Test
    void provisionMoodleUser_allPathsFail_throwsMoodleApiException() {
        User user = buildUser(1L, "charlie", "charlie@x.com");

        when(moodleClient.getUserByUsername("charlie")).thenReturn(null);
        when(moodleClient.getUserByEmail("charlie@x.com")).thenReturn(null);
        when(moodleClient.createUser(any(), any(), any(), any(), any()))
                .thenThrow(new MoodleApiException("Moodle server error"));

        assertThatThrownBy(() -> moodleSyncService.provisionMoodleUser(user))
                .isInstanceOf(MoodleApiException.class)
                .hasMessageContaining("Moodle server error");
    }

    // ── provisionMoodleUser — username case sensitivity ───────────────────────

    @Test
    void provisionMoodleUser_mixedCaseUsername_createUserCalledWithOriginalUsername() throws Exception {
        // MoodleSyncService passes user.getUsername() as-is to MoodleClient.
        // MoodleClient.createUser() is responsible for lowercasing before sending to Moodle.
        // This test verifies MoodleSyncService calls createUser with the user's original username.
        User user = buildUser(1L, "Student2", "student2@x.com");
        user.setFirstName("Student");
        user.setLastName("Two");

        when(moodleClient.getUserByUsername("Student2")).thenReturn(null);
        when(moodleClient.getUserByEmail("student2@x.com")).thenReturn(null);
        when(moodleClient.createUser(eq("Student2"), any(), eq("student2@x.com"), any(), any()))
                .thenReturn(400L);
        when(moodleClient.requestUserToken(any(), any(), any())).thenReturn("token");

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(400L);
        assertThat(user.getMoodleId()).isEqualTo(400L);
        // MoodleSyncService sends original username; MoodleClient (real impl) lowercases it
        verify(moodleClient).createUser(eq("Student2"), any(), eq("student2@x.com"), any(), any());
    }

    // ── provisionMoodleUser — edge: user with null email ─────────────────────

    @Test
    void provisionMoodleUser_nullEmail_skipsEmailLookup() throws Exception {
        User user = buildUser(1L, "nomail", null);

        when(moodleClient.getUserByUsername("nomail")).thenReturn(null);
        // No getUserByEmail call expected for null email
        when(moodleClient.createUser(any(), any(), isNull(), any(), any())).thenReturn(500L);

        long result = moodleSyncService.provisionMoodleUser(user);

        assertThat(result).isEqualTo(500L);
        verify(moodleClient, never()).getUserByEmail(any());
    }

    // ── syncAllUsersDetailed ──────────────────────────────────────────────────

    @Test
    void syncAllUsersDetailed_connectionFailure_returnsErrorMap() {
        when(userRepository.findAll()).thenReturn(java.util.List.of(buildUser(1L, "alice", "alice@x.com")));
        when(moodleClient.getSiteInfo()).thenThrow(new MoodleApiException("connection refused"));

        var result = moodleSyncService.syncAllUsersDetailed();

        assertThat(result).containsKey("error");
        assertThat(result.get("synced")).isEqualTo(0);
    }

    @Test
    void syncAllUsersDetailed_skipsUsersWithExistingMoodleId() throws Exception {
        User alreadySynced = buildUser(1L, "alice", "alice@x.com");
        alreadySynced.setMoodleId(99L);
        User guestUser = buildUser(2L, "guest_abc", "guest@guest.local");
        guestUser.setGuest(true);

        when(userRepository.findAll()).thenReturn(java.util.List.of(alreadySynced, guestUser));
        when(moodleClient.getSiteInfo()).thenReturn(mapper.readTree("{\"sitename\":\"Moodle\"}"));

        var result = moodleSyncService.syncAllUsersDetailed();

        assertThat(result.get("synced")).isEqualTo(0);
        assertThat(result.get("skipped")).isEqualTo(2);
        assertThat(result.get("total")).isEqualTo(2);
        verify(moodleClient, never()).getUserByUsername(any());
    }

    @Test
    void syncAllUsersDetailed_syncsBothSuccessAndFailure_reportsCorrectly() throws Exception {
        User alice = buildUser(1L, "alice", "alice@x.com");
        User bob = buildUser(2L, "bob", "bob@x.com");
        JsonNode aliceMoodle = mapper.readTree("{\"id\":10,\"username\":\"alice\"}");

        when(userRepository.findAll()).thenReturn(java.util.List.of(alice, bob));
        when(moodleClient.getSiteInfo()).thenReturn(mapper.readTree("{\"sitename\":\"Moodle\"}"));
        when(moodleClient.getUserByUsername("alice")).thenReturn(aliceMoodle);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moodleClient.getUserByUsername("bob")).thenReturn(null);
        when(moodleClient.getUserByEmail("bob@x.com")).thenReturn(null);
        when(moodleClient.createUser(eq("bob"), any(), any(), any(), any()))
                .thenThrow(new MoodleApiException("user creation failed"));

        var result = moodleSyncService.syncAllUsersDetailed();

        assertThat(result.get("synced")).isEqualTo(1);
        assertThat(result.get("total")).isEqualTo(2);
        assertThat(result).containsKey("errors");
        @SuppressWarnings("unchecked")
        var errors = (java.util.List<String>) result.get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("bob");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("hashed")
                .active(true)
                .guest(false)
                .build();
    }
}
