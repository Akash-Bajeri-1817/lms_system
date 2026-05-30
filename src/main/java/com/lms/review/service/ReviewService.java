package com.lms.review.service;

import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.review.dto.CourseRatingResponse;
import com.lms.review.dto.ReviewRequest;
import com.lms.review.dto.ReviewResponse;
import com.lms.review.entity.Review;
import com.lms.review.repository.ReviewRepository;
import com.lms.search.service.SearchService;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SearchService searchService;

    // student submits a review
    @Transactional
    public ReviewResponse createReview(Long courseId,
                                       ReviewRequest request) {
        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new RuntimeException("Course not found")
                );

        // only enrolled students can review
        if (!enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), courseId)) {
            throw new RuntimeException(
                    "You must be enrolled to review this course"
            );
        }

        // one review per student per course
        if (reviewRepository.existsByStudentIdAndCourseId(
                student.getId(), courseId)) {
            throw new RuntimeException(
                    "You have already reviewed this course"
            );
        }

        var review = Review.builder()
                .student(student)
                .course(course)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        var saved = reviewRepository.save(review);

        // update average rating in Elasticsearch
        Double newAverage = reviewRepository
                .findAverageRatingByCourseId(courseId);
        searchService.updateCourseRating(courseId, newAverage);

        return mapToResponse(saved);
    }

    // student updates their existing review
    @Transactional
    public ReviewResponse updateReview(Long courseId,
                                       ReviewRequest request) {
        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var review = reviewRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() ->
                        new RuntimeException("Review not found")
                );

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        var saved = reviewRepository.save(review);

        // update Elasticsearch with new average
        Double newAverage = reviewRepository
                .findAverageRatingByCourseId(courseId);
        searchService.updateCourseRating(courseId, newAverage);

        return mapToResponse(saved);
    }

    // student deletes their review
    @Transactional
    public void deleteReview(Long courseId) {
        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var review = reviewRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() ->
                        new RuntimeException("Review not found")
                );

        reviewRepository.delete(review);

        // update Elasticsearch with new average
        Double newAverage = reviewRepository
                .findAverageRatingByCourseId(courseId);
        searchService.updateCourseRating(courseId,
                newAverage != null ? newAverage : 0.0);
    }

    // get all reviews for a course with rating breakdown
    public CourseRatingResponse getCourseReviews(Long courseId, int page,
                                                 int size) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new RuntimeException("Course not found")
                );

        // get paginated reviews
        var reviewPage = reviewRepository
                .findByCourseIdOrderByCreatedAtDesc(
                        courseId, PageRequest.of(page, size)
                );

        // calculate rating breakdown
        Double average = reviewRepository
                .findAverageRatingByCourseId(courseId);

        List<ReviewResponse> reviews = reviewPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return CourseRatingResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .averageRating(average != null
                        ? Math.round(average * 10.0) / 10.0 : 0.0)
                .totalReviews(reviewRepository.countByCourseId(courseId))
                .fiveStars((int) reviewRepository
                        .countByCourseIdAndRating(courseId, 5))
                .fourStars((int) reviewRepository
                        .countByCourseIdAndRating(courseId, 4))
                .threeStars((int) reviewRepository
                        .countByCourseIdAndRating(courseId, 3))
                .twoStars((int) reviewRepository
                        .countByCourseIdAndRating(courseId, 2))
                .oneStar((int) reviewRepository
                        .countByCourseIdAndRating(courseId, 1))
                .reviews(reviews)
                .build();
    }

    private ReviewResponse mapToResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .studentName(
                        r.getStudent().getFirstName() + " " +
                                r.getStudent().getLastName()
                )
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}