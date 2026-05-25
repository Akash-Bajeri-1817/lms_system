package com.lms.assessment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class QuestionRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    @Min(value = 1, message = "Marks must be at least 1")
    private Integer marks;

    @NotEmpty(message = "At least one option is required")
    private List<OptionRequest> options;
}