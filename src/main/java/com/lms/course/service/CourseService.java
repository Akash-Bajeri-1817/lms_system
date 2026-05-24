package com.lms.course.service;

import com.lms.common.PageResponse;
import com.lms.common.SanitizationUtil;
import com.lms.course.dto.CourseRequest;
import com.lms.course.dto.CourseResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final SanitizationUtil sanitizer;

    public CourseResponse createCourse(CourseRequest request) {

        // get the currently logged-in user from the security context
        // SecurityContextHolder holds the authenticated user for the current request
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User instructor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .slug(generateSlug(request.getTitle()))
                .status(CourseStatus.DRAFT)
                .price(request.getPrice())
                .instructor(instructor)
                .build();
        String title = sanitizer.sanitize(request.getTitle());
        String description = sanitizer.sanitize(request.getDescription());

        Course saved = courseRepository.save(course);
        return mapToResponse(saved);
    }

    public PageResponse<CourseResponse> getAllCourses(int page, int size) {
        // Pageable tells Spring: give me 'size' items starting at 'page'
        // sorted by createdAt descending — newest first
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<CourseResponse> result = courseRepository.findAll(pageable)
                .map(this::mapToResponse);

        return PageResponse.from(result);
    }

    public PageResponse<CourseResponse> getPublishedCourses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<CourseResponse> result = courseRepository
                .findByStatus(CourseStatus.PUBLISHED, pageable)
                .map(this::mapToResponse);

        return PageResponse.from(result);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        courseRepository.delete(course);
    }

    // converts "Intro to Java" → "intro-to-java"
    private String generateSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");

        // ensure slug is unique by appending a number if needed
        String slug = baseSlug;
        int counter = 1;
        while (courseRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    private CourseResponse mapToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .slug(course.getSlug())
                .status(course.getStatus())
                .price(course.getPrice())
                .instructorName(
                        course.getInstructor().getFirstName() + " " +
                                course.getInstructor().getLastName()
                )
                .createdAt(course.getCreatedAt())
                .build();
    }

    public CourseResponse publishCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setStatus(CourseStatus.PUBLISHED);
        return mapToResponse(courseRepository.save(course));
    }
}