package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long courseId;
    private String courseName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
    private String status;
    private Instant paidAt;
    private Instant createdAt;
}
