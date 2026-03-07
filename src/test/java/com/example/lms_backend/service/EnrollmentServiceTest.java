package com.example.lms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.exception.AccessDeniedException;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

        private static final UUID CLASS_ID = UUID.randomUUID();
        private static final UUID COURSE_ID = UUID.randomUUID();
        private static final UUID TEACHER_ID = UUID.randomUUID();
        private static final UUID OTHER_TEACHER_ID = UUID.randomUUID();
        private static final UUID STUDENT_ID = UUID.randomUUID();
        private static final UUID ADMIN_ID = UUID.randomUUID();
        private static final UUID ENROLLMENT_ID = UUID.randomUUID();

        @Mock
        private EnrollmentRepository enrollmentRepository;

        @Mock
        private CourseClassRepository courseClassRepository;

        @Mock
        private UserRepository userRepository;

        private EnrollmentService enrollmentService;

        @BeforeEach
        void setUp() {
                enrollmentService = new EnrollmentService(enrollmentRepository, courseClassRepository, userRepository);
        }

        @Test
        @DisplayName("Defaults class student sort to createdAt DESC")
        void shouldDefaultClassStudentSortToCreatedAtDesc() {
                when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
                when(enrollmentRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(sampleEnrollment()), PageRequest.of(0, 10), 1));

                var page = enrollmentService.getClassStudents(CLASS_ID, ADMIN_ID, "ADMIN", null, PageRequest.of(0, 10));

                assertEquals(1, page.getTotalElements());

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(enrollmentRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                                pageableCaptor.capture());

                Sort.Order createdAtOrder = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
                assertNotNull(createdAtOrder);
                assertEquals(Sort.Direction.DESC, createdAtOrder.getDirection());
        }

        @Test
        @DisplayName("Maps studentName sort alias to nested student.fullName path")
        void shouldTranslateStudentNameSortAlias() {
                when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
                when(enrollmentRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(sampleEnrollment()), PageRequest.of(0, 10), 1));

                enrollmentService.getClassStudents(
                                CLASS_ID,
                                TEACHER_ID,
                                "TEACHER",
                                "nguyen",
                                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("studentName"))));

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(enrollmentRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                                pageableCaptor.capture());

                Sort.Order studentNameOrder = pageableCaptor.getValue().getSort().getOrderFor("student.fullName");
                assertNotNull(studentNameOrder);
                assertEquals(Sort.Direction.ASC, studentNameOrder.getDirection());
        }

        @Test
        @DisplayName("Rejects unsupported sort fields before querying enrollments")
        void shouldRejectUnsupportedSortField() {
                when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));

                var exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> enrollmentService.getClassStudents(
                                                CLASS_ID,
                                                ADMIN_ID,
                                                "ADMIN",
                                                null,
                                                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("hacked")))));

                assertEquals(
                                "Unsupported sort field: hacked. Allowed values: createdAt, studentName, studentEmail",
                                exception.getMessage());
                verify(enrollmentRepository, never()).findAll(
                                org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Rejects teacher who does not own the class")
        void shouldRejectTeacherForForeignClass() {
                when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                                .thenReturn(Optional.of(sampleCourseClass(OTHER_TEACHER_ID)));

                var exception = assertThrows(
                                AccessDeniedException.class,
                                () -> enrollmentService.getClassStudents(CLASS_ID, TEACHER_ID, "TEACHER", null,
                                                PageRequest.of(0, 10)));

                assertEquals("You are not the teacher of this class", exception.getMessage());
                verify(enrollmentRepository, never()).findAll(
                                org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                                any(Pageable.class));
        }

        private Enrollment sampleEnrollment() {
                var enrollment = new Enrollment();
                enrollment.setId(ENROLLMENT_ID);
                enrollment.setStudent(sampleStudent("student@example.com", "Nguyen Van A"));
                enrollment.setCourseClass(sampleCourseClass(TEACHER_ID));
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                return enrollment;
        }

        private CourseClass sampleCourseClass(UUID teacherId) {
                var course = new Course();
                course.setId(COURSE_ID);
                course.setCode("CS101");
                course.setName("Software Engineering");
                course.setCredits(3);

                var teacher = new User();
                teacher.setId(teacherId);
                teacher.setEmail("teacher@lms.com");
                teacher.setPassword("encoded-password");
                teacher.setFullName("John Doe");
                teacher.setRole(Role.TEACHER);

                var courseClass = new CourseClass();
                courseClass.setId(CLASS_ID);
                courseClass.setCode("CLASS-01");
                courseClass.setSemester("Fall 2026");
                courseClass.setCourse(course);
                courseClass.setTeacher(teacher);
                courseClass.setJoinCode("ABC1234");
                return courseClass;
        }

        private User sampleStudent(String email, String fullName) {
                var student = new User();
                student.setId(STUDENT_ID);
                student.setEmail(email);
                student.setPassword("encoded-password");
                student.setFullName(fullName);
                student.setRole(Role.STUDENT);
                return student;
        }
}
