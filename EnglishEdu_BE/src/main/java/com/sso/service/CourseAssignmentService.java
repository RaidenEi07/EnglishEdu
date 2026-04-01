package com.sso.service;

import com.sso.dto.request.AssignCoursesRequest;
import com.sso.dto.response.CourseAssignmentResponse;
import com.sso.dto.response.CourseResponse;
import com.sso.entity.Course;
import com.sso.entity.CourseAssignment;
import com.sso.entity.User;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.mapper.CourseMapper;
import com.sso.repository.CourseAssignmentRepository;
import com.sso.repository.CourseRepository;
import com.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseAssignmentService {

    private final CourseAssignmentRepository courseAssignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;
    private final NotificationPushService notificationPushService;

    public boolean isAssigned(Long userId, Long courseId) {
        return courseAssignmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Transactional(readOnly = true)
    public List<CourseAssignmentResponse> getAssignmentsByUser(Long userId) {
        return courseAssignmentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseAssignmentResponse> getAssignmentsByCourse(Long courseId) {
        return courseAssignmentRepository.findByCourseId(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAssignedCourses(Long userId) {
        List<Long> courseIds = courseAssignmentRepository.findCourseIdsByUserId(userId);
        if (courseIds.isEmpty()) return List.of();
        return courseRepository.findAllById(courseIds).stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<CourseAssignmentResponse> assignCourses(AssignCoursesRequest request, Long adminId) {
        User student = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new BadRequestException("Chỉ có thể chỉ định khóa học cho tài khoản học sinh.");
        }

        User admin = userRepository.getReferenceById(adminId);
        List<CourseAssignmentResponse> results = new ArrayList<>();

        for (Long courseId : request.getCourseIds()) {
            if (courseAssignmentRepository.existsByUserIdAndCourseId(student.getId(), courseId)) {
                continue; // already assigned, skip
            }
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

            CourseAssignment assignment = CourseAssignment.builder()
                    .user(student)
                    .course(course)
                    .assignedBy(admin)
                    .build();
            assignment = courseAssignmentRepository.save(assignment);
            results.add(toResponse(assignment));

            notificationPushService.sendNotification(student.getId(),
                    "Bạn đã được chỉ định khóa học: " + course.getName(),
                    null,
                    "ENROLLMENT");
        }
        return results;
    }

    @Transactional
    public void unassignCourse(Long userId, Long courseId) {
        if (!courseAssignmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ResourceNotFoundException("Assignment not found");
        }
        courseAssignmentRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    private CourseAssignmentResponse toResponse(CourseAssignment a) {
        User student = a.getUser();
        Course course = a.getCourse();
        User assignedBy = a.getAssignedBy();
        return CourseAssignmentResponse.builder()
                .id(a.getId())
                .userId(student.getId())
                .username(student.getUsername())
                .studentFullName(student.getFullName())
                .courseId(course.getId())
                .courseName(course.getName())
                .courseCategory(course.getCategory())
                .assignedByUsername(assignedBy != null ? assignedBy.getUsername() : null)
                .assignedAt(a.getAssignedAt())
                .build();
    }
}
