package com.lms.course.repository;

import com.lms.course.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByCourseIdOrderByPositionAsc(Long courseId);
    long countByCourseId(Long courseId);
}