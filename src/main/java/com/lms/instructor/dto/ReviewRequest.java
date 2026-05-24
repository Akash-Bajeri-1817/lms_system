package com.lms.instructor.dto;

import com.lms.instructor.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Decision is required")
    private ApplicationStatus decision;   // APPROVED or REJECTED

    private String rejectionReason;       // required only if REJECTED
}