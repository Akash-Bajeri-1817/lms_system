package com.lms.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    private String razorpayOrderId;    // frontend needs this to open payment popup
    private String keyId;              // frontend needs this too
    private BigDecimal amount;
    private String currency;
    private String courseTitle;
    private String studentEmail;
}