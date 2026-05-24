package com.lms.instructor.controller;

import com.lms.instructor.dto.ApplicationRequest;
import com.lms.instructor.dto.ApplicationResponse;
import com.lms.instructor.dto.ReviewRequest;
import com.lms.instructor.service.InstructorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instructor-applications")
@RequiredArgsConstructor
@Tag(name = "Instructor Applications", description = "Apply to become an instructor")
public class InstructorApplicationController {

    private final InstructorApplicationService applicationService;

    // STUDENT submits application
    @Operation(summary = "Submit a new instructor application")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationResponse> apply(
            @RequestBody @Valid ApplicationRequest request) {
        return ResponseEntity.ok(applicationService.apply(request));
     }

    // STUDENT checks their application status
    @Operation(summary = "Get current student's instructor application")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationResponse> getMyApplication() {
        return ResponseEntity.ok(applicationService.getMyApplication());
    }

    // ADMIN views all pending applications
    @Operation(summary = "Get all pending instructor applications")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getPendingApplications());
    }

    // ADMIN approves or rejects
    @Operation(summary = "Review (approve/reject) an instructor application")
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> review(
            @PathVariable Long id,
            @RequestBody @Valid ReviewRequest request) {
        return ResponseEntity.ok(
                applicationService.reviewApplication(id, request)
        );
    }
}