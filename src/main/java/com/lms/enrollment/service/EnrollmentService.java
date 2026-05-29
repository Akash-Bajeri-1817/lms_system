package com.lms.enrollment.service;

import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.dto.EnrollmentResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.EmailTemplateService;
import com.lms.search.service.SearchService;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailTemplateService templateService;
    private final SearchService searchService;


    public EnrollmentResponse enroll(Long courseId) {

        // get current logged-in student
        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // if course has a price, student must pay first
        if (course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException(
                    "This is a paid course. Please complete payment first."
            );
        }

        // only allow enrollment in published courses
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Course is not available for enrollment");
        }

        // prevent duplicate enrollment
        if (enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), courseId)) {
            throw new RuntimeException("You are already enrolled in this course");
        }
        searchService.updateStudentCount(courseId, 1);

        var enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        var saved = enrollmentRepository.save(enrollment);
        emailService.sendEmail(
                student.getEmail(),
                "Enrolled: " + course.getTitle(),
                templateService.enrollmentEmail(
                        student.getFirstName(),
                        course.getTitle(),
                        course.getInstructor().getFirstName() + " " +
                                course.getInstructor().getLastName()
                )
        );
        return mapToResponse(saved);
    }

    public List<EnrollmentResponse> getMyEnrollments() {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return enrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public EnrollmentResponse dropCourse(Long courseId) {

        var email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        var student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        var saved = enrollmentRepository.save(enrollment);
        return mapToResponse(saved);
    }

    private EnrollmentResponse mapToResponse(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .courseTitle(e.getCourse().getTitle())
                .courseSlug(e.getCourse().getSlug())
                .instructorName(
                        e.getCourse().getInstructor().getFirstName() + " " +
                                e.getCourse().getInstructor().getLastName()
                )
                .status(e.getStatus())
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .build();
    }
}