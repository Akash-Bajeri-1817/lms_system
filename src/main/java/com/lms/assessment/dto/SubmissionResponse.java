package com.lms.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private String quizTitle;
    private Integer score;
    private Integer totalMarks;
    private Integer totalQuestions;
    private Integer attempted;
    private BigDecimal percentage;
    private boolean passed;
    private LocalDateTime submittedAt;
}