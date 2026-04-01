package com.sso.controller;

import com.sso.dto.request.ChangePasswordRequest;
import com.sso.dto.request.UpdateProfileRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.UserResponse;
import com.sso.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(getUserId(user))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(getUserId(user), request)));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(getUserId(user), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(userService.uploadAvatar(getUserId(user), file)));
    }

    private Long getUserId(UserDetails user) {
        return Long.parseLong(user.getUsername());
    }
}
