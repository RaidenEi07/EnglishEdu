package com.sso.controller;

import com.sso.dto.request.PaymentCallbackRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.PaymentResponse;
import com.sso.service.PaymentRateLimitService;
import com.sso.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRateLimitService rateLimitService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @RequestParam Long courseId,
            @RequestParam String method,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest request) {
        if (!rateLimitService.isAllowed(getClientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Quá nhiều yêu cầu. Vui lòng thử lại sau 60 giây."));
        }
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiatePayment(userId, courseId, method)));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentCallback(
            @Valid @RequestBody PaymentCallbackRequest callback,
            HttpServletRequest request) {
        if (!rateLimitService.isAllowed(getClientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Quá nhiều yêu cầu. Vui lòng thử lại sau 60 giây."));
        }
        return ResponseEntity.ok(ApiResponse.ok(paymentService.handlePaymentCallback(callback)));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> myPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getUserPayments(userId, page, size)));
    }
}
