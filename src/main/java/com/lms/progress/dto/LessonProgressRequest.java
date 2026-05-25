package com.lms.progress.dto;

import lombok.Data;

@Data
public class LessonProgressRequest {
    private Integer watchedSeconds;   // how many seconds watched so far
    private boolean completed;        // has the student finished this lesson
}