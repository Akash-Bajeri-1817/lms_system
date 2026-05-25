package com.lms.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {
    private Long lessonId;
    private String lessonTitle;
    private boolean completed;
    private Integer watchedSeconds;
    private LocalDateTime completedAt;
}