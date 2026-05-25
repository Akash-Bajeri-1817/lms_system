package com.lms.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OptionRequest {
    @NotBlank(message = "Option text is required")
    private String optionText;
    private boolean correct;
}