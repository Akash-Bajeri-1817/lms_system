package com.lms.payment.repository;

import com.lms.payment.entity.Payment;
import com.lms.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String orderId);

    List<Payment> findByStudentId(Long studentId);

    boolean existsByStudentIdAndCourseIdAndStatus(
            Long studentId, Long courseId, PaymentStatus status);
}