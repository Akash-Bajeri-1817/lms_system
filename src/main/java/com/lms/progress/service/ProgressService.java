package com.lms.progress.service;

import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.ModuleRepository;
import com.lms.media.service.MediaService;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.EmailTemplateService;
import com.lms.progress.dto.*;
import com.lms.progress.entity.Certificate;
import com.lms.progress.entity.LessonProgress;
import com.lms.progress.repository.CertificateRepository;
import com.lms.progress.repository.LessonProgressRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LessonProgressRepository progressRepository;
    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailTemplateService templateService;
    private final CertificatePdfService certificatePdfService;
    private final MediaService mediaService;



    // student marks a lesson as watched / completed
    @Transactional
    public LessonProgressResponse updateProgress(
            Long lessonId, LessonProgressRequest request) {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        var course = lesson.getModule().getCourse();

        // find existing progress or create new one
        var progress = progressRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElse(LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .course(course)
                        .watchedSeconds(0)
                        .completed(false)
                        .build());

        // update progress
        progress.setWatchedSeconds(request.getWatchedSeconds());

        // once completed, always completed — can't un-complete a lesson
        if (request.isCompleted() && !progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            log.info("Student {} completed lesson {} in course {}",
                    student.getEmail(), lessonId, course.getId());

            // check if entire course is now complete
            checkAndIssueCertificate(student.getId(), course.getId());
        }

        var saved = progressRepository.save(progress);
        return mapToProgressResponse(saved);
    }

    // get full course progress for the logged-in student
    public CourseProgressResponse getCourseProgress(Long courseId) {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        long totalLessons = lessonRepository
                .countLessonsByCourseId(courseId);

        long completedLessons = progressRepository
                .countByStudentIdAndCourseIdAndCompleted(
                        student.getId(), courseId, true);

        double percentage = totalLessons > 0
                ? (completedLessons * 100.0) / totalLessons
                : 0.0;

        boolean courseCompleted = percentage == 100.0;

        // check if certificate exists
        CertificateResponse certificateResponse = null;
        if (courseCompleted) {
            certificateResponse = certificateRepository
                    .findByStudentIdAndCourseId(student.getId(), courseId)
                    .map(this::mapToCertificateResponse)
                    .orElse(null);
        }

        // get detailed lesson progress
        List<LessonProgressResponse> lessonProgress = progressRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .stream()
                .map(this::mapToProgressResponse)
                .toList();

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .progressPercentage(Math.round(percentage * 100.0) / 100.0)
                .courseCompleted(courseCompleted)
                .certificate(certificateResponse)
                .lessonProgress(lessonProgress)
                .build();
    }

    // get all certificates for the logged-in student
    public List<CertificateResponse> getMyCertificates() {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return certificateRepository.findByStudentId(student.getId())
                .stream()
                .map(this::mapToCertificateResponse)
                .toList();
    }

    // verify a certificate by its number — public endpoint
    public CertificateResponse verifyCertificate(String certificateNumber) {
        return certificateRepository
                .findByCertificateNumber(certificateNumber)
                .map(this::mapToCertificateResponse)
                .orElseThrow(() ->
                        new RuntimeException("Certificate not found or invalid")
                );
    }

    // ── AUTO CERTIFICATE GENERATION ──────────────────────────────────

    @Transactional
    public void checkAndIssueCertificate(Long studentId, Long courseId) {

        long totalLessons = lessonRepository
                .countLessonsByCourseId(courseId);
        long completedLessons = progressRepository
                .countByStudentIdAndCourseIdAndCompleted(
                        studentId, courseId, true);

        if (totalLessons > 0
                && completedLessons >= totalLessons
                && !certificateRepository
                .existsByStudentIdAndCourseId(studentId, courseId)) {

            var student = userRepository.findById(studentId).orElseThrow();
            var course  = courseRepository.findById(courseId).orElseThrow();

            String certNumber = generateCertificateNumber();

            var certificate = Certificate.builder()
                    .student(student)
                    .course(course)
                    .certificateNumber(certNumber)
                    .build();

            Certificate saved = certificateRepository.save(certificate);

            // generate PDF and upload to S3
            try {
                byte[] pdfBytes = certificatePdfService
                        .generateCertificate(saved);
                String pdfKey = mediaService.uploadCertificate(
                        certNumber, pdfBytes);
                saved.setPdfKey(pdfKey);    // store S3 key
                certificateRepository.save(saved);

                log.info("Certificate PDF uploaded: {}", pdfKey);
            } catch (Exception e) {
                log.error("Failed to generate certificate PDF: {}",
                        e.getMessage());
                // certificate record is still saved even if PDF fails
            }

            // send email with certificate number
            emailService.sendEmail(
                    student.getEmail(),
                    "Certificate Earned — " + course.getTitle() + " 🎓",
                    templateService.certificateEmail(
                            student.getFirstName(),
                            course.getTitle(),
                            certNumber
                    )
            );
        }
    }

    private String generateCertificateNumber() {
        // get total certificate count for sequential numbering
        long count = certificateRepository.count() + 1;
        return String.format("LMS-%d-%05d", Year.now().getValue(), count);
    }

    // ── MAPPERS ──────────────────────────────────────────────────────

    private LessonProgressResponse mapToProgressResponse(LessonProgress p) {
        return LessonProgressResponse.builder()
                .lessonId(p.getLesson().getId())
                .lessonTitle(p.getLesson().getTitle())
                .completed(p.isCompleted())
                .watchedSeconds(p.getWatchedSeconds())
                .completedAt(p.getCompletedAt())
                .build();
    }

    private CertificateResponse mapToCertificateResponse(Certificate c) {
        return CertificateResponse.builder()
                .id(c.getId())
                .studentName(
                        c.getStudent().getFirstName() + " " +
                                c.getStudent().getLastName()
                )
                .courseTitle(c.getCourse().getTitle())
                .certificateNumber(c.getCertificateNumber())
                .issuedAt(c.getIssuedAt())
                .build();
    }

    public Map<String, String> getCertificateDownloadUrl(
            String certificateNumber) {

        Certificate cert = certificateRepository
                .findByCertificateNumber(certificateNumber)
                .orElseThrow(() ->
                        new RuntimeException("Certificate not found")
                );

        if (cert.getPdfKey() == null) {
            throw new RuntimeException(
                    "Certificate PDF not yet generated"
            );
        }

        String downloadUrl = mediaService
                .generatePresignedUrl(cert.getPdfKey());

        return Map.of("downloadUrl", downloadUrl);
    }
}