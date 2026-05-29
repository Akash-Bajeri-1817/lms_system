package com.lms.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private String courseId;
    private Long id;
    private String title;
    private String description;
    private String instructorName;
    private BigDecimal price;
    private Double averageRating;
    private Integer totalStudents;
    private String status;
    private float score;           // relevance score from Elasticsearch
}