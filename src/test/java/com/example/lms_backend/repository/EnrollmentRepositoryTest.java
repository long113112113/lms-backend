package com.example.lms_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.specification.EnrollmentSpecification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseClassRepository courseClassRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("findAll(spec, pageable) preloads student and class graph to avoid extra queries when mapping response")
    void shouldPreloadStudentAndCourseGraphForSpecificationPageQuery() {
        var data = seedClassStudentSearchData();

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        var page = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(data.targetClassId(), null),
                PageRequest.of(0, 10));

        assertEquals(2, page.getContent().size());

        long statementsBeforeAccess = statistics.getPrepareStatementCount();

        for (var enrollment : page.getContent()) {
            assertTrue(Persistence.getPersistenceUtil().isLoaded(enrollment, "student"));
            assertTrue(Persistence.getPersistenceUtil().isLoaded(enrollment, "courseClass"));
            assertTrue(Persistence.getPersistenceUtil().isLoaded(enrollment.getCourseClass(), "course"));

            enrollment.getStudent().getFullName();
            enrollment.getCourseClass().getCourse().getName();
        }

        long statementsAfterAccess = statistics.getPrepareStatementCount();
        assertEquals(statementsBeforeAccess, statementsAfterAccess);
    }

    @Test
    @DisplayName("buildForClassStudents filters by class, active status, and q on name or email")
    void shouldFilterByClassActiveStatusAndSearchQuery() {
        var data = seedClassStudentSearchData();

        entityManager.flush();
        entityManager.clear();

        var byName = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(data.targetClassId(), "nguyen"),
                PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());
        assertEquals("Nguyen Van A", byName.getContent().get(0).getStudent().getFullName());

        var byEmail = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(data.targetClassId(), "MATCHING@EXAMPLE.COM"),
                PageRequest.of(0, 10));
        assertEquals(1, byEmail.getTotalElements());
        assertEquals("matching@example.com", byEmail.getContent().get(0).getStudent().getEmail());

        var allActive = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(data.targetClassId(), null),
                PageRequest.of(0, 10));
        assertEquals(2, allActive.getTotalElements());
    }

    @Test
    @DisplayName("buildForClassStudents escapes SQL wildcard characters in q")
    void shouldEscapeWildcardCharactersInSearchQuery() {
        UUID targetClassId = seedWildcardSearchData();

        entityManager.flush();
        entityManager.clear();

        var underscoreSearch = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(targetClassId, "alice_1"),
                PageRequest.of(0, 10));
        assertEquals(1, underscoreSearch.getTotalElements());
        assertEquals("alice_1@example.com", underscoreSearch.getContent().get(0).getStudent().getEmail());

        var percentSearch = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(targetClassId, "100%"),
                PageRequest.of(0, 10));
        assertEquals(1, percentSearch.getTotalElements());
        assertEquals("Student 100% Real", percentSearch.getContent().get(0).getStudent().getFullName());
    }

    @Test
    @DisplayName("buildForClassStudents treats empty and whitespace-only q the same as null")
    void shouldTreatEmptyAndWhitespaceQueryAsNull() {
        var data = seedClassStudentSearchData();

        entityManager.flush();
        entityManager.clear();

        var emptyQuery = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(data.targetClassId(), ""),
                PageRequest.of(0, 10));
        assertEquals(2, emptyQuery.getTotalElements());

        var whitespaceQuery = enrollmentRepository.findAll(
                EnrollmentSpecification.buildForClassStudents(data.targetClassId(), "   "),
                PageRequest.of(0, 10));
        assertEquals(2, whitespaceQuery.getTotalElements());
    }

    @Test
    @DisplayName("findDashboardEnrollmentsByStudentIdAndStatuses preloads class graph, filters statuses, and sorts newest first")
    void shouldLoadStudentDashboardEnrollmentsWithCourseAndTeacherGraph() throws InterruptedException {
        UUID studentId = seedStudentDashboardData();

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        var enrollments = enrollmentRepository.findDashboardEnrollmentsByStudentIdAndStatuses(
                studentId,
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED));

        assertEquals(3, enrollments.size());
        assertEquals(EnrollmentStatus.ACTIVE, enrollments.get(0).getStatus());
        assertEquals(EnrollmentStatus.ACTIVE, enrollments.get(1).getStatus());
        assertEquals(EnrollmentStatus.COMPLETED, enrollments.get(2).getStatus());

        long statementsBeforeAccess = statistics.getPrepareStatementCount();

        for (var enrollment : enrollments) {
            assertTrue(Persistence.getPersistenceUtil().isLoaded(enrollment, "courseClass"));
            assertTrue(Persistence.getPersistenceUtil().isLoaded(enrollment.getCourseClass(), "course"));
            assertTrue(Persistence.getPersistenceUtil().isLoaded(enrollment.getCourseClass(), "teacher"));

            enrollment.getCourseClass().getCode();
            enrollment.getCourseClass().getCourse().getName();
            enrollment.getCourseClass().getTeacher().getFullName();
        }

        long statementsAfterAccess = statistics.getPrepareStatementCount();
        assertEquals(statementsBeforeAccess, statementsAfterAccess);
    }

    private SearchSeedData seedClassStudentSearchData() {
        var teacher = createTeacher("teacher-search@lms.com", "Teacher Search");
        userRepository.save(teacher);

        var course = createCourse("CS201", "Databases");
        courseRepository.save(course);

        var targetClass = createCourseClass(course, teacher, "CS201-01", "ABC2010");
        var otherClass = createCourseClass(course, teacher, "CS201-02", "ABC2011");
        courseClassRepository.saveAll(List.of(targetClass, otherClass));

        var studentByName = createStudent("student@example.com", "Nguyen Van A");
        var studentByEmail = createStudent("matching@example.com", "Tran Student");
        var inactiveStudent = createStudent("inactive@example.com", "Inactive Student");
        var otherClassStudent = createStudent("other@example.com", "Nguyen Other Class");
        userRepository.saveAll(List.of(studentByName, studentByEmail, inactiveStudent, otherClassStudent));

        enrollmentRepository.saveAll(List.of(
                createEnrollment(studentByName, targetClass, EnrollmentStatus.ACTIVE),
                createEnrollment(studentByEmail, targetClass, EnrollmentStatus.ACTIVE),
                createEnrollment(inactiveStudent, targetClass, EnrollmentStatus.LEFT),
                createEnrollment(otherClassStudent, otherClass, EnrollmentStatus.ACTIVE)));

        return new SearchSeedData(targetClass.getId());
    }

    private UUID seedWildcardSearchData() {
        var teacher = createTeacher("teacher-wildcard@lms.com", "Teacher Wildcard");
        userRepository.save(teacher);

        var course = createCourse("CS301", "Information Retrieval");
        courseRepository.save(course);

        var targetClass = createCourseClass(course, teacher, "CS301-01", "ABC3010");
        courseClassRepository.save(targetClass);

        var underscoreStudent = createStudent("alice_1@example.com", "Alice Under");
        var wildcardUnderscoreCandidate = createStudent("alicex1@example.com", "Alice Wide");
        var percentStudent = createStudent("percent@example.com", "Student 100% Real");
        var wildcardPercentCandidate = createStudent("percent-wide@example.com", "Student 100X Real");
        userRepository.saveAll(List.of(
                underscoreStudent,
                wildcardUnderscoreCandidate,
                percentStudent,
                wildcardPercentCandidate));

        enrollmentRepository.saveAll(List.of(
                createEnrollment(underscoreStudent, targetClass, EnrollmentStatus.ACTIVE),
                createEnrollment(wildcardUnderscoreCandidate, targetClass, EnrollmentStatus.ACTIVE),
                createEnrollment(percentStudent, targetClass, EnrollmentStatus.ACTIVE),
                createEnrollment(wildcardPercentCandidate, targetClass, EnrollmentStatus.ACTIVE)));

        return targetClass.getId();
    }

    private UUID seedStudentDashboardData() throws InterruptedException {
        var teacher = createTeacher("teacher-dashboard@lms.com", "Teacher Dashboard");
        userRepository.save(teacher);

        var targetStudent = createStudent("student-dashboard@lms.com", "Student Dashboard");
        var otherStudent = createStudent("student-other@lms.com", "Other Student");
        userRepository.saveAll(List.of(targetStudent, otherStudent));

        var course = createCourse("CS401", "Distributed Systems");
        courseRepository.save(course);

        var oldestCompletedClass = createCourseClass(course, teacher, "CS401-01", "ABC4010");
        var olderActiveClass = createCourseClass(course, teacher, "CS401-02", "ABC4011");
        var newestActiveClass = createCourseClass(course, teacher, "CS401-03", "ABC4012");
        var ignoredStatusClass = createCourseClass(course, teacher, "CS401-04", "ABC4013");
        var otherStudentClass = createCourseClass(course, teacher, "CS401-05", "ABC4014");
        courseClassRepository.saveAll(List.of(
                oldestCompletedClass,
                olderActiveClass,
                newestActiveClass,
                ignoredStatusClass,
                otherStudentClass));

        saveEnrollmentWithDelay(createEnrollment(targetStudent, oldestCompletedClass, EnrollmentStatus.COMPLETED));
        saveEnrollmentWithDelay(createEnrollment(targetStudent, olderActiveClass, EnrollmentStatus.ACTIVE));
        saveEnrollmentWithDelay(createEnrollment(targetStudent, newestActiveClass, EnrollmentStatus.ACTIVE));
        saveEnrollmentWithDelay(createEnrollment(targetStudent, ignoredStatusClass, EnrollmentStatus.LEFT));
        saveEnrollmentWithDelay(createEnrollment(otherStudent, otherStudentClass, EnrollmentStatus.ACTIVE));

        return targetStudent.getId();
    }

    private void saveEnrollmentWithDelay(Enrollment enrollment) throws InterruptedException {
        enrollmentRepository.saveAndFlush(enrollment);
        Thread.sleep(5);
    }

    private User createTeacher(String email, String fullName) {
        var user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setFullName(fullName);
        user.setRole(Role.TEACHER);
        return user;
    }

    private User createStudent(String email, String fullName) {
        var user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setFullName(fullName);
        user.setRole(Role.STUDENT);
        return user;
    }

    private Course createCourse(String code, String name) {
        var course = new Course();
        course.setCode(code);
        course.setName(name);
        course.setCredits(3);
        course.setDescription("Seeded for enrollment repository tests");
        return course;
    }

    private CourseClass createCourseClass(Course course, User teacher, String code, String joinCode) {
        var courseClass = new CourseClass();
        courseClass.setCourse(course);
        courseClass.setCode(code);
        courseClass.setSemester("HK1-2026");
        courseClass.setTeacher(teacher);
        courseClass.setJoinCode(joinCode);
        return courseClass;
    }

    private Enrollment createEnrollment(User student, CourseClass courseClass, EnrollmentStatus status) {
        var enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourseClass(courseClass);
        enrollment.setStatus(status);
        return enrollment;
    }

    private record SearchSeedData(UUID targetClassId) {
    }
}
