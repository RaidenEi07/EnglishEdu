package com.sso.controller;

import com.sso.dto.request.ForgotPasswordRequest;
import com.sso.dto.request.LoginRequest;
import com.sso.dto.request.RegisterRequest;
import com.sso.dto.request.ResetPasswordRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.AuthResponse;
import com.sso.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<AuthResponse>> guestLogin() {
        return ResponseEntity.ok(ApiResponse.ok(authService.guestLogin()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset email sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        // Guard: header may be missing or malformed — never crash with StringIndexOutOfBoundsException
        if (bearerToken != null && bearerToken.startsWith("Bearer ") && bearerToken.length() > 7) {
            String token = bearerToken.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}
