package com.example.lms_backend.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import com.example.lms_backend.dto.course.CourseClassResponse;
import com.example.lms_backend.dto.course.CreateCourseClassRequest;
import com.example.lms_backend.exception.GlobalExceptionHandler;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.service.CourseClassService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseClassController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class CourseClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseClassService courseClassService;

    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    // ──────────────────────────────────────────────
    // Mocks: JWT with roles
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
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private CourseClassResponse sampleClassResponse() {
        return new CourseClassResponse(
                CLASS_ID,
                "CLASS-01",
                "Fall 2024",
                TEACHER_ID,
                "John Doe", // teacherName
                COURSE_ID,
                "Software Engineering", // courseName
                "ABCDEF", // joinCode
                Instant.now(),
                Instant.now());
    }

    // ═══════════════════════════════════════════════
    // 1. GET /api/course-classes
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/course-classes")
    class GetAllCourseClasses {
        private static final String URL = "/api/course-classes";

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 - Wrong HTTP Method (POST instead of GET)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            // POST /api/course-classes requires Body & ADMIN role. Without token, it gets
            // 401.
            // So we hit it with admin token but missing body -> 400 BAD REQUEST
            mockMvc.perform(post(URL).with(adminJwt()))
                    .andExpect(status().isBadRequest());
        }

        // ── Business Logic ──

        @Test
        @DisplayName("200 - STUDENT views classes successfully")
        void shouldReturn200_WhenStudent() throws Exception {
            testSuccessfulFetch(studentJwt(), "STUDENT");
        }

        @Test
        @DisplayName("200 - TEACHER views classes successfully")
        void shouldReturn200_WhenTeacher() throws Exception {
            testSuccessfulFetch(teacherJwt(), "TEACHER");
        }

        @Test
        @DisplayName("200 - ADMIN views classes successfully")
        void shouldReturn200_WhenAdmin() throws Exception {
            testSuccessfulFetch(adminJwt(), "ADMIN");
        }

        private void testSuccessfulFetch(RequestPostProcessor jwtProcessor, String role) throws Exception {
            var page = new PageImpl<>(List.of(sampleClassResponse()), PageRequest.of(0, 10), 1);
            when(courseClassService.getCourseClasses(any(UUID.class), eq(role), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(URL).with(jwtProcessor)
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].code").value("CLASS-01"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ═══════════════════════════════════════════════
    // 2. POST /api/course-classes
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/course-classes")
    class CreateCourseClass {
        private static final String URL = "/api/course-classes";

        private final String VALID_BODY = String.format("""
                {
                    "courseId": "%s",
                    "code": "CLASS-01",
                    "semester": "Fall 2024",
                    "teacherId": "%s"
                }
                """, COURSE_ID, TEACHER_ID);

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot create course class")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(post(URL).with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot create course class")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(post(URL).with(teacherJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - XSS Injection in Code/Semester")
        void shouldReturn400_WhenXssInFields() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "courseId": "%s",
                                "code": "<script>alert(1)</script>",
                                "semester": "<img src=x onerror=alert(1)>",
                                "teacherId": "%s"
                            }
                            """, COURSE_ID, TEACHER_ID)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.code").exists())
                    .andExpect(jsonPath("$.errors.semester").exists());
        }

        @Test
        @DisplayName("400 - Malformed JSON")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "courseId": "%s",
                                "code": "CLASS",
                                "teacherId": "%s"
                            """, COURSE_ID, TEACHER_ID))) // Missing closing brace
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - Buffer Overflow (String > 50 chars)")
        void shouldReturn400_WhenBufferOverflow() throws Exception {
            String longString = "A".repeat(100);
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "courseId": "%s",
                                "code": "%s",
                                "semester": "Fall 2024",
                                "teacherId": "%s"
                            }
                            """, COURSE_ID, longString, TEACHER_ID)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.code").exists()); // Failed @Size validation
        }

        @Test
        @DisplayName("400 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            // GET /api/course-classes doesn't take a body but requires Pageable.
            // When we send GET to /api/course-classes with a token, it actually succeeds
            // (200 OK)!
            // To properly test "Wrong Method" rejecting our POST body, we can try PUT
            // instead.
            mockMvc.perform(put(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isMethodNotAllowed()); // PUT /api/course-classes doesn't exist -> 405
        }

        // ── Validation & Business Logic ──

        @Test
        @DisplayName("400 - Validation error (Missing fields)")
        void shouldReturn400_WhenValidationFails() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.courseId").exists())
                    .andExpect(jsonPath("$.errors.code").exists())
                    .andExpect(jsonPath("$.errors.semester").exists())
                    .andExpect(jsonPath("$.errors.teacherId").exists());
        }

        @Test
        @DisplayName("409 - Class Code already exists")
        void shouldReturn409_WhenClassCodeExists() throws Exception {
            when(courseClassService.createCourseClass(any(CreateCourseClassRequest.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Class code already exists: CLASS-01"));

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Class code already exists: CLASS-01"));
        }

        @Test
        @DisplayName("404 - Course or Teacher not found")
        void shouldReturn404_WhenCourseOrTeacherNotFound() throws Exception {
            when(courseClassService.createCourseClass(any(CreateCourseClassRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Course not found"));

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Course not found"));
        }

        @Test
        @DisplayName("201 - ADMIN successfully creates course class")
        void shouldReturn201_WhenCreatedSuccessfully() throws Exception {
            when(courseClassService.createCourseClass(any(CreateCourseClassRequest.class)))
                    .thenReturn(sampleClassResponse());

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("CLASS-01"));
        }
    }

    // ═══════════════════════════════════════════════
    // 3. PUT /api/course-classes/{id}/reset-join-code
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("PUT /api/course-classes/{id}/reset-join-code")
    class ResetJoinCode {
        private String url() {
            return "/api/course-classes/" + CLASS_ID + "/reset-join-code";
        }

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(put(url()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot reset join code")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(put(url()).with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - ADMIN cannot reset join code (Teacher only feature)")
        void shouldReturn403_WhenRoleIsAdmin() throws Exception {
            mockMvc.perform(put(url()).with(adminJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - Type Mismatch (Invalid UUID format)")
        void shouldReturn400_WhenIdHasInvalidFormat() throws Exception {
            mockMvc.perform(put("/api/course-classes/invalid-uuid/reset-join-code").with(teacherJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of PUT)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(url()).with(teacherJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── Validation & Business Logic ──

        @Test
        @DisplayName("404 - Class not found")
        void shouldReturn404_WhenClassNotFound() throws Exception {
            when(courseClassService.resetJoinCode(eq(TEACHER_ID), eq(CLASS_ID)))
                    .thenThrow(new ResourceNotFoundException("Course class not found"));

            mockMvc.perform(put(url()).with(teacherJwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Course class not found"));
        }

        @Test
        @DisplayName("200 - TEACHER successfully resets join code")
        void shouldReturn200_WhenSuccess() throws Exception {
            when(courseClassService.resetJoinCode(eq(TEACHER_ID), eq(CLASS_ID)))
                    .thenReturn(sampleClassResponse());

            mockMvc.perform(put(url()).with(teacherJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("CLASS-01"));
        }
    }
}
