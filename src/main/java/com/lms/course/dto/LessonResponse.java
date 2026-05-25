package com.lms.course.dto;

import com.lms.course.entity.LessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private Long id;
    private Long moduleId;
    private String title;
    private String description;
    private String contentUrl;
    private LessonType type;
    private Integer duration;
    private Integer position;
    private boolean free;
}