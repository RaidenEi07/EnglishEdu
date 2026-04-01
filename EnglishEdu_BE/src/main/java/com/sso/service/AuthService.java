package com.sso.service;

import com.sso.dto.request.LoginRequest;
import com.sso.dto.request.ForgotPasswordRequest;
import com.sso.dto.request.RegisterRequest;
import com.sso.dto.request.ResetPasswordRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final MoodleSyncService moodleSyncService;

    /**
     * Step 1: Self-registration – creates a local user and immediately clones the
     * account to Moodle so the user is known to Moodle before any course access.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role("STUDENT")
                .active(true)
                .build();

        User saved = userRepository.save(user);

        // Clone account to Moodle immediately – errors are non-fatal
        try {
            moodleSyncService.ensureMoodleUser(saved);
        } catch (Exception e) {
            log.warn("Moodle user sync on register failed for '{}': {}", saved.getUsername(), e.getMessage());
        }

        String token = tokenProvider.generateToken(saved.getId(), saved.getUsername());
        return AuthResponse.builder().token(token).user(userMapper.toResponse(saved)).build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = tokenProvider.generateToken(user.getId(), user.getUsername());
        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.builder().token(token).user(userResponse).build();
    }

    @Transactional
    public AuthResponse guestLogin() {
        String guestName = "guest_" + UUID.randomUUID().toString().substring(0, 8);

        User guest = new User();
        guest.setUsername(guestName);
        guest.setEmail(guestName + "@guest.local");
        guest.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        guest.setFirstName("Guest");
        guest.setLastName("User");
        guest.setGuest(true);
        guest.setActive(true);
        guest = userRepository.save(guest);

        String token = tokenProvider.generateToken(guest.getId(), guest.getUsername());
        return AuthResponse.builder().token(token).user(userMapper.toResponse(guest)).build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getSearch(), request.getSearch())
                .orElseThrow(() -> new BadRequestException("No account found with that username or email"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void logout(String token) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "true",
                tokenProvider.getExpirationMs(),
                TimeUnit.MILLISECONDS
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
