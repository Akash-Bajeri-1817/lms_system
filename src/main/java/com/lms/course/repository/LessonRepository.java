package com.lms.course.repository;

import com.lms.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByModuleIdOrderByPositionAsc(Long moduleId);

    // count total lessons in a course across all modules
    @Query("""
        SELECT COUNT(l) FROM Lesson l
        WHERE l.module.course.id = :courseId
    """)
    long countLessonsByCourseId(@Param("courseId") Long courseId);
}