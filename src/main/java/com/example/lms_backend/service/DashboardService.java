package com.example.lms_backend.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.dashboard.TeacherDashboardResponse;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.EnrollmentRepository;

@Service
public class DashboardService {

    private final CourseClassRepository courseClassRepository;
    private final EnrollmentRepository enrollmentRepository;

    public DashboardService(CourseClassRepository courseClassRepository,
            EnrollmentRepository enrollmentRepository) {
        this.courseClassRepository = courseClassRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(UUID teacherId) {
        // 1. Fetch all classes taught by this teacher (active + deleted)
        List<CourseClass> allClasses = courseClassRepository.findAllByTeacherId(teacherId);

        // 2. Split into active and completed (soft-deleted)
        List<CourseClass> activeClasses = allClasses.stream()
                .filter(c -> !c.isDeleted())
                .toList();
        int totalCompletedClasses = (int) allClasses.stream()
                .filter(CourseClass::isDeleted)
                .count();

        // 3. Batch-fetch active student counts for active classes only
        List<UUID> activeClassIds = activeClasses.stream()
                .map(CourseClass::getId)
                .toList();

        Map<UUID, Long> studentCountMap = activeClassIds.isEmpty()
                ? Map.of()
                : enrollmentRepository.countActiveStudentsByClassIds(activeClassIds)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (UUID) row[0],
                                row -> (Long) row[1]));

        // 4. Map to response
        List<TeacherDashboardResponse.ClassSummary> classSummaries = activeClasses.stream()
                .map(c -> new TeacherDashboardResponse.ClassSummary(
                        c.getId(),
                        c.getCode(),
                        c.getSemester(),
                        c.getCourse().getName(),
                        studentCountMap.getOrDefault(c.getId(), 0L).intValue()))
                .toList();

        return new TeacherDashboardResponse(
                activeClasses.size(),
                totalCompletedClasses,
                classSummaries);
    }
}
