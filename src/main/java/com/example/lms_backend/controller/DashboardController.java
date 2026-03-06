package com.example.lms_backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms_backend.dto.dashboard.TeacherDashboardResponse;
import com.example.lms_backend.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher")
    public ResponseEntity<TeacherDashboardResponse> getTeacherDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        UUID teacherId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(dashboardService.getTeacherDashboard(teacherId));
    }
}
