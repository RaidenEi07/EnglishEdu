package com.sso.service;

import com.sso.dto.request.LoginRequest;
import com.sso.dto.request.ForgotPasswordRequest;
import com.sso.dto.request.RegisterRequest;
import com.sso.dto.response.AuthResponse;
import com.sso.dto.response.UserResponse;
import com.sso.entity.PasswordResetToken;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.mapper.UserMapper;
import com.sso.moodle.MoodleSyncService;
import com.sso.repository.PasswordResetTokenRepository;
import com.sso.repository.UserRepository;
import com.sso.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private EmailService emailService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private MoodleSyncService moodleSyncService;

    @InjectMocks private AuthService authService;

    private User buildUser(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@x.com")
                .password("hashed")
                .role("STUDENT")
                .active(true)
                .build();
    }

    private UserResponse buildResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_throwsWhenUsernameTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@x.com");
        req.setPassword("Secret1");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already taken");
    }

    @Test
    void register_throwsWhenEmailTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@x.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@x.com");
        req.setPassword("Secret1");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void register_success_returnsTokenAndUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret1")).thenReturn("hashed");

        User saved = buildUser(1L, "alice");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(tokenProvider.generateToken(1L, "alice")).thenReturn("jwt-token");
        when(userMapper.toResponse(saved)).thenReturn(buildResponse(saved));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@x.com");
        req.setPassword("Secret1");

        AuthResponse result = authService.register(req);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void register_success_setsMoodleIdOnUser() {
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(userRepository.existsByEmail("carol@x.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret1")).thenReturn("hashed");

        User saved = buildUser(3L, "carol");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(tokenProvider.generateToken(3L, "carol")).thenReturn("jwt-carol");
        when(userMapper.toResponse(any(User.class))).thenReturn(buildResponse(saved));

        // Simulate provisionMoodleUser setting moodleId on the entity
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setMoodleId(42L);
            return 42L;
        }).when(moodleSyncService).provisionMoodleUser(any(User.class));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("carol");
        req.setEmail("carol@x.com");
        req.setPassword("Secret1");

        AuthResponse result = authService.register(req);

        assertThat(result.getToken()).isEqualTo("jwt-carol");
        // Verify moodleId was set and persisted
        assertThat(saved.getMoodleId()).isEqualTo(42L);
        // Verify save was called twice: initial create + after Moodle sync
        verify(userRepository, times(2)).save(any(User.class));
        verify(moodleSyncService).provisionMoodleUser(saved);
    }

    @Test
    void register_moodleSyncFailure_doesNotAbortRegistration() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@x.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User saved = buildUser(2L, "bob");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(tokenProvider.generateToken(2L, "bob")).thenReturn("jwt-bob");
        when(userMapper.toResponse(saved)).thenReturn(buildResponse(saved));
        doThrow(new RuntimeException("Moodle is down")).when(moodleSyncService).provisionMoodleUser(saved);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@x.com");
        req.setPassword("Secret1");

        // Should NOT throw even though Moodle sync failed
        AuthResponse result = authService.register(req);
        assertThat(result.getToken()).isEqualTo("jwt-bob");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_throwsOnBadCredentials() {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("Secret1");

        User user = buildUser(1L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(1L, "alice")).thenReturn("jwt-token");
        when(userMapper.toResponse(user)).thenReturn(buildResponse(user));

        AuthResponse result = authService.login(req);

        assertThat(result.getToken()).isEqualTo("jwt-token");
    }

    // ── guestLogin ────────────────────────────────────────────────────────────

    @Test
    void guestLogin_createsGuestUserAndReturnsToken() {
        User guestUser = User.builder().id(99L).username("guest_abcd1234").email("guest_abcd1234@guest.local")
                .password("hashed").guest(true).active(true).build();
        when(userRepository.save(any(User.class))).thenReturn(guestUser);
        when(tokenProvider.generateToken(eq(99L), any())).thenReturn("guest-token");
        when(userMapper.toResponse(guestUser)).thenReturn(UserResponse.builder().id(99L).build());

        AuthResponse result = authService.guestLogin();

        assertThat(result.getToken()).isEqualTo("guest-token");
        verify(userRepository).save(argThat(u -> u.isGuest() && u.isActive()));
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Test
    void forgotPassword_throwsWhenUserNotFound() {
        when(userRepository.findByUsernameOrEmail("nobody", "nobody")).thenReturn(Optional.empty());

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setSearch("nobody");

        assertThatThrownBy(() -> authService.forgotPassword(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No account found");
    }

    @Test
    void forgotPassword_success_savesTokenAndSendsEmail() {
        User user = buildUser(1L, "alice");
        when(userRepository.findByUsernameOrEmail("alice", "alice")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setSearch("alice");

        authService.forgotPassword(req);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("alice@x.com"), any());
    }
}
