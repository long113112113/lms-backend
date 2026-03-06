package com.example.lms_backend.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.lms_backend.config.SecurityConfig;
import com.example.lms_backend.dto.dashboard.TeacherDashboardResponse;
import com.example.lms_backend.exception.GlobalExceptionHandler;
import com.example.lms_backend.service.DashboardService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final String URL = "/api/dashboard/teacher";

    private static RequestPostProcessor teacherJwt() {
        return jwt().jwt(j -> j.subject(TEACHER_ID.toString()).claim("role", "TEACHER"))
                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"));
    }

    private static RequestPostProcessor studentJwt() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    private static RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private TeacherDashboardResponse sampleResponse() {
        return new TeacherDashboardResponse(
                2,
                1,
                List.of(new TeacherDashboardResponse.ClassSummary(
                        CLASS_ID, "CS101-A", "Fall 2024", "Intro to CS", 15),
                        new TeacherDashboardResponse.ClassSummary(
                                UUID.randomUUID(), "CS102-A", "Fall 2024", "Data Structures", 22)));
    }

    // ═══════════════════════════════════════════════
    // GET /api/dashboard/teacher
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/dashboard/teacher")
    class GetTeacherDashboard {

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot access teacher dashboard")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(get(URL).with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - ADMIN cannot access teacher dashboard")
        void shouldReturn403_WhenRoleIsAdmin() throws Exception {
            mockMvc.perform(get(URL).with(adminJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of GET)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(URL).with(teacherJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("200 - TEACHER successfully retrieves dashboard")
        void shouldReturn200_WhenTeacher() throws Exception {
            when(dashboardService.getTeacherDashboard(any(UUID.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(get(URL).with(teacherJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActiveClasses").value(2))
                    .andExpect(jsonPath("$.totalCompletedClasses").value(1))
                    .andExpect(jsonPath("$.classes.length()").value(2))
                    .andExpect(jsonPath("$.classes[0].code").value("CS101-A"))
                    .andExpect(jsonPath("$.classes[0].activeStudentCount").value(15));
        }

        @Test
        @DisplayName("200 - TEACHER with no classes returns empty dashboard")
        void shouldReturn200_WhenTeacherHasNoClasses() throws Exception {
            when(dashboardService.getTeacherDashboard(any(UUID.class)))
                    .thenReturn(new TeacherDashboardResponse(0, 0, List.of()));

            mockMvc.perform(get(URL).with(teacherJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActiveClasses").value(0))
                    .andExpect(jsonPath("$.totalCompletedClasses").value(0))
                    .andExpect(jsonPath("$.classes.length()").value(0));
        }
    }
}
