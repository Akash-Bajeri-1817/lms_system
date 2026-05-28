package com.lms.payment.entity;

public enum PaymentStatus {
    PENDING,    // order created, payment not yet done
    SUCCESS,    // payment verified
    FAILED,     // payment failed
    REFUNDED    // payment refunded
}