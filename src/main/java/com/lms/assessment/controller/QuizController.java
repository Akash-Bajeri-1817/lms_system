package com.lms.assessment.controller;

import com.lms.assessment.dto.*;
import com.lms.assessment.service.QuizService;
import com.lms.common.ApiResponse;
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
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Tag(name = "Assessments", description = "Quiz creation and submission endpoints")
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Create a quiz for a course")
    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @PathVariable Long courseId,
            @RequestBody @Valid QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        quizService.createQuiz(courseId, request),
                        "Quiz created successfully"
                ));
    }

    @Operation(summary = "Publish a quiz")
    @PatchMapping("/{quizId}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> publishQuiz(
            @PathVariable Long quizId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        quizService.publishQuiz(quizId),
                        "Quiz published"
                )
        );
    }

    @Operation(summary = "Get all quizzes for a course")
    @GetMapping("/course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getQuizzesByCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuizzesByCourse(courseId))
        );
    }

    @Operation(summary = "Get quiz to take — student view, no correct answers shown")
    @GetMapping("/{quizId}/take")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<QuizResponse>> takeQuiz(
            @PathVariable Long quizId) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuizForStudent(quizId))
        );
    }

    @Operation(summary = "Get quiz with answers — instructor view")
    @GetMapping("/{quizId}/review")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> reviewQuiz(
            @PathVariable Long quizId) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuizForInstructor(quizId))
        );
    }

    @Operation(summary = "Submit quiz answers")
    @PostMapping("/{quizId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        quizService.submitQuiz(quizId, request),
                        "Quiz submitted successfully"
                )
        );
    }

    @Operation(summary = "Get my result for a quiz")
    @GetMapping("/{quizId}/my-result")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getMyResult(
            @PathVariable Long quizId) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getMyResult(quizId))
        );
    }

    @Operation(summary = "Get all student results for a quiz — instructor only")
    @GetMapping("/{quizId}/results")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getQuizResults(
            @PathVariable Long quizId) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.getQuizResults(quizId))
        );
    }
}