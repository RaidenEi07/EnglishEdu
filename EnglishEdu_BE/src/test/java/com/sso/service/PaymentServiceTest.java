package com.sso.service;

import com.sso.dto.request.PaymentCallbackRequest;
import com.sso.dto.response.PaymentResponse;
import com.sso.entity.*;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationPushService notificationPushService;

    @InjectMocks private PaymentService paymentService;

    private User buildUser(Long id) {
        return User.builder().id(id).username("user" + id).email("u" + id + "@x.com")
                .password("hashed").role("STUDENT").active(true).build();
    }

    private Course buildCourse(Long id, boolean free, BigDecimal price) {
        Course c = new Course();
        c.setId(id);
        c.setName("Course " + id);
        c.setFree(free);
        c.setPrice(price);
        return c;
    }

    private Enrollment buildEnrollment(User user, Course course) {
        Enrollment e = new Enrollment();
        e.setUser(user);
        e.setCourse(course);
        e.setStatus("pending");
        return e;
    }

    // ── initiatePayment ───────────────────────────────────────────────────────

    @Test
    void initiatePayment_throwsWhenCourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 99L, "VNPAY"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void initiatePayment_throwsWhenCourseIsFree() {
        Course free = buildCourse(1L, true, BigDecimal.ZERO);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(free));

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 1L, "VNPAY"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("free");
    }

    @Test
    void initiatePayment_throwsWhenEnrollmentNotFound() {
        Course paid = buildCourse(2L, false, new BigDecimal("500000"));
        User user = buildUser(1L);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(paid));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 2L, "VNPAY"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Enrollment not found");
    }

    @Test
    void initiatePayment_throwsWhenAlreadyCompleted() {
        Course paid = buildCourse(2L, false, new BigDecimal("500000"));
        User user = buildUser(1L);
        Enrollment enrollment = buildEnrollment(user, paid);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(paid));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(Optional.of(enrollment));
        when(paymentRepository.existsByUserIdAndCourseIdAndStatus(1L, 2L, "COMPLETED")).thenReturn(true);

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 2L, "VNPAY"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void initiatePayment_success_returnsPaymentWithTransactionId() {
        Course paid = buildCourse(2L, false, new BigDecimal("500000"));
        User user = buildUser(1L);
        Enrollment enrollment = buildEnrollment(user, paid);

        when(courseRepository.findById(2L)).thenReturn(Optional.of(paid));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(Optional.of(enrollment));
        when(paymentRepository.existsByUserIdAndCourseIdAndStatus(1L, 2L, "COMPLETED")).thenReturn(false);

        Payment savedPayment = new Payment();
        savedPayment.setUser(user);
        savedPayment.setCourse(paid);
        savedPayment.setTransactionId("tx-123");
        savedPayment.setStatus("PENDING");
        savedPayment.setAmount(paid.getPrice());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse result = paymentService.initiatePayment(1L, 2L, "VNPAY");

        assertThat(result).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    // ── handlePaymentCallback ─────────────────────────────────────────────────

    @Test
    void handlePaymentCallback_throwsWhenTransactionNotFound() {
        when(paymentRepository.findByTransactionId("bad-tx")).thenReturn(Optional.empty());

        PaymentCallbackRequest req = new PaymentCallbackRequest();
        req.setTransactionId("bad-tx");
        req.setStatus("COMPLETED");

        assertThatThrownBy(() -> paymentService.handlePaymentCallback(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handlePaymentCallback_idempotent_whenAlreadyCompleted() {
        Course paid = buildCourse(2L, false, new BigDecimal("500000"));
        User user = buildUser(1L);
        Payment payment = new Payment();
        payment.setTransactionId("tx-ok");
        payment.setStatus("COMPLETED");
        payment.setUser(user);
        payment.setCourse(paid);

        when(paymentRepository.findByTransactionId("tx-ok")).thenReturn(Optional.of(payment));

        PaymentCallbackRequest req = new PaymentCallbackRequest();
        req.setTransactionId("tx-ok");
        req.setStatus("COMPLETED");

        PaymentResponse result = paymentService.handlePaymentCallback(req);

        assertThat(result).isNotNull();
        // No save should be called — idempotent
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handlePaymentCallback_failed_setsStatusFailed() {
        Course paid = buildCourse(2L, false, new BigDecimal("500000"));
        User user = buildUser(1L);

        Payment payment = new Payment();
        payment.setTransactionId("tx-fail");
        payment.setStatus("PENDING");
        payment.setUser(user);
        payment.setCourse(paid);

        when(paymentRepository.findByTransactionId("tx-fail")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentCallbackRequest req = new PaymentCallbackRequest();
        req.setTransactionId("tx-fail");
        req.setStatus("FAILED");

        paymentService.handlePaymentCallback(req);

        assertThat(payment.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void handlePaymentCallback_completed_activatesEnrollment() {
        Course paid = buildCourse(2L, false, new BigDecimal("500000"));
        User user = buildUser(1L);
        Enrollment enrollment = buildEnrollment(user, paid);

        Payment payment = new Payment();
        payment.setTransactionId("tx-new");
        payment.setStatus("PENDING");
        payment.setUser(user);
        payment.setCourse(paid);
        payment.setEnrollment(enrollment);

        when(paymentRepository.findByTransactionId("tx-new")).thenReturn(Optional.of(payment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentCallbackRequest req = new PaymentCallbackRequest();
        req.setTransactionId("tx-new");
        req.setStatus("COMPLETED");

        paymentService.handlePaymentCallback(req);

        assertThat(payment.getStatus()).isEqualTo("COMPLETED");
        assertThat(enrollment.getStatus()).isEqualTo("active");
        verify(enrollmentRepository).save(enrollment);
        verify(notificationPushService).sendNotification(eq(1L), any(), any(), eq("PAYMENT"));
    }
}
