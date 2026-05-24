package com.lms.instructor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationRequest {

    @NotBlank(message = "Expertise is required")
    private String expertise;

    @NotBlank(message = "Experience is required")
    private String experience;

    @NotBlank(message = "Reason is required")
    private String reason;
}