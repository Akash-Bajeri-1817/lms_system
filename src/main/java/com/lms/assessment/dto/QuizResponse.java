package com.lms.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Long id;
    private String title;
    private String description;
    private Integer passingScore;
    private Integer timeLimit;
    private Integer totalMarks;
    private boolean published;
    private int questionCount;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;  // null when listing, filled when viewing one quiz
}