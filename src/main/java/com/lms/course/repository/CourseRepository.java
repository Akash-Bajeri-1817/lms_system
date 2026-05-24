package com.lms.course.repository;

import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    List<Course> findByInstructorId(Long instructorId);

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);
}