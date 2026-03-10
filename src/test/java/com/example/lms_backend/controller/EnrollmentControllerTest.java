package com.example.lms_backend.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.lms_backend.config.SecurityConfig;
import com.example.lms_backend.dto.enrollment.EnrollmentResponse;
import com.example.lms_backend.exception.AccessDeniedException;
import com.example.lms_backend.exception.GlobalExceptionHandler;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.service.EnrollmentService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    // ──────────────────────────────────────────────
    // Helpers: JWT mock with role-based authorities
    // ──────────────────────────────────────────────
    private static RequestPostProcessor studentJwt() {
        return jwt().jwt(j -> j.subject(STUDENT_ID.toString()).claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    private static RequestPostProcessor teacherJwt() {
        return jwt().jwt(j -> j.subject(TEACHER_ID.toString()).claim("role", "TEACHER"))
                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"));
    }

    private static RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject(ADMIN_ID.toString()).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private EnrollmentResponse sampleResponse() {
        return new EnrollmentResponse(
                ENROLLMENT_ID,
                STUDENT_ID,
                "Nguyen Van A",
                "student@example.com",
                CLASS_ID,
                "CS101-A",
                "Intro to CS",
                "Fall 2026",
                "Dr. Teacher",
                "ACTIVE",
                Instant.now());
    }

    // ═══════════════════════════════════════════════
    // 1. POST /api/enrollments/join
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/enrollments/join")
    class JoinClass {

        private static final String URL = "/api/enrollments/join";
        private static final String VALID_BODY = """
                { "joinCode": "ABC1234" }
                """;

        // ── Security ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - ADMIN cannot join a class")
        void shouldReturn403_WhenRoleIsAdmin() throws Exception {
            mockMvc.perform(post(URL)
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot join a class")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(post(URL)
                    .with(teacherJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        // ── Validation & Malicious Payloads ──

        @Test
        @DisplayName("400 - Join code is blank")
        void shouldReturn400_WhenJoinCodeIsBlank() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": "" }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode").exists());

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Join code is missing")
        void shouldReturn400_WhenJoinCodeIsMissing() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode").value("Join code is required"));

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Join code is null")
        void shouldReturn400_WhenJoinCodeIsNull() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": null }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode").value("Join code is required"));

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Malformed JSON (Missing brace)")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": "ABC1234"
                            """)) // Missing closing brace
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - XSS Injection in Join Code")
        void shouldReturn400_WhenXssInJoinCode() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": "<script>alert('xss')</script>" }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode").exists());

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Join code has invalid format")
        void shouldReturn400_WhenJoinCodeHasInvalidFormat() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": "abc1234" }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode")
                            .value("Join code must be exactly 7 uppercase letters or digits"));

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Join code has invalid length")
        void shouldReturn400_WhenJoinCodeHasInvalidLength() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": "ABC12345" }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode")
                            .value("Join code must be exactly 7 uppercase letters or digits"));

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(get(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("400 - Buffer Overflow (Extremely long join code)")
        void shouldReturn400_WhenJoinCodeIsTooLong() throws Exception {
            String longString = "A".repeat(10000);

            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            { "joinCode": "%s" }
                            """, longString)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.joinCode")
                            .value("Join code must be exactly 7 uppercase letters or digits"));

            verifyNoInteractions(enrollmentService);
        }

        // ── Business Logic ──

        @Test
        @DisplayName("404 - Invalid join code")
        void shouldReturn404_WhenJoinCodeInvalid() throws Exception {
            when(enrollmentService.joinClass(any(UUID.class), eq("INVALID")))
                    .thenThrow(new ResourceNotFoundException("Invalid join code"));

            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "joinCode": "INVALID" }
                            """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Invalid join code"));
        }

        @Test
        @DisplayName("409 - Student has already joined this class")
        void shouldReturn409_WhenAlreadyJoined() throws Exception {
            when(enrollmentService.joinClass(any(UUID.class), eq("ABC1234")))
                    .thenThrow(new ResourceAlreadyExistsException("You have already joined this class"));

            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("You have already joined this class"));
        }

        @Test
        @DisplayName("403 - Student was kicked from this class")
        void shouldReturn403_WhenStudentWasKicked() throws Exception {
            when(enrollmentService.joinClass(any(UUID.class), eq("ABC1234")))
                    .thenThrow(new AccessDeniedException("You have been removed from this class"));

            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You have been removed from this class"));
        }

        @Test
        @DisplayName("201 - Successfully join a class")
        void shouldReturn201_WhenJoinSuccessful() throws Exception {
            when(enrollmentService.joinClass(any(UUID.class), eq("ABC1234")))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(URL)
                    .with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.enrollmentId").value(ENROLLMENT_ID.toString()))
                    .andExpect(jsonPath("$.studentName").value("Nguyen Van A"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    // ═══════════════════════════════════════════════
    // 2. DELETE /api/enrollments/{id}/leave
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/enrollments/{id}/leave")
    class LeaveClass {

        private String url() {
            return "/api/enrollments/" + CLASS_ID + "/leave";
        }

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(delete(url()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - TEACHER cannot leave a class")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(delete(url())
                    .with(teacherJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - Type Mismatch (Invalid UUID format)")
        void shouldReturn400_WhenIdHasInvalidFormat() throws Exception {
            mockMvc.perform(delete("/api/enrollments/invalid-uuid/leave")
                    .with(studentJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists()); // Should be caught by GlobalExceptionHandler
                                                                // MethodArgumentTypeMismatchException
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (GET instead of DELETE)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(get(url())
                    .with(studentJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── Business Logic ──

        @Test
        @DisplayName("404 - Enrollment not found")
        void shouldReturn404_WhenEnrollmentNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Enrollment not found"))
                    .when(enrollmentService).leaveClass(any(UUID.class), eq(CLASS_ID));

            mockMvc.perform(delete(url())
                    .with(studentJwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Enrollment not found"));
        }

        @Test
        @DisplayName("204 - Successfully leave a class")
        void shouldReturn204_WhenLeaveSuccessful() throws Exception {
            doNothing().when(enrollmentService).leaveClass(any(UUID.class), eq(CLASS_ID));

            mockMvc.perform(delete(url())
                    .with(studentJwt()))
                    .andExpect(status().isNoContent());
        }
    }

    // ═══════════════════════════════════════════════
    // 3. DELETE /api/enrollments/{id}/kick/{studentId}
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/enrollments/{id}/kick/{studentId}")
    class KickStudent {

        private String url() {
            return "/api/enrollments/" + CLASS_ID + "/kick/" + STUDENT_ID;
        }

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(delete(url()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot kick another student")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(delete(url())
                    .with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - ADMIN cannot kick (only TEACHER)")
        void shouldReturn403_WhenRoleIsAdmin() throws Exception {
            mockMvc.perform(delete(url())
                    .with(adminJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - Type Mismatch (Invalid UUID format for studentId)")
        void shouldReturn400_WhenStudentIdHasInvalidFormat() throws Exception {
            mockMvc.perform(delete("/api/enrollments/" + CLASS_ID + "/kick/not-a-uuid")
                    .with(teacherJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of DELETE)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(url())
                    .with(teacherJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── Business Logic ──

        @Test
        @DisplayName("404 - Class not found")
        void shouldReturn404_WhenClassNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Class not found"))
                    .when(enrollmentService).kickStudent(any(UUID.class), eq(CLASS_ID), eq(STUDENT_ID));

            mockMvc.perform(delete(url())
                    .with(teacherJwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Class not found"));
        }

        @Test
        @DisplayName("403 - Teacher does not own this class")
        void shouldReturn403_WhenTeacherDoesNotOwnClass() throws Exception {
            doThrow(new AccessDeniedException("You are not the teacher of this class"))
                    .when(enrollmentService).kickStudent(any(UUID.class), eq(CLASS_ID), eq(STUDENT_ID));

            mockMvc.perform(delete(url())
                    .with(teacherJwt()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You are not the teacher of this class"));
        }

        @Test
        @DisplayName("204 - Successfully kick a student")
        void shouldReturn204_WhenKickSuccessful() throws Exception {
            doNothing().when(enrollmentService).kickStudent(any(UUID.class), eq(CLASS_ID), eq(STUDENT_ID));

            mockMvc.perform(delete(url())
                    .with(teacherJwt()))
                    .andExpect(status().isNoContent());
        }
    }

    // ═══════════════════════════════════════════════
    // 4. GET /api/enrollments/my-classes
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/enrollments/my-classes")
    class GetMyEnrollments {

        private static final String URL = "/api/enrollments/my-classes";

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - TEACHER cannot view student enrollments")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(get(URL)
                    .with(teacherJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of GET)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(URL)
                    .with(studentJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── Business Logic ──

        @Test
        @DisplayName("200 - Returns empty list when student has no enrollments")
        void shouldReturn200WithEmptyList_WhenNoEnrollments() throws Exception {
            when(enrollmentService.getMyEnrollments(any(UUID.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get(URL)
                    .with(studentJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 - Returns enrolled classes list")
        void shouldReturn200WithData_WhenEnrollmentsExist() throws Exception {
            when(enrollmentService.getMyEnrollments(any(UUID.class)))
                    .thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get(URL)
                    .with(studentJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].enrollmentId").value(ENROLLMENT_ID.toString()))
                    .andExpect(jsonPath("$[0].studentName").value("Nguyen Van A"))
                    .andExpect(jsonPath("$[0].courseClassCode").value("CS101-A"));
        }
    }

    // ═══════════════════════════════════════════════
    // 5. GET /api/enrollments/class/{id}/students
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/enrollments/class/{id}/students")
    class GetClassStudents {

        private String url() {
            return "/api/enrollments/class/" + CLASS_ID + "/students";
        }

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(url()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot view class students")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(get(url())
                    .with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - Type Mismatch (Invalid UUID format)")
        void shouldReturn400_WhenIdHasInvalidFormat() throws Exception {
            mockMvc.perform(get("/api/enrollments/class/invalid-uuid/students")
                    .with(teacherJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of GET)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(url())
                    .with(teacherJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── Business Logic ──

        @Test
        @DisplayName("404 - Class not found")
        void shouldReturn404_WhenClassNotFound() throws Exception {
            when(enrollmentService.getClassStudents(eq(CLASS_ID), any(UUID.class), eq("ADMIN"), isNull(),
                    any(Pageable.class)))
                    .thenThrow(new ResourceNotFoundException("Class not found"));

            mockMvc.perform(get(url())
                    .with(adminJwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Class not found"));
        }

        @Test
        @DisplayName("403 - Teacher does not own this class")
        void shouldReturn403_WhenTeacherDoesNotOwnClass() throws Exception {
            when(enrollmentService.getClassStudents(eq(CLASS_ID), any(UUID.class), eq("TEACHER"), isNull(),
                    any(Pageable.class)))
                    .thenThrow(new AccessDeniedException("You are not the teacher of this class"));

            mockMvc.perform(get(url())
                    .with(teacherJwt()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You are not the teacher of this class"));
        }

        @Test
        @DisplayName("200 - ADMIN views class with no students")
        void shouldReturn200WithEmptyPage_WhenAdminViewsEmptyClass() throws Exception {
            when(enrollmentService.getClassStudents(eq(CLASS_ID), any(UUID.class), eq("ADMIN"), isNull(),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get(url())
                    .param("page", "0")
                    .param("size", "10")
                    .with(adminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        @DisplayName("200 - TEACHER views class with enrolled students")
        void shouldReturn200WithPageMetadata_WhenTeacherOwnsClass() throws Exception {
            when(enrollmentService.getClassStudents(eq(CLASS_ID), any(UUID.class), eq("TEACHER"), eq("nguyen"),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get(url())
                    .param("page", "0")
                    .param("size", "10")
                    .param("q", "nguyen")
                    .with(teacherJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].studentEmail").value("student@example.com"))
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("400 - Invalid search query from XSS payload")
        void shouldReturn400_WhenSearchQueryContainsUnsafeHtml() throws Exception {
            mockMvc.perform(get(url())
                    .param("q", "<script>alert('xss')</script>")
                    .with(adminJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.q").exists());

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Search query exceeds max length")
        void shouldReturn400_WhenSearchQueryTooLong() throws Exception {
            mockMvc.perform(get(url())
                    .param("q", "A".repeat(101))
                    .with(adminJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.q").value("Search query must not exceed 100 characters"));

            verifyNoInteractions(enrollmentService);
        }

        @Test
        @DisplayName("400 - Unsupported sort field")
        void shouldReturn400_WhenSortFieldIsUnsupported() throws Exception {
            when(enrollmentService.getClassStudents(eq(CLASS_ID), any(UUID.class), eq("ADMIN"), isNull(),
                    any(Pageable.class)))
                    .thenThrow(new IllegalArgumentException("Unsupported sort field: hacked"));

            mockMvc.perform(get(url())
                    .param("sort", "hacked,asc")
                    .with(adminJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Unsupported sort field: hacked"));
        }

        @Test
        @DisplayName("200 - Controller caps page size at 100")
        void shouldCapPageSizeAt100() throws Exception {
            when(enrollmentService.getClassStudents(eq(CLASS_ID), any(UUID.class), eq("ADMIN"), isNull(),
                    any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

            mockMvc.perform(get(url())
                    .param("page", "0")
                    .param("size", "500")
                    .with(adminJwt()))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(enrollmentService).getClassStudents(eq(CLASS_ID), any(UUID.class), eq("ADMIN"), isNull(),
                    pageableCaptor.capture());
            org.junit.jupiter.api.Assertions.assertEquals(100, pageableCaptor.getValue().getPageSize());
        }
    }
}
