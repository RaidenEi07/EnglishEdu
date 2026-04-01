package com.sso.service;

import com.sso.dto.request.ChangePasswordRequest;
import com.sso.dto.request.CreateAdminUserRequest;
import com.sso.dto.request.UpdateProfileRequest;
import com.sso.dto.response.UserResponse;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.mapper.UserMapper;
import com.sso.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private StorageService storageService;
    @Mock private com.sso.moodle.MoodleSyncService moodleSyncService;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .password("hashed_password")
                .firstName("Test")
                .lastName("User")
                .role("STUDENT")
                .active(true)
                .build();
    }

    private UserResponse buildResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    // ── getProfile ────────────────────────────────────────

    @Test
    void getProfile_returnsUserResponse() {
        User user = buildUser(1L);
        UserResponse expected = buildResponse(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        UserResponse result = userService.getProfile(1L);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getProfile_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    // ── updateProfile ────────────────────────────────────────

    @Test
    void updateProfile_updatesFirstAndLastName() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(buildResponse(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("John");
        req.setLastName("Doe");

        userService.updateProfile(1L, req);

        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    void updateProfile_throwsWhenEmailAlreadyTaken() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("other@example.com")).thenReturn(true);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("other@example.com");

        assertThatThrownBy(() -> userService.updateProfile(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void updateProfile_allowsSameEmailForSameUser() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(buildResponse(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("test@example.com"); // same as current email

        assertThatNoException().isThrownBy(() -> userService.updateProfile(1L, req));
    }

    // ── changePassword ────────────────────────────────────────

    @Test
    void changePassword_success() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current123", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("new_hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("current123");
        req.setNewPassword("newpass123");

        userService.changePassword(1L, req);

        assertThat(user.getPassword()).isEqualTo("new_hashed");
    }

    @Test
    void changePassword_throwsWhenCurrentPasswordWrong() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed_password")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrongpass");
        req.setNewPassword("newpass123");

        assertThatThrownBy(() -> userService.changePassword(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");
    }

    // ── createUser (admin) ────────────────────────────────────────

    @Test
    void createUser_throwsWhenUsernameExists() {
        when(userRepository.existsByUsername("duplicate")).thenReturn(true);

        CreateAdminUserRequest req = new CreateAdminUserRequest();
        req.setUsername("duplicate");
        req.setEmail("x@x.com");
        req.setPassword("pass1234");

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already taken");
    }

    @Test
    void createUser_throwsWhenEmailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);

        CreateAdminUserRequest req = new CreateAdminUserRequest();
        req.setUsername("newuser");
        req.setEmail("dup@x.com");
        req.setPassword("pass1234");

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void createUser_success_withDefaultStudentRole() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed");

        User savedUser = buildUser(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(buildResponse(savedUser));

        CreateAdminUserRequest req = new CreateAdminUserRequest();
        req.setUsername("newuser");
        req.setEmail("new@x.com");
        req.setPassword("pass1234");
        // role is null → defaults to STUDENT

        UserResponse result = userService.createUser(req);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("pass1234");
    }

    // ── toggleUserActive ────────────────────────────────────────

    @Test
    void toggleUserActive_deactivatesActiveUser() {
        User user = buildUser(1L);
        user.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(buildResponse(user));

        userService.toggleUserActive(1L);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void toggleUserActive_activatesInactiveUser() {
        User user = buildUser(2L);
        user.setActive(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(buildResponse(user));

        userService.toggleUserActive(2L);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void toggleUserActive_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.toggleUserActive(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
