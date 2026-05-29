package com.lms.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// @Document = like @Entity but for Elasticsearch
// indexName = like a table name in PostgreSQL
@Document(indexName = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {

    @Id
    private String id;           // Elasticsearch uses String IDs

    private Long courseId;       // reference to PostgreSQL course id

    // FieldType.Text = analyzed for full text search
    // analyzer = "english" handles stemming:
    // "running" matches "run", "courses" matches "course"
    @Field(type = FieldType.Text, analyzer = "english")
    private String title;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    // FieldType.Keyword = exact match only (not analyzed)
    // used for filtering, not searching
    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String instructorName;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer totalStudents;

    @Field(type = FieldType.Date)
    private LocalDateTime publishedAt;
}