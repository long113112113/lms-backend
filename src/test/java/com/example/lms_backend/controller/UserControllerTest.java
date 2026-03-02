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
import com.example.lms_backend.dto.user.CreateUserRequest;
import com.example.lms_backend.dto.user.CreateUserWithRoleRequest;
import com.example.lms_backend.dto.user.UpdateUserRequest;
import com.example.lms_backend.dto.user.UserResponse;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.exception.GlobalExceptionHandler;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.service.UserService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

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

    private UserResponse sampleUserResponse(Role role) {
        return new UserResponse(
                USER_ID,
                "test@example.com",
                "Test User",
                role,
                Instant.now(),
                Instant.now());
    }

    // ═══════════════════════════════════════════════
    // 1. GET /api/users
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsers {
        private static final String URL = "/api/users";

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot view all users")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(get(URL).with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot view all users")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(get(URL).with(teacherJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 - ADMIN can view list of users")
        void shouldReturn200_WhenRoleIsAdmin() throws Exception {
            var page = new PageImpl<>(List.of(sampleUserResponse(Role.STUDENT)), PageRequest.of(0, 10), 1);
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(URL).with(adminJwt())
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("400 - Wrong HTTP Method (POST instead of GET)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt()))
                    .andExpect(status().isBadRequest()); // Routes to POST /api/users lacking body -> 400
        }
    }

    // ═══════════════════════════════════════════════
    // 2. GET /api/users/{id}
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserById {
        private String url() {
            return "/api/users/" + USER_ID;
        }

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(get(url()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot view user details")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(get(url()).with(studentJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot view user details")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(get(url()).with(teacherJwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("404 - User not found")
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(userService.getUserById(USER_ID))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get(url()).with(adminJwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("200 - ADMIN views successful")
        void shouldReturn200_WhenUserFound() throws Exception {
            when(userService.getUserById(USER_ID))
                    .thenReturn(sampleUserResponse(Role.STUDENT));

            mockMvc.perform(get(url()).with(adminJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("400 - Type Mismatch (Invalid UUID format)")
        void shouldReturn400_WhenIdHasInvalidFormat() throws Exception {
            mockMvc.perform(get("/api/users/invalid-uuid").with(adminJwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of GET)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(url()).with(adminJwt()))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ═══════════════════════════════════════════════
    // 3. POST /api/users (createUser)
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {
        private static final String URL = "/api/users";

        @Test
        @DisplayName("400 - Validation errors (Blank email/pass)")
        void shouldReturn400_WhenValidationFails() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "",
                                "password": "",
                                "fullName": ""
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.password").exists())
                    .andExpect(jsonPath("$.errors.fullName").exists());
        }

        @Test
        @DisplayName("400 - Invalid Email Format")
        void shouldReturn400_WhenEmailInvalidFormat() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "not-an-email",
                                "password": "password123",
                                "fullName": "Test User"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").value("Email format is invalid"));
        }

        @Test
        @DisplayName("409 - Email already exists")
        void shouldReturn409_WhenEmailExists() throws Exception {
            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Email already exists"));

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "test@example.com",
                                "password": "password123",
                                "fullName": "Test User"
                            }
                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("201 - Successfully create user (Public endpoint)")
        void shouldReturn201_WhenCreatedSuccessfully() throws Exception {
            UserResponse mockResponse = sampleUserResponse(Role.STUDENT);
            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenReturn(mockResponse);

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "test@example.com",
                                "password": "password123",
                                "fullName": "Test User"
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.role").value("STUDENT"));
        }

        // ── Malicious Payloads ──

        @Test
        @DisplayName("400 - XSS Injection in Email/FullName")
        void shouldReturn400_WhenXssInFields() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "<script>alert('xss')</script>@example.com",
                                "password": "password123",
                                "fullName": "<img src=x onerror=alert(1)>"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.fullName").exists());
        }

        @Test
        @DisplayName("400 - Malformed JSON")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "test@example.com",
                                "password": "password123"
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - Buffer Overflow (String > 100 chars)")
        void shouldReturn400_WhenBufferOverflow() throws Exception {
            String longString = "A".repeat(150);
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "email": "test@example.com",
                                "password": "password123",
                                "fullName": "%s"
                            }
                            """, longString)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fullName").exists()); // Failed @Size validation
        }

        @Test
        @DisplayName("401 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized()); // Routes to GET /api/users lacking auth -> 401
        }
    }

    // ═══════════════════════════════════════════════
    // 4. POST /api/users/admin
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/users/admin")
    class CreateUserWithRole {
        private static final String URL = "/api/users/admin";
        private static final String VALID_BODY = """
                {
                    "email": "teacher@example.com",
                    "password": "password123",
                    "fullName": "Teacher 1",
                    "role": "TEACHER"
                }
                """;

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot create user with role")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(post(URL).with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot create user with role")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(post(URL).with(teacherJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - Role is missing")
        void shouldReturn400_WhenRoleMissing() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "test@example.com",
                                "password": "password123",
                                "fullName": "Test User"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.role").exists());
        }

        @Test
        @DisplayName("409 - Email already exists")
        void shouldReturn409_WhenEmailExists() throws Exception {
            when(userService.createUserWithRole(any(CreateUserWithRoleRequest.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Email already exists"));

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("201 - ADMIN successfully creates user with role")
        void shouldReturn201_WhenCreatedSuccessfully() throws Exception {
            when(userService.createUserWithRole(any(CreateUserWithRoleRequest.class)))
                    .thenReturn(sampleUserResponse(Role.TEACHER));

            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("TEACHER"));
        }

        // ── Malicious Payloads ──

        @Test
        @DisplayName("400 - Type Mismatch for Enum (Role)")
        void shouldReturn400_WhenEnumInvalid() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "teacher@example.com",
                                "password": "password123",
                                "fullName": "Teacher 1",
                                "role": "SUPER_HACKER"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - Malformed JSON")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "teacher@example.com",
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - XSS Injection")
        void shouldReturn400_WhenXssInFields() throws Exception {
            mockMvc.perform(post(URL).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "<script>alert(1)</script>@example.com",
                                "password": "password123",
                                "fullName": "<script>alert(1)</script>",
                                "role": "TEACHER"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.fullName").exists());
        }

        @Test
        @DisplayName("400 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(get(URL).with(adminJwt()))
                    .andExpect(status().isBadRequest()); // Routes to GET /api/users/{id} where id=admin -> 400 Type
                                                         // Mismatch
        }
    }

    // ═══════════════════════════════════════════════
    // 5. PUT /api/users/{id}
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("PUT /api/users/{id}")
    class UpdateUser {
        private String url() {
            return "/api/users/" + USER_ID;
        }

        private static final String VALID_BODY = """
                {
                    "email": "updated@example.com",
                    "fullName": "Updated User",
                    "role": "ADMIN",
                    "password": "newpassword123"
                }
                """;

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(put(url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 - STUDENT cannot update user")
        void shouldReturn403_WhenRoleIsStudent() throws Exception {
            mockMvc.perform(put(url()).with(studentJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 - TEACHER cannot update user")
        void shouldReturn403_WhenRoleIsTeacher() throws Exception {
            mockMvc.perform(put(url()).with(teacherJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 - Validation error (missing fields)")
        void shouldReturn400_WhenMissingRequiredFields() throws Exception {
            mockMvc.perform(put(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "password": "newpassword123"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.fullName").exists())
                    .andExpect(jsonPath("$.errors.role").exists());
        }

        @Test
        @DisplayName("404 - User not found")
        void shouldReturn404_WhenUserNotFound() throws Exception {
            when(userService.updateUser(eq(USER_ID), any(UpdateUserRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(put(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("409 - Email belonging to another user")
        void shouldReturn409_WhenEmailExists() throws Exception {
            when(userService.updateUser(eq(USER_ID), any(UpdateUserRequest.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Email already exists"));

            mockMvc.perform(put(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("200 - ADMIN successfully updates user")
        void shouldReturn200_WhenUpdateSuccess() throws Exception {
            when(userService.updateUser(eq(USER_ID), any(UpdateUserRequest.class)))
                    .thenReturn(sampleUserResponse(Role.ADMIN));

            mockMvc.perform(put(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        // ── Malicious Payloads ──

        @Test
        @DisplayName("400 - Type Mismatch (Invalid UUID format)")
        void shouldReturn400_WhenIdHasInvalidFormat() throws Exception {
            mockMvc.perform(put("/api/users/invalid-uuid").with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 - XSS Injection in Email/FullName")
        void shouldReturn400_WhenXssInFields() throws Exception {
            mockMvc.perform(put(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "javascript:alert(1)",
                                "fullName": "<script>alert(1)</script>",
                                "role": "ADMIN",
                                "password": "newpassword123"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.fullName").exists());
        }

        @Test
        @DisplayName("400 - Buffer Overflow (String > 100 chars)")
        void shouldReturn400_WhenBufferOverflow() throws Exception {
            String longString = "A".repeat(150);
            mockMvc.perform(put(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "email": "updated@example.com",
                                "fullName": "%s",
                                "role": "ADMIN",
                                "password": "newpassword123"
                            }
                            """, longString)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fullName").exists());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (POST instead of PUT)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(post(url()).with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
