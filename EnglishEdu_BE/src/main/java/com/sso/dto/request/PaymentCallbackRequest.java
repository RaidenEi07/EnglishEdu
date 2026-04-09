package com.sso.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PaymentCallbackRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(COMPLETED|FAILED)$", message = "Status must be COMPLETED or FAILED")
    private String status;

    private String paymentMethod;   // VNPAY, MOMO, STRIPE
}
