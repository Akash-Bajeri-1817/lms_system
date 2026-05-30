package com.lms.review.repository;

import com.lms.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // check if student already reviewed this course
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // get a specific student's review for a course
    Optional<Review> findByStudentIdAndCourseId(
            Long studentId, Long courseId);

    // get all reviews for a course — paginated
    Page<Review> findByCourseIdOrderByCreatedAtDesc(
            Long courseId, Pageable pageable);

    // calculate average rating for a course
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.id = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") Long courseId);

    // count total reviews for a course
    long countByCourseId(Long courseId);

    // count reviews by specific rating for a course
    // used to build the rating breakdown (5 stars: 42, 4 stars: 31 etc.)
    long countByCourseIdAndRating(Long courseId, Integer rating);
}