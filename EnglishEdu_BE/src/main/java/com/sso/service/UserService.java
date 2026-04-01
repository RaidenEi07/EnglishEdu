package com.sso.service;

import com.sso.dto.request.ChangePasswordRequest;
import com.sso.dto.request.CreateAdminUserRequest;
import com.sso.dto.request.UpdateProfileRequest;
import com.sso.dto.request.UpdateUserRoleRequest;
import com.sso.dto.response.UserResponse;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.mapper.UserMapper;
import com.sso.repository.UserRepository;
import com.sso.moodle.MoodleSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final StorageService storageService;
    private final MoodleSyncService moodleSyncService;

    public UserResponse getProfile(Long userId) {
        User user = findUser(userId);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail()) &&
                    !user.getEmail().equals(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) throws Exception {
        User user = findUser(userId);
        String url = storageService.uploadFile(file, "avatars");
        user.setAvatarUrl(url);
        return userMapper.toResponse(userRepository.save(user));
    }

    /* ── Admin ───────────────────────────────────────── */

    public Page<UserResponse> getUsersAdmin(String role, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        boolean hasRole    = role != null && !role.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasRole && hasKeyword) {
            return userRepository.findByRoleAndKeyword(role.toUpperCase(), keyword.toLowerCase(), pageable).map(userMapper::toResponse);
        }
        if (hasRole) {
            return userRepository.findByRole(role.toUpperCase(), pageable).map(userMapper::toResponse);
        }
        if (hasKeyword) {
            return userRepository.findByKeyword(keyword.toLowerCase(), pageable).map(userMapper::toResponse);
        }
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse createUser(CreateAdminUserRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already in use");
        }
        String role = req.getRole() != null ? req.getRole().toUpperCase() : "STUDENT";
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .role(role)
                .active(true)
                .build();
        User saved = userRepository.save(user);
        try { moodleSyncService.ensureMoodleUser(saved); } catch (Exception e) { log.warn("Moodle user sync failed: {}", e.getMessage()); }
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest req) {
        User user = findUser(id);
        user.setRole(req.getRole());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse toggleUserActive(Long id) {
        User user = findUser(id);
        user.setActive(!user.isActive());
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
