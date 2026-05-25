package com.lms.progress.repository;

import com.lms.progress.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository
        extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByStudentIdAndLessonId(
            Long studentId, Long lessonId);

    List<LessonProgress> findByStudentIdAndCourseId(
            Long studentId, Long courseId);

    // count completed lessons for a student in a course
    long countByStudentIdAndCourseIdAndCompleted(
            Long studentId, Long courseId, boolean completed);
}