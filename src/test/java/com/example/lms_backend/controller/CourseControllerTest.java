package com.example.lms_backend.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.lms_backend.config.SecurityConfig;
import com.example.lms_backend.dto.course.CourseRequest;
import com.example.lms_backend.dto.course.CourseResponse;
import com.example.lms_backend.exception.GlobalExceptionHandler;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.service.CourseService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

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
        return jwt().jwt(j -> j.subject(ADMIN_ID.toString()).claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private CourseResponse sampleCourseResponse() {
        return new CourseResponse(
                COURSE_ID,
                "CS101",
                "Intro to CS",
                3,
                "Basic programming concepts",
                Instant.now(),
                Instant.now());
    }

    // ═══════════════════════════════════════════════
    // 1. GET /api/courses
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/courses")
    class GetAllCourses {
        private static final String URL = "/api/courses";

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 - STUDENT can view courses with pagination")
        void shouldReturn200_WhenRoleIsStudent() throws Exception {
            var page = new PageImpl<>(List.of(sampleCourseResponse()), PageRequest.of(0, 10), 1);
            when(courseService.getAllCourses(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(URL).with(studentJwt())
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].code").value("CS101"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("200 - TEACHER can view courses")
        void shouldReturn200_WhenRoleIsTeacher() throws Exception {
            var page = new PageImpl<>(List.of(sampleCourseResponse()), PageRequest.of(0, 10), 1);
            when(courseService.getAllCourses(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(URL).with(teacherJwt())
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("200 - ADMIN can view courses")
        void shouldReturn200_WhenRoleIsAdmin() throws Exception {
            var page = new PageImpl<>(List.of(sampleCourseResponse()), PageRequest.of(0, 10), 1);
            when(courseService.getAllCourses(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(URL).with(adminJwt())
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("200 - Filter courses by code")
        void shouldReturn200_WhenFilterByCode() throws Exception {
            var page = new PageImpl<>(List.of(sampleCourseResponse()), PageRequest.of(0, 10), 1);
            when(courseService.getAllCourses(eq("CS"), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(URL).with(studentJwt())
                    .param("code", "CS")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].code").value("CS101"));
        }
    }

    // ═══════════════════════════════════════════════
    // 2. POST /api/courses (createCourse)
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/courses")
    class CreateCourse {
        private static final String URL = "/api/courses";

        private static final String VALID_BODY = """
                {
                    "code": "CS101",
                    "name": "Intro to CS",
                    "credits": 3,
                    "description": "Basic programming concepts"
                }
                """;

        // ── I. Security & RBAC Cơ Bản ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot create course")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(post(URL).with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot create course")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(post(URL).with(teacherJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (PUT instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(put(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── II. Malicious Payloads (Hacker cố tình phá) ──

        @Test
        @DisplayName("400 - XSS Injection in Course Code")
        void shouldReturn400_WhenXssInCode() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "code": "<script>alert(1)</script>",
                                "name": "Intro to CS",
                                "credits": 3
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.code").exists()); // @SafeHtml prevents this
        }

        @Test
        @DisplayName("400 - XSS Injection in Name/Description")
        void shouldReturn400_WhenXssInNameDesc() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "code": "CS102",
                                "name": "<img src=x onerror=alert(1)>",
                                "credits": 3,
                                "description": "javascript:alert('XSS')"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    // Depending on the exact SafeHtml logic, some payloads might be sanitized or
                    // rejected.
                    // The 'name' field definitely fails validation with this payload.
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("400 - Malformed JSON (Syntax Error)")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "code": "CS101",
                                "name": "Intro to CS",
                                "credits": 3,
                            """)) // Missing closing brace
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - Type Mismatch (String instead of Integer for credits)")
        void shouldReturn400_WhenTypeMismatch() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "code": "CS101",
                                "name": "Intro to CS",
                                "credits": "ABC",
                                "description": "Desc"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - Buffer Overflow (Extremely long string, > 10000 chars)")
        void shouldReturn400_WhenBufferOverflow() throws Exception {
            String extremelyLongString = "A".repeat(15000); // Exceeds default length/bounds typically

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "code": "%s",
                                "name": "Intro to CS",
                                "credits": 3
                            }
                            """, extremelyLongString)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.code").exists());
        }

        // ── III. Validation Nghiệp vụ & Thành công ──

        @Test
        @DisplayName("400 - Missing required fields")
        void shouldReturn400_WhenMissingRequiredFields() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.code").exists())
                    .andExpect(jsonPath("$.errors.name").exists())
                    .andExpect(jsonPath("$.errors.credits").exists());
        }

        @Test
        @DisplayName("400 - Invalid credits (Zero or Negative)")
        void shouldReturn400_WhenCreditsInvalid() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "code": "CS101",
                                "name": "Intro to CS",
                                "credits": 0,
                                "description": "Basic concept"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.credits").exists());
        }

        @Test
        @DisplayName("409 - Course code already exists")
        void shouldReturn409_WhenCourseCodeExists() throws Exception {
            when(courseService.createCourse(any(CourseRequest.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Course code already exists: CS101"));

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Course code already exists: CS101"));
        }

        @Test
        @DisplayName("201 - ADMIN successfully creates a course")
        void shouldReturn201_WhenCreatedSuccessfully() throws Exception {
            when(courseService.createCourse(any(CourseRequest.class)))
                    .thenReturn(sampleCourseResponse());

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("CS101"))
                    .andExpect(jsonPath("$.credits").value(3));
        }
    }

    // ═══════════════════════════════════════════════
    // 3. PUT /api/courses/{id} (updateCourse)
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("PUT /api/courses/{id}")
    class UpdateCourse {
        private static final String URL = "/api/courses/{id}";

        private static final String VALID_BODY = """
                {
                    "code": "CS101",
                    "name": "Intro to CS Updated",
                    "credits": 4,
                    "description": "Updated concepts"
                }
                """;

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(put(URL, COURSE_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot update course")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(put(URL, COURSE_ID).with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 - ADMIN successfully updates a course")
        void shouldReturn200_WhenUpdatedSuccessfully() throws Exception {
            when(courseService.updateCourse(eq(COURSE_ID), any(CourseRequest.class)))
                    .thenReturn(sampleCourseResponse());

            mockMvc.perform(put(URL, COURSE_ID).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("CS101"));
        }
    }

    // ═══════════════════════════════════════════════
    // 4. DELETE /api/courses/{id} (deleteCourse)
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/courses/{id}")
    class DeleteCourse {
        private static final String URL = "/api/courses/{id}";

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(delete(URL, COURSE_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot delete course")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(delete(URL, COURSE_ID).with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("204 - ADMIN successfully deletes a course")
        void shouldReturn204_WhenDeletedSuccessfully() throws Exception {
            doNothing().when(courseService).deleteCourse(COURSE_ID);

            mockMvc.perform(delete(URL, COURSE_ID).with(adminJwt()))
                    .andExpect(status().isNoContent());
        }
    }
}
