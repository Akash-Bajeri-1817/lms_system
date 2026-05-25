package com.lms.progress.controller;

import com.lms.common.ApiResponse;
import com.lms.progress.dto.*;
import com.lms.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Lesson progress and certificates")
public class ProgressController {

    private final ProgressService progressService;

    @Operation(summary = "Mark a lesson as watched or completed")
    @PostMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> updateProgress(
            @PathVariable Long lessonId,
            @RequestBody LessonProgressRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        progressService.updateProgress(lessonId, request),
                        "Progress updated"
                )
        );
    }

    @Operation(summary = "Get full progress for a course")
    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getCourseProgress(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        progressService.getCourseProgress(courseId)
                )
        );
    }

    @Operation(summary = "Get all my certificates")
    @GetMapping("/certificates")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>>
    getMyCertificates() {
        return ResponseEntity.ok(
                ApiResponse.success(progressService.getMyCertificates())
        );
    }

    @Operation(summary = "Verify a certificate by number — public")
    @GetMapping("/certificates/verify/{certificateNumber}")
    public ResponseEntity<ApiResponse<CertificateResponse>> verifyCertificate(
            @PathVariable String certificateNumber) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        progressService.verifyCertificate(certificateNumber),
                        "Certificate is valid"
                )
        );
    }
}