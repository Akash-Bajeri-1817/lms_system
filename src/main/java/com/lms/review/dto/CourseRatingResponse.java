package com.lms.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRatingResponse {
    private Long courseId;
    private String courseTitle;
    private Double averageRating;       // e.g. 4.3
    private Long totalReviews;          // e.g. 127
    private Integer fiveStars;          // count of 5-star reviews
    private Integer fourStars;
    private Integer threeStars;
    private Integer twoStars;
    private Integer oneStar;
    private List<ReviewResponse> reviews;
}