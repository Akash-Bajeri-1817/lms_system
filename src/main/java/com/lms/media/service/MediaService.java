package com.lms.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private int presignedUrlExpiry;

    // allowed file types
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/mpeg", "video/quicktime"
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument" +
                    ".presentationml.presentation"
    );

    // max file sizes
    private static final long MAX_IMAGE_SIZE  = 5  * 1024 * 1024;  // 5MB
    private static final long MAX_VIDEO_SIZE  = 500 * 1024 * 1024; // 500MB
    private static final long MAX_DOC_SIZE    = 50  * 1024 * 1024; // 50MB

    // upload course thumbnail
    public String uploadThumbnail(Long courseId,
                                  MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
        String key = "thumbnails/course-" + courseId + "/"
                + UUID.randomUUID() + getExtension(file);
        return uploadToS3(key, file);
    }

    // upload lesson video
    public String uploadVideo(Long lessonId,
                              MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_VIDEO_TYPES, MAX_VIDEO_SIZE);
        String key = "videos/lesson-" + lessonId + "/"
                + UUID.randomUUID() + getExtension(file);
        return uploadToS3(key, file);
    }

    // upload lesson attachment (PDF, slides)
    public String uploadAttachment(Long lessonId,
                                   MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_DOC_SIZE);
        String key = "attachments/lesson-" + lessonId + "/"
                + UUID.randomUUID() + getExtension(file);
        return uploadToS3(key, file);
    }

    // upload certificate PDF
    public String uploadCertificate(String certificateNumber,
                                    byte[] pdfBytes) {
        String key = "certificates/" + certificateNumber + ".pdf";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdfBytes)
        );
        log.info("Certificate uploaded: {}", key);
        return key;
    }

    // generate a presigned URL — expires after configured minutes
    // this is how students securely access videos
    public String generatePresignedUrl(String fileKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiry))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build())
                .build();

        String url = s3Presigner.presignGetObject(presignRequest)
                .url().toString();

        log.info("Generated presigned URL for: {}", fileKey);
        return url;
    }

    // delete a file from S3
    public void deleteFile(String fileKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build());
        log.info("Deleted file from S3: {}", fileKey);
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────────

    private String uploadToS3(String key,
                              MultipartFile file) throws IOException {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );
        log.info("Uploaded file to S3: {}", key);
        return key;   // return the key, not the URL
        // URL is generated on-demand via presigned URLs
    }

    private void validateFile(MultipartFile file,
                              Set<String> allowedTypes,
                              long maxSize) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (!allowedTypes.contains(file.getContentType())) {
            throw new RuntimeException(
                    "File type not allowed: " + file.getContentType()
            );
        }
        if (file.getSize() > maxSize) {
            throw new RuntimeException(
                    "File too large. Maximum size: " + (maxSize / 1024 / 1024)
                            + "MB"
            );
        }
    }

    private String getExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(
                    originalName.lastIndexOf(".")
            );
        }
        return "";
    }
}