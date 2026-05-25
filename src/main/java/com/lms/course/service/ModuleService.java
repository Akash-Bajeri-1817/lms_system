package com.lms.course.service;

import com.lms.course.entity.Lesson;
import com.lms.course.entity.LessonType;
import com.lms.course.entity.Module;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public Module createModule(Long courseId, String title,
                               String description) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        long position = moduleRepository.countByCourseId(courseId);

        var module = Module.builder()
                .course(course)
                .title(title)
                .description(description)
                .position((int) position)
                .build();

        return moduleRepository.save(module);
    }

    @Transactional
    public Lesson createLesson(Long moduleId, String title,
                               String description, String contentUrl,
                               LessonType type, Integer duration,
                               boolean isFree) {
        var module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        long position = module.getLessons().size();

        var lesson = Lesson.builder()
                .module(module)
                .title(title)
                .description(description)
                .contentUrl(contentUrl)
                .type(type)
                .duration(duration)
                .position((int) position)
                .free(isFree)
                .build();

        return lessonRepository.save(lesson);
    }
}