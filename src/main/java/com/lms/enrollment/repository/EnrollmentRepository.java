package com.lms.enrollment.repository;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // get all enrollments for a student
    List<Enrollment> findByStudentId(Long studentId);

    // get all enrollments for a course (instructor wants to see their students)
    List<Enrollment> findByCourseId(Long courseId);

    // check if student already enrolled in this course
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // find a specific enrollment
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // count total students in a course
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}