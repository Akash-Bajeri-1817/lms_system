package com.lms.course.dto;

import com.lms.course.entity.CourseStatus;
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
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private String slug;
    private CourseStatus status;
    private BigDecimal price;
    private String instructorName;   // we expose name, not the full User object
    private LocalDateTime createdAt;
}
