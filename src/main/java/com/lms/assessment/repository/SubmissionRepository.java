package com.lms.assessment.repository;

import com.lms.assessment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    boolean existsByStudentIdAndQuizId(Long studentId, Long quizId);
    Optional<Submission> findByStudentIdAndQuizId(Long studentId, Long quizId);
    List<Submission> findByStudentId(Long studentId);
    List<Submission> findByQuizId(Long quizId);
}