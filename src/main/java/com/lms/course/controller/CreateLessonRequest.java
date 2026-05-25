package com.lms.course.controller;

import com.lms.course.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLessonRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String contentUrl;

    @NotNull(message = "Type is required")
    private LessonType type;      // VIDEO, ARTICLE, QUIZ

    private Integer duration = 0;
    private boolean free = false;
}