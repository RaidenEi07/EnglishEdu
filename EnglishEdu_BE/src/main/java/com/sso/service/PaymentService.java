package com.sso.service;

import com.sso.dto.request.PaymentCallbackRequest;
import com.sso.dto.response.PaymentResponse;
import com.sso.entity.*;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NotificationPushService notificationPushService;

    /**
     * Initiate payment for a paid course enrollment.
     * Returns payment with a transaction ID to redirect user to payment gateway.
     */
    @Transactional
    public PaymentResponse initiatePayment(Long userId, Long courseId, String paymentMethod) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (course.isFree()) {
            throw new BadRequestException("This course is free, no payment required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new BadRequestException("Enrollment not found. Please enroll first."));

        if (paymentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, "COMPLETED")) {
            throw new BadRequestException("Payment already completed for this course");
        }

        Payment payment = Payment.builder()
                .user(user)
                .course(course)
                .enrollment(enrollment)
                .amount(course.getPrice())
                .paymentMethod(paymentMethod)
                .transactionId(UUID.randomUUID().toString())
                .build();
        payment = paymentRepository.save(payment);

        return toResponse(payment);
    }

    /**
     * Webhook callback from payment gateway.
     * On success: activate enrollment automatically.
     */
    @Transactional
    public PaymentResponse handlePaymentCallback(PaymentCallbackRequest req) {
        Payment payment = paymentRepository.findByTransactionId(req.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if ("COMPLETED".equals(req.getStatus())) {
            payment.setStatus("COMPLETED");
            payment.setPaidAt(Instant.now());

            // Auto-activate enrollment
            Enrollment enrollment = payment.getEnrollment();
            if (enrollment != null && !"active".equals(enrollment.getStatus())) {
                enrollment.setStatus("active");
                enrollment.setApprovedAt(Instant.now());
                enrollmentRepository.save(enrollment);

                // Push notification
                notificationPushService.sendNotification(
                        enrollment.getUser().getId(),
                        "Thanh toán thành công! Khóa học \"" + payment.getCourse().getName() + "\" đã được kích hoạt.",
                        "/pages/my/courses/",
                        "PAYMENT"
                );
            }
        } else {
            payment.setStatus("FAILED");
        }

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, int page, int size) {
        return paymentRepository.findByUserId(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .courseId(p.getCourse().getId())
                .courseName(p.getCourse().getName())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
