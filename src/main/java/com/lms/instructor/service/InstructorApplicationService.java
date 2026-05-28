package com.lms.instructor.service;

import com.lms.instructor.dto.ApplicationRequest;
import com.lms.instructor.dto.ApplicationResponse;
import com.lms.instructor.dto.ReviewRequest;
import com.lms.instructor.entity.ApplicationStatus;
import com.lms.instructor.entity.InstructorApplication;
import com.lms.instructor.repository.InstructorApplicationRepository;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.EmailTemplateService;
import com.lms.user.entity.Role;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstructorApplicationService {

    private final InstructorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailTemplateService templateService;

    // STUDENT submits an application
    public ApplicationResponse apply(ApplicationRequest request) {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var applicant = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // prevent applying if already an instructor
        if (applicant.getRole() == Role.INSTRUCTOR) {
            throw new RuntimeException("You are already an instructor");
        }

        // prevent duplicate pending application
        if (applicationRepository.existsByApplicantIdAndStatus(
                applicant.getId(), ApplicationStatus.PENDING)) {
            throw new RuntimeException(
                    "You already have a pending application under review"
            );
        }

        var application = InstructorApplication.builder()
                .applicant(applicant)
                .expertise(request.getExpertise())
                .experience(request.getExperience())
                .reason(request.getReason())
                .status(ApplicationStatus.PENDING)
                .build();

        return mapToResponse(applicationRepository.save(application));
    }

    // STUDENT checks their own application status
    public ApplicationResponse getMyApplication() {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var applicant = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var application = applicationRepository
                .findByApplicantId(applicant.getId())
                .orElseThrow(() -> new RuntimeException(
                        "No application found"
                ));

        return mapToResponse(application);
    }

    // ADMIN views all pending applications
    public List<ApplicationResponse> getPendingApplications() {
        return applicationRepository
                .findByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ADMIN approves or rejects an application
    @Transactional   // both the application update AND user role change must succeed together
    public ApplicationResponse reviewApplication(Long applicationId,
                                                 ReviewRequest request) {
        // validate — can't reject without a reason
        if (request.getDecision() == ApplicationStatus.REJECTED &&
                (request.getRejectionReason() == null ||
                        request.getRejectionReason().isBlank())) {
            throw new RuntimeException(
                    "Rejection reason is required when rejecting an application"
            );
        }

        var adminEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // can only review pending applications
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException(
                    "Application has already been reviewed"
            );
        }

        // update application
        application.setStatus(request.getDecision());
        application.setRejectionReason(request.getRejectionReason());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);

        // if approved — upgrade the user's role to INSTRUCTOR
        if (request.getDecision() == ApplicationStatus.APPROVED) {
            User applicant = application.getApplicant();
            applicant.setRole(Role.INSTRUCTOR);
            userRepository.save(applicant);   // save role change
        }

        if (request.getDecision() == ApplicationStatus.APPROVED) {
            emailService.sendEmail(
                    application.getApplicant().getEmail(),
                    "Your Instructor Application is Approved! 🎉",
                    templateService.instructorApprovedEmail(
                            application.getApplicant().getFirstName()
                    )
            );
        } else {
            emailService.sendEmail(
                    application.getApplicant().getEmail(),
                    "Instructor Application Update",
                    templateService.instructorRejectedEmail(
                            application.getApplicant().getFirstName(),
                            request.getRejectionReason()
                    )
            );
        }


        return mapToResponse(applicationRepository.save(application));
    }

    private ApplicationResponse mapToResponse(InstructorApplication a) {
        return ApplicationResponse.builder()
                .id(a.getId())
                .applicantName(
                        a.getApplicant().getFirstName() + " " +
                                a.getApplicant().getLastName()
                )
                .applicantEmail(a.getApplicant().getEmail())
                .expertise(a.getExpertise())
                .experience(a.getExperience())
                .reason(a.getReason())
                .status(a.getStatus())
                .rejectionReason(a.getRejectionReason())
                .appliedAt(a.getAppliedAt())
                .reviewedAt(a.getReviewedAt())
                .reviewedByName(
                        a.getReviewedBy() != null
                                ? a.getReviewedBy().getFirstName() + " " +
                                a.getReviewedBy().getLastName()
                                : null
                )
                .build();
    }
}