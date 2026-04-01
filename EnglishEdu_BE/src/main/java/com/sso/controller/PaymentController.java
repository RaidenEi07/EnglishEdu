package com.sso.controller;

import com.sso.dto.request.PaymentCallbackRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.PaymentResponse;
import com.sso.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @RequestParam Long courseId,
            @RequestParam String method,
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.initiatePayment(userId, courseId, method)));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentCallback(
            @Valid @RequestBody PaymentCallbackRequest callback) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.handlePaymentCallback(callback)));
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
