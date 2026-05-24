package com.lms.enrollment.controller;

import com.lms.enrollment.dto.EnrollmentResponse;
import com.lms.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Course enrollment endpoints")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // only students can enroll
    @Operation(summary = "Enroll in a course")
    @PostMapping("/enroll/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.enroll(courseId));
    }

    // students view their own enrollments
    @Operation(summary = "Get current student's enrollments")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments() {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments());
    }

    // student drops a course
    @Operation(summary = "Drop a course enrollment")
    @DeleteMapping("/drop/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> dropCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.dropCourse(courseId));
    }
}