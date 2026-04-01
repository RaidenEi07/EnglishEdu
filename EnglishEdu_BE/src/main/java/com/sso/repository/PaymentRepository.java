package com.sso.repository;

import com.sso.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Page<Payment> findByStatus(String status, Pageable pageable);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByEnrollmentId(Long enrollmentId);

    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);
}
