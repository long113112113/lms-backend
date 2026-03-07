package com.example.lms_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.specification.CourseClassSpecification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class CourseClassRepositoryTest {

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
    @DisplayName("findAll(spec, pageable) preloads course and teacher to avoid extra queries when mapping response")
    void shouldPreloadCourseAndTeacherForSpecificationPageQuery() {
        seedCourseClasses();

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        var spec = CourseClassSpecification.buildForAdmin(null, null, null, null);
        var page = courseClassRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(2, page.getContent().size());

        long statementsBeforeAccess = statistics.getPrepareStatementCount();

        for (var courseClass : page.getContent()) {
            assertTrue(Persistence.getPersistenceUtil().isLoaded(courseClass, "course"));
            assertTrue(Persistence.getPersistenceUtil().isLoaded(courseClass, "teacher"));

            courseClass.getCourse().getName();
            courseClass.getTeacher().getFullName();
        }

        long statementsAfterAccess = statistics.getPrepareStatementCount();
        assertEquals(statementsBeforeAccess, statementsAfterAccess);
    }

    private void seedCourseClasses() {
        var teacherA = createTeacher("teacher-a@lms.com", "Teacher A");
        var teacherB = createTeacher("teacher-b@lms.com", "Teacher B");
        userRepository.saveAll(List.of(teacherA, teacherB));

        var courseA = createCourse("INT1001", "Course A");
        var courseB = createCourse("INT1002", "Course B");
        courseRepository.saveAll(List.of(courseA, courseB));

        courseClassRepository.save(createCourseClass(courseA, teacherA, "INT1001-01"));
        courseClassRepository.save(createCourseClass(courseB, teacherB, "INT1002-01"));
    }

    private User createTeacher(String email, String fullName) {
        var user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setFullName(fullName);
        user.setRole(Role.TEACHER);
        return user;
    }

    private Course createCourse(String code, String name) {
        var course = new Course();
        course.setCode(code);
        course.setName(name);
        course.setCredits(3);
        course.setDescription("Seeded for repository test");
        return course;
    }

    private CourseClass createCourseClass(Course course, User teacher, String code) {
        var courseClass = new CourseClass();
        courseClass.setCourse(course);
        courseClass.setCode(code);
        courseClass.setSemester("HK1-2026");
        courseClass.setTeacher(teacher);
        courseClass.setJoinCode(code.replace("-", "").substring(0, 7));
        return courseClass;
    }
}
