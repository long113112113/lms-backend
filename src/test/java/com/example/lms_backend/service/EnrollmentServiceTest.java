package com.example.lms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import com.example.lms_backend.dto.enrollment.EnrollmentResponse;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.exception.AccessDeniedException;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
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
    private static final UUID OTHER_STUDENT_ID = UUID.randomUUID();
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
    @DisplayName("Creates a new ACTIVE enrollment when a student joins with a valid code")
    void shouldCreateEnrollmentWhenJoinCodeIsValid() {
        var courseClass = sampleCourseClass(TEACHER_ID);
        var student = sampleStudent(STUDENT_ID, "student@example.com", "Nguyen Van A");

        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("ABC1234"))
                .thenReturn(Optional.of(courseClass));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            var enrollment = invocation.getArgument(0, Enrollment.class);
            enrollment.setId(ENROLLMENT_ID);
            return enrollment;
        });

        EnrollmentResponse response = enrollmentService.joinClass(STUDENT_ID, "ABC1234");

        assertEquals(ENROLLMENT_ID, response.enrollmentId());
        assertEquals(STUDENT_ID, response.studentId());
        assertEquals("Nguyen Van A", response.studentName());
        assertEquals("student@example.com", response.studentEmail());
        assertEquals(CLASS_ID, response.courseClassId());
        assertEquals("CLASS-01", response.courseClassCode());
        assertEquals("Software Engineering", response.courseName());
        assertEquals("ACTIVE", response.status());

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        Enrollment savedEnrollment = enrollmentCaptor.getValue();
        assertSame(student, savedEnrollment.getStudent());
        assertSame(courseClass, savedEnrollment.getCourseClass());
        assertEquals(EnrollmentStatus.ACTIVE, savedEnrollment.getStatus());
    }

    @Test
    @DisplayName("Rejects join when the join code does not exist")
    void shouldRejectJoinWhenJoinCodeIsInvalid() {
        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("INVALID"))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.joinClass(STUDENT_ID, "INVALID"));

        assertEquals("Invalid join code", exception.getMessage());
        verify(userRepository, never()).findById(any(UUID.class));
        verify(enrollmentRepository, never()).findByStudentIdAndCourseClassId(any(UUID.class), any(UUID.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects join when the student no longer exists")
    void shouldRejectJoinWhenStudentDoesNotExist() {
        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("ABC1234"))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.joinClass(STUDENT_ID, "ABC1234"));

        assertEquals("Student not found", exception.getMessage());
        verify(enrollmentRepository, never()).findByStudentIdAndCourseClassId(any(UUID.class), any(UUID.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects join when the student is already ACTIVE in the class")
    void shouldRejectJoinWhenEnrollmentIsAlreadyActive() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.ACTIVE);

        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("ABC1234"))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(sampleStudent(STUDENT_ID,
                "student@example.com", "Nguyen Van A")));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));

        var exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> enrollmentService.joinClass(STUDENT_ID, "ABC1234"));

        assertEquals("You have already joined this class", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects join when the student was kicked from the class")
    void shouldRejectJoinWhenEnrollmentWasKicked() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.KICKED);

        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("ABC1234"))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(sampleStudent(STUDENT_ID,
                "student@example.com", "Nguyen Van A")));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));

        var exception = assertThrows(AccessDeniedException.class,
                () -> enrollmentService.joinClass(STUDENT_ID, "ABC1234"));

        assertEquals("You have been removed from this class", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Reactivates an existing LEFT enrollment instead of creating a duplicate")
    void shouldReactivateLeftEnrollment() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.LEFT);

        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("ABC1234"))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(sampleStudent(STUDENT_ID,
                "student@example.com", "Nguyen Van A")));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));
        when(enrollmentRepository.save(existingEnrollment)).thenReturn(existingEnrollment);

        EnrollmentResponse response = enrollmentService.joinClass(STUDENT_ID, "ABC1234");

        assertEquals(ENROLLMENT_ID, response.enrollmentId());
        assertEquals("ACTIVE", response.status());
        assertEquals(EnrollmentStatus.ACTIVE, existingEnrollment.getStatus());
        verify(enrollmentRepository).save(existingEnrollment);
    }

    @Test
    @DisplayName("Rejects join when the student already completed the class")
    void shouldRejectJoinWhenEnrollmentIsCompleted() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.COMPLETED);

        when(courseClassRepository.findByJoinCodeAndIsDeletedFalse("ABC1234"))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(sampleStudent(STUDENT_ID,
                "student@example.com", "Nguyen Van A")));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));

        var exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> enrollmentService.joinClass(STUDENT_ID, "ABC1234"));

        assertEquals("You have already completed this class", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Marks an ACTIVE enrollment as LEFT when the student leaves")
    void shouldMarkEnrollmentLeftWhenStudentLeaves() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));
        when(enrollmentRepository.save(existingEnrollment)).thenReturn(existingEnrollment);

        enrollmentService.leaveClass(STUDENT_ID, CLASS_ID);

        assertEquals(EnrollmentStatus.LEFT, existingEnrollment.getStatus());
        verify(enrollmentRepository).save(existingEnrollment);
    }

    @Test
    @DisplayName("Rejects leave when the enrollment does not exist")
    void shouldRejectLeaveWhenEnrollmentDoesNotExist() {
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.leaveClass(STUDENT_ID, CLASS_ID));

        assertEquals("Enrollment not found", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects leave when the enrollment is no longer ACTIVE")
    void shouldRejectLeaveWhenEnrollmentIsNotActive() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.LEFT);

        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.leaveClass(STUDENT_ID, CLASS_ID));

        assertEquals("No active enrollment found for this class", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects kick when the class does not exist")
    void shouldRejectKickWhenClassDoesNotExist() {
        when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.kickStudent(TEACHER_ID, CLASS_ID, STUDENT_ID));

        assertEquals("Class not found", exception.getMessage());
        verify(enrollmentRepository, never()).findByStudentIdAndCourseClassId(any(UUID.class), any(UUID.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects kick when the caller does not own the class")
    void shouldRejectKickWhenTeacherDoesNotOwnClass() {
        when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                .thenReturn(Optional.of(sampleCourseClass(OTHER_TEACHER_ID)));

        var exception = assertThrows(AccessDeniedException.class,
                () -> enrollmentService.kickStudent(TEACHER_ID, CLASS_ID, STUDENT_ID));

        assertEquals("You are not the teacher of this class", exception.getMessage());
        verify(enrollmentRepository, never()).findByStudentIdAndCourseClassId(any(UUID.class), any(UUID.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects kick when the student is not enrolled in the class")
    void shouldRejectKickWhenStudentIsNotEnrolled() {
        when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(OTHER_STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.kickStudent(TEACHER_ID, CLASS_ID, OTHER_STUDENT_ID));

        assertEquals("Student is not enrolled in this class", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Rejects kick when the enrollment is not ACTIVE")
    void shouldRejectKickWhenEnrollmentIsNotActive() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.LEFT);

        when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.kickStudent(TEACHER_ID, CLASS_ID, STUDENT_ID));

        assertEquals("No active enrollment found for this student", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Marks an ACTIVE enrollment as KICKED when the teacher removes the student")
    void shouldMarkEnrollmentKickedWhenTeacherRemovesStudent() {
        var existingEnrollment = sampleEnrollment(EnrollmentStatus.ACTIVE);

        when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(enrollmentRepository.findByStudentIdAndCourseClassId(STUDENT_ID, CLASS_ID))
                .thenReturn(Optional.of(existingEnrollment));
        when(enrollmentRepository.save(existingEnrollment)).thenReturn(existingEnrollment);

        enrollmentService.kickStudent(TEACHER_ID, CLASS_ID, STUDENT_ID);

        assertEquals(EnrollmentStatus.KICKED, existingEnrollment.getStatus());
        verify(enrollmentRepository).save(existingEnrollment);
    }

    @Test
    @DisplayName("Defaults class student sort to createdAt DESC")
    void shouldDefaultClassStudentSortToCreatedAtDesc() {
        when(courseClassRepository.findByIdAndIsDeletedFalse(CLASS_ID))
                .thenReturn(Optional.of(sampleCourseClass(TEACHER_ID)));
        when(enrollmentRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Enrollment>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEnrollment(EnrollmentStatus.ACTIVE)), PageRequest.of(0, 10), 1));

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
                .thenReturn(new PageImpl<>(List.of(sampleEnrollment(EnrollmentStatus.ACTIVE)), PageRequest.of(0, 10), 1));

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

    private Enrollment sampleEnrollment(EnrollmentStatus status) {
        var enrollment = new Enrollment();
        enrollment.setId(ENROLLMENT_ID);
        enrollment.setStudent(sampleStudent(STUDENT_ID, "student@example.com", "Nguyen Van A"));
        enrollment.setCourseClass(sampleCourseClass(TEACHER_ID));
        enrollment.setStatus(status);
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

    private User sampleStudent(UUID studentId, String email, String fullName) {
        var student = new User();
        student.setId(studentId);
        student.setEmail(email);
        student.setPassword("encoded-password");
        student.setFullName(fullName);
        student.setRole(Role.STUDENT);
        return student;
    }
}
