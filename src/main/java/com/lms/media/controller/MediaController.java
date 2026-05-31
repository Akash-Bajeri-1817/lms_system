package com.lms.media.controller;

import com.lms.common.ApiResponse;
import com.lms.course.repository.LessonRepository;
import com.lms.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "File upload and access endpoints")
public class MediaController {

    private final MediaService mediaService;
    private final LessonRepository lessonRepository;

    @Operation(summary = "Upload course thumbnail")
    @PostMapping(
            value = "/courses/{courseId}/thumbnail",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadThumbnail(
            @PathVariable Long courseId,
            @RequestPart("file") MultipartFile file) throws IOException {
        String key = mediaService.uploadThumbnail(courseId, file);
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("fileKey", key),
                        "Thumbnail uploaded successfully"
                )
        );
    }

    @Operation(summary = "Upload lesson video")
    @PostMapping(
            value = "/lessons/{lessonId}/video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadVideo(
            @PathVariable Long lessonId,
            @RequestPart("file") MultipartFile file) throws IOException {
        String key = mediaService.uploadVideo(lessonId, file);
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("fileKey", key),
                        "Video uploaded successfully"
                )
        );
    }

    @Operation(summary = "Upload lesson attachment")
    @PostMapping(
            value = "/lessons/{lessonId}/attachment",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(
            @PathVariable Long lessonId,
            @RequestPart("file") MultipartFile file) throws IOException {
        String key = mediaService.uploadAttachment(lessonId, file);
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("fileKey", key),
                        "Attachment uploaded successfully"
                )
        );
    }

    @Operation(summary = "Get secure access URL for a file")
    @GetMapping("/access")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAccessUrl(
            @RequestParam String fileKey) {
        String url = mediaService.generatePresignedUrl(fileKey);
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("url", url),
                        "Access URL generated — expires in "
                                + "60 minutes"
                )
        );
    }

    @Operation(summary = "Get HLS streaming URL for a lesson video")
    @GetMapping("/lessons/{lessonId}/stream")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStreamUrl(
            @PathVariable Long lessonId) {

        var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new RuntimeException("Lesson not found")
                );

        if (lesson.getContentUrl() == null) {
            throw new RuntimeException(
                    "Video not yet available — transcoding may still be in progress"
            );
        }

        // generate presigned URL for master playlist
        String streamUrl = mediaService
                .generatePresignedUrl(lesson.getContentUrl());

        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of(
                                "streamUrl", streamUrl,
                                "format", "HLS",
                                "note", "Use a HLS-compatible player like Video.js or hls.js"
                        ),
                        "Stream URL generated"
                )
        );
    }
}