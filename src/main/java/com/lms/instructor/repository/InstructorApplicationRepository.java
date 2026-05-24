package com.lms.instructor.repository;

import com.lms.instructor.entity.ApplicationStatus;
import com.lms.instructor.entity.InstructorApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorApplicationRepository
        extends JpaRepository<InstructorApplication, Long> {

    // check if user already has an application
    boolean existsByApplicantIdAndStatus(Long userId, ApplicationStatus status);

    // get all pending applications for admin review
    List<InstructorApplication> findByStatus(ApplicationStatus status);

    // get a specific user's application
    Optional<InstructorApplication> findByApplicantId(Long userId);
}