package com.lms.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponse {
    private Long courseId;
    private String courseTitle;
    private long totalLessons;
    private long completedLessons;
    private double progressPercentage;
    private boolean courseCompleted;
    private CertificateResponse certificate;   // null if not yet completed
    private List<LessonProgressResponse> lessonProgress;
}