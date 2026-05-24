package com.lms.course.controller;

import com.lms.common.ApiResponse;
import com.lms.common.PageResponse;
import com.lms.course.dto.CourseRequest;
import com.lms.course.dto.CourseResponse;
import com.lms.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    private final CourseService courseService;

    // only INSTRUCTOR or ADMIN can create a course
    @Operation(summary = "Create a new course")
    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(
            @RequestBody @Valid CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(request));
    }

    // everyone can view published courses — no @PreAuthorize needed
    @Operation(summary = "Get paginated list of published courses")
    @GetMapping("/published")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getPublishedCourses(
            @RequestParam(defaultValue = "0") int page,    // default page 0
            @RequestParam(defaultValue = "10") int size    // default 10 per page
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        courseService.getPublishedCourses(page, size),
                        "Published courses retrieved"
                )
        );
    }

    // only ADMIN can see all courses including drafts
    @Operation(summary = "Get paginated list of all courses (drafts and published)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        courseService.getAllCourses(page, size),
                        "All courses retrieved"
                )
        );
    }

    // only ADMIN can delete
    @Operation(summary = "Delete a course")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publish a course")
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> publishCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.publishCourse(id));
    }
}