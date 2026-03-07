package com.example.lms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.exception.AccessDeniedException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CourseClassServiceTest {

    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID OTHER_TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private CourseClassRepository courseClassRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    private CourseClassService courseClassService;

    @BeforeEach
    void setUp() {
        courseClassService = new CourseClassService(
                courseClassRepository,
                courseRepository,
                enrollmentRepository,
                userRepository);
    }

    @Test
    @DisplayName("ADMIN can view any class detail")
    void shouldAllowAdminToViewAnyClass() {
        var courseClass = sampleCourseClass(TEACHER_ID);
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(courseClass));

        var response = courseClassService.getCourseClassById(ADMIN_ID, "ADMIN", CLASS_ID);

        assertEquals(CLASS_ID, response.id());
        assertEquals("CLASS-01", response.code());
        verify(enrollmentRepository, never())
                .existsByStudentIdAndCourseClassIdAndStatus(STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("TEACHER can view own class detail")
    void shouldAllowTeacherToViewOwnClass() {
        var courseClass = sampleCourseClass(TEACHER_ID);
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(courseClass));

        var response = courseClassService.getCourseClassById(TEACHER_ID, "TEACHER", CLASS_ID);

        assertEquals(TEACHER_ID, response.teacherId());
        assertEquals("Software Engineering", response.courseName());
    }

    @Test
    @DisplayName("TEACHER cannot view another teacher's class")
    void shouldRejectTeacherForForeignClass() {
        var courseClass = sampleCourseClass(OTHER_TEACHER_ID);
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(courseClass));

        var exception = assertThrows(
                AccessDeniedException.class,
                () -> courseClassService.getCourseClassById(TEACHER_ID, "TEACHER", CLASS_ID));

        assertEquals("You are not the teacher of this class", exception.getMessage());
        verify(enrollmentRepository, never())
                .existsByStudentIdAndCourseClassIdAndStatus(STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("STUDENT can view enrolled class detail")
    void shouldAllowStudentWithActiveEnrollment() {
        var courseClass = sampleCourseClass(TEACHER_ID);
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(courseClass));
        when(enrollmentRepository.existsByStudentIdAndCourseClassIdAndStatus(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);

        var response = courseClassService.getCourseClassById(STUDENT_ID, "STUDENT", CLASS_ID);

        assertEquals(COURSE_ID, response.courseId());
        assertEquals("John Doe", response.teacherName());
    }

    @Test
    @DisplayName("STUDENT cannot view class without active enrollment")
    void shouldRejectStudentWithoutActiveEnrollment() {
        var courseClass = sampleCourseClass(TEACHER_ID);
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(courseClass));
        when(enrollmentRepository.existsByStudentIdAndCourseClassIdAndStatus(
                STUDENT_ID, CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);

        var exception = assertThrows(
                AccessDeniedException.class,
                () -> courseClassService.getCourseClassById(STUDENT_ID, "STUDENT", CLASS_ID));

        assertEquals("You are not enrolled in this class", exception.getMessage());
    }

    @Test
    @DisplayName("Should return 404 when class does not exist")
    void shouldThrowWhenClassNotFound() {
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.empty());

        var exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseClassService.getCourseClassById(ADMIN_ID, "ADMIN", CLASS_ID));

        assertEquals("Class not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject unknown role")
    void shouldRejectUnknownRole() {
        var courseClass = sampleCourseClass(TEACHER_ID);
        when(courseClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(courseClass));

        var exception = assertThrows(
                AccessDeniedException.class,
                () -> courseClassService.getCourseClassById(ADMIN_ID, "GUEST", CLASS_ID));

        assertEquals("Unknown role: GUEST", exception.getMessage());
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
        teacher.setFullName("John Doe");
        teacher.setRole(Role.TEACHER);

        var courseClass = new CourseClass();
        courseClass.setId(CLASS_ID);
        courseClass.setCode("CLASS-01");
        courseClass.setSemester("Fall 2024");
        courseClass.setCourse(course);
        courseClass.setTeacher(teacher);
        courseClass.setJoinCode("ABC1234");
        return courseClass;
    }
}
