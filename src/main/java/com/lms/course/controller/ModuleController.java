package com.lms.course.controller;

import com.lms.common.ApiResponse;
import com.lms.course.dto.LessonResponse;
import com.lms.course.dto.ModuleResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.LessonType;
import com.lms.course.entity.Module;
import com.lms.course.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Tag(name = "Modules", description = "Course module and lesson management")
public class ModuleController {

    private final ModuleService moduleService;

    @Operation(summary = "Create a module inside a course")
    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable Long courseId,
            @RequestBody CreateModuleRequest request) {

        Module module = moduleService.createModule(
                courseId, request.getTitle(), request.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        ModuleResponse.builder()
                                .id(module.getId())
                                .title(module.getTitle())
                                .description(module.getDescription())
                                .position(module.getPosition())
                                .courseId(module.getCourse().getId())
                                .build(),
                        "Module created successfully"
                ));
    }

    @Operation(summary = "Create a lesson inside a module")
    @PostMapping("/{moduleId}/lessons")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable Long moduleId,
            @RequestBody CreateLessonRequest request) {

        Lesson lesson = moduleService.createLesson(
                moduleId,
                request.getTitle(),
                request.getDescription(),
                request.getContentUrl(),
                request.getType(),
                request.getDuration(),
                request.isFree()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        LessonResponse.builder()
                                .id(lesson.getId())
                                .moduleId(lesson.getModule().getId())
                                .title(lesson.getTitle())
                                .description(lesson.getDescription())
                                .contentUrl(lesson.getContentUrl())
                                .type(lesson.getType())
                                .duration(lesson.getDuration())
                                .position(lesson.getPosition())
                                .free(lesson.isFree())
                                .build(),
                        "Lesson created successfully"
                ));
    }
}