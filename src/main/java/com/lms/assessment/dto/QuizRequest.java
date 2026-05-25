package com.lms.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class QuizRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Integer passingScore = 70;  // default 70%

    private Integer timeLimit;          // null = no limit

    @NotEmpty(message = "At least one question is required")
    private List<QuestionRequest> questions;
}