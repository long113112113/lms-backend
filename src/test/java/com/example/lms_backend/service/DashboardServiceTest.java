package com.example.lms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.lms_backend.dto.dashboard.StudentDashboardResponse;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.EnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID OTHER_CLASS_ID = UUID.randomUUID();

    @Mock
    private CourseClassRepository courseClassRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(courseClassRepository, enrollmentRepository);
    }

    @Test
    @DisplayName("Builds student dashboard from active and completed enrollments")
    void shouldBuildStudentDashboardFromActiveAndCompletedEnrollments() {
        Enrollment activeNewest = sampleEnrollment(
                CLASS_ID,
                "CS101-A",
                "Software Engineering",
                "Nguyen Van Teacher",
                EnrollmentStatus.ACTIVE,
                Instant.parse("2026-03-09T10:15:30Z"));
        Enrollment completed = sampleEnrollment(
                UUID.randomUUID(),
                "CS102-A",
                "Databases",
                "Tran Minh Teacher",
                EnrollmentStatus.COMPLETED,
                Instant.parse("2026-03-08T10:15:30Z"));
        Enrollment activeOldest = sampleEnrollment(
                OTHER_CLASS_ID,
                "CS103-A",
                "Operating Systems",
                "Le Thi Teacher",
                EnrollmentStatus.ACTIVE,
                Instant.parse("2026-03-07T10:15:30Z"));
        Enrollment left = sampleEnrollment(
                UUID.randomUUID(),
                "CS104-A",
                "Networks",
                "Ignored Teacher",
                EnrollmentStatus.LEFT,
                Instant.parse("2026-03-06T10:15:30Z"));
        Enrollment kicked = sampleEnrollment(
                UUID.randomUUID(),
                "CS105-A",
                "Security",
                "Ignored Teacher",
                EnrollmentStatus.KICKED,
                Instant.parse("2026-03-05T10:15:30Z"));

        when(enrollmentRepository.findDashboardEnrollmentsByStudentIdAndStatuses(
                STUDENT_ID,
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)))
                .thenReturn(List.of(activeNewest, completed, activeOldest, left, kicked));

        StudentDashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

        assertEquals(2, response.totalActiveClasses());
        assertEquals(1, response.totalCompletedClasses());
        assertEquals(2, response.activeClasses().size());
        assertEquals(CLASS_ID, response.activeClasses().get(0).classId());
        assertEquals("CS101-A", response.activeClasses().get(0).code());
        assertEquals("Software Engineering", response.activeClasses().get(0).courseName());
        assertEquals("Nguyen Van Teacher", response.activeClasses().get(0).teacherName());
        assertEquals(Instant.parse("2026-03-09T10:15:30Z"), response.activeClasses().get(0).enrolledAt());
        assertEquals(OTHER_CLASS_ID, response.activeClasses().get(1).classId());
    }

    @Test
    @DisplayName("Handles active class without teacher")
    void shouldHandleActiveClassWithoutTeacher() {
        Enrollment enrollment = sampleEnrollment(
                CLASS_ID,
                "CS101-A",
                "Software Engineering",
                null,
                EnrollmentStatus.ACTIVE,
                Instant.parse("2026-03-09T10:15:30Z"));

        when(enrollmentRepository.findDashboardEnrollmentsByStudentIdAndStatuses(
                STUDENT_ID,
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)))
                .thenReturn(List.of(enrollment));

        StudentDashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

        assertEquals(1, response.totalActiveClasses());
        assertEquals(0, response.totalCompletedClasses());
        assertEquals(1, response.activeClasses().size());
        assertNull(response.activeClasses().get(0).teacherName());
    }

    @Test
    @DisplayName("Returns empty student dashboard when no active or completed enrollments exist")
    void shouldReturnEmptyDashboardWhenNoEnrollmentsExist() {
        when(enrollmentRepository.findDashboardEnrollmentsByStudentIdAndStatuses(
                STUDENT_ID,
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)))
                .thenReturn(List.of());

        StudentDashboardResponse response = dashboardService.getStudentDashboard(STUDENT_ID);

        assertEquals(0, response.totalActiveClasses());
        assertEquals(0, response.totalCompletedClasses());
        assertEquals(List.of(), response.activeClasses());
    }

    private Enrollment sampleEnrollment(
            UUID classId,
            String classCode,
            String courseName,
            String teacherName,
            EnrollmentStatus status,
            Instant createdAt) {
        var course = new Course();
        course.setId(UUID.randomUUID());
        course.setCode(classCode.replace("-A", ""));
        course.setName(courseName);
        course.setCredits(3);
        course.setDescription("Seeded for dashboard service tests");

        User teacher = null;
        if (teacherName != null) {
            teacher = new User();
            teacher.setId(UUID.randomUUID());
            teacher.setEmail(teacherName.toLowerCase().replace(" ", ".") + "@lms.com");
            teacher.setPassword("encoded-password");
            teacher.setFullName(teacherName);
            teacher.setRole(Role.TEACHER);
        }

        var courseClass = new CourseClass();
        courseClass.setId(classId);
        courseClass.setCode(classCode);
        courseClass.setSemester("HK1-2026");
        courseClass.setCourse(course);
        courseClass.setTeacher(teacher);
        courseClass.setJoinCode("ABC1234");

        var student = new User();
        student.setId(STUDENT_ID);
        student.setEmail("student@lms.com");
        student.setPassword("encoded-password");
        student.setFullName("Student Dashboard");
        student.setRole(Role.STUDENT);

        var enrollment = new Enrollment();
        enrollment.setId(UUID.randomUUID());
        enrollment.setStudent(student);
        enrollment.setCourseClass(courseClass);
        enrollment.setStatus(status);
        enrollment.setCreatedAt(createdAt);
        enrollment.setUpdatedAt(createdAt);
        return enrollment;
    }
}
