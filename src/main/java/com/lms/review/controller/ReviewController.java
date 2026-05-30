package com.lms.review.controller;

import com.lms.common.ApiResponse;
import com.lms.review.dto.CourseRatingResponse;
import com.lms.review.dto.ReviewRequest;
import com.lms.review.dto.ReviewResponse;
import com.lms.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Course reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Submit a review for a course")
    @PostMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long courseId,
            @RequestBody @Valid ReviewRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.createReview(courseId, request),
                        "Review submitted successfully"
                )
        );
    }

    @Operation(summary = "Update your review")
    @PutMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long courseId,
            @RequestBody @Valid ReviewRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.updateReview(courseId, request),
                        "Review updated successfully"
                )
        );
    }

    @Operation(summary = "Delete your review")
    @DeleteMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long courseId) {
        reviewService.deleteReview(courseId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Review deleted successfully")
        );
    }

    @Operation(summary = "Get all reviews and rating breakdown for a course")
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<CourseRatingResponse>> getCourseReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.getCourseReviews(courseId, page, size)
                )
        );
    }
}