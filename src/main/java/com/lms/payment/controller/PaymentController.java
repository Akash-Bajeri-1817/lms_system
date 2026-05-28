package com.lms.payment.controller;

import com.lms.common.ApiResponse;
import com.lms.payment.dto.*;
import com.lms.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment integration")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create Razorpay order for a course")
    @PostMapping("/create-order")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        paymentService.createOrder(request),
                        "Order created successfully"
                )
        );
    }

    @Operation(summary = "Verify payment and unlock enrollment")
    @PostMapping("/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @RequestBody @Valid VerifyPaymentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        paymentService.verifyAndEnroll(request),
                        "Payment verified — enrollment unlocked!"
                )
        );
    }

    @Operation(summary = "Get my payment history")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments() {
        return ResponseEntity.ok(
                ApiResponse.success(paymentService.getMyPayments())
        );
    }
}