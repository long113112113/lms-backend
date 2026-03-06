package com.example.lms_backend.dto.dashboard;

import java.util.List;
import java.util.UUID;

public record TeacherDashboardResponse(
        int totalActiveClasses,
        int totalCompletedClasses,
        List<ClassSummary> classes) {

    public record ClassSummary(
            UUID classId,
            String code,
            String semester,
            String courseName,
            int activeStudentCount) {
    }
}
