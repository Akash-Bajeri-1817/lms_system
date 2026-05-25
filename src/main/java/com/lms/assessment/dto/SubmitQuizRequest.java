package com.lms.assessment.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SubmitQuizRequest {
    // Map of questionId → optionId
    // null value = skipped question
    private Map<Long, Long> answers;
}