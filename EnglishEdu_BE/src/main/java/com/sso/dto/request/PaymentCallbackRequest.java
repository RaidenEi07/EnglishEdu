package com.sso.dto.request;

import lombok.Data;

@Data
public class PaymentCallbackRequest {
    private String transactionId;
    private String status;          // COMPLETED, FAILED
    private String paymentMethod;   // VNPAY, MOMO, STRIPE
}
