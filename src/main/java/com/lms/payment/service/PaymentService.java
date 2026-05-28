package com.lms.payment.service;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.EmailTemplateService;
import com.lms.payment.dto.*;
import com.lms.payment.entity.Payment;
import com.lms.payment.entity.PaymentStatus;
import com.lms.payment.repository.PaymentRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.user.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EmailService emailService;
    private final EmailTemplateService templateService;


    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.currency}")
    private String currency;

    // STEP 1 — student initiates payment, we create a Razorpay order
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // prevent paying for a course already enrolled in
        if (enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), course.getId())) {
            throw new RuntimeException(
                    "You are already enrolled in this course"
            );
        }

        // prevent duplicate pending payment
        if (paymentRepository.existsByStudentIdAndCourseIdAndStatus(
                student.getId(), course.getId(), PaymentStatus.PENDING)) {
            throw new RuntimeException(
                    "You already have a pending payment for this course"
            );
        }

        try {
            // create order in Razorpay
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            // Razorpay amount is in paise (1 INR = 100 paise)
            orderRequest.put("amount",
                    course.getPrice()
                            .multiply(BigDecimal.valueOf(100))
                            .intValue()
            );
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "lms_" + student.getId()
                    + "_" + course.getId());

            Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // save pending payment in our database
            Payment payment = Payment.builder()
                    .student(student)
                    .course(course)
                    .razorpayOrderId(razorpayOrderId)
                    .amount(course.getPrice())
                    .currency(currency)
                    .status(PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            log.info("Payment order created: {} for student: {} course: {}",
                    razorpayOrderId, email, course.getTitle());

            // return order details — frontend needs these to open popup
            return CreateOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .keyId(keyId)
                    .amount(course.getPrice())
                    .currency(currency)
                    .courseTitle(course.getTitle())
                    .studentEmail(email)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: "
                    + e.getMessage());
        }
    }

    // STEP 2 — frontend sends back payment details, we verify and enroll
    @Transactional
    public PaymentResponse verifyAndEnroll(VerifyPaymentRequest request) {

        // find our payment record
        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Payment record not found")
                );

        // ── SIGNATURE VERIFICATION ──────────────────────────────────
        // this proves the payment data wasn't tampered with
        // Razorpay signs: orderId + "|" + paymentId with your secret key
        try {
            String payload = request.getRazorpayOrderId()
                    + "|" + request.getRazorpayPaymentId();

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(), "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            String generatedSignature = HexFormat.of()
                    .formatHex(hash);

            // compare our generated signature with Razorpay's signature
            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new RuntimeException(
                        "Payment verification failed — invalid signature"
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Signature verification error: " + e.getMessage()
            );
        }
        // ── END VERIFICATION ─────────────────────────────────────────

        // update payment record
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // automatically enroll the student
        Enrollment enrollment = Enrollment.builder()
                .student(payment.getStudent())
                .course(payment.getCourse())
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollmentRepository.save(enrollment);

        log.info("Payment verified and enrollment created for student: {} "
                        + "course: {}",
                payment.getStudent().getEmail(),
                payment.getCourse().getTitle());

        emailService.sendEmail(
                payment.getStudent().getEmail(),
                "Payment Successful — " + payment.getCourse().getTitle(),
                templateService.paymentSuccessEmail(
                        payment.getStudent().getFirstName(),
                        payment.getCourse().getTitle(),
                        payment.getAmount().toString(),
                        payment.getRazorpayOrderId()
                )
        );


        return mapToResponse(payment);
    }

    // student views their payment history
    public List<PaymentResponse> getMyPayments() {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return paymentRepository.findByStudentId(student.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .courseTitle(p.getCourse().getTitle())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}