package com.example.lms_backend.dto.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDashboardResponse(
        int totalActiveClasses,
        int totalCompletedClasses,
        List<ActiveClassSummary> activeClasses) {

    public record ActiveClassSummary(
            UUID classId,
            String code,
            String semester,
            String courseName,
            String teacherName,
            Instant enrolledAt) {
    }
}
