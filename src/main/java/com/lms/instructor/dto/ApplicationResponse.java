package com.lms.instructor.dto;

import com.lms.instructor.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private String applicantName;
    private String applicantEmail;
    private String expertise;
    private String experience;
    private String reason;
    private ApplicationStatus status;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
}