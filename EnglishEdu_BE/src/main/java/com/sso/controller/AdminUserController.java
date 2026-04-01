package com.sso.controller;

import com.sso.dto.request.CreateAdminUserRequest;
import com.sso.dto.request.UpdateUserRoleRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.UserResponse;
import com.sso.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUsersAdmin(role, keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userService.createUser(request)));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateUserRole(id, request)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<UserResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.toggleUserActive(id)));
    }
}
