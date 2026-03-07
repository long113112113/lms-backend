package com.example.lms_backend.config;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.lms_backend.dto.course.CreateCourseClassRequest;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.repository.UserRepository;
import com.example.lms_backend.service.CourseClassService;

@Configuration
@Profile("dev")
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEFAULT_SEED_PASSWORD = "123456";
    private static final String DEFAULT_TEACHER_EMAIL = "teacher@lms.com";
    private static final String PAGINATION_TEST_CLASS_CODE = "INT3306-WEB-01";
    private static final int PAGINATION_TEST_STUDENT_COUNT = 27;
    private static final List<SeedUser> BASE_DEV_USERS = List.of(
            new SeedUser("admin@lms.com", "System Admin", Role.ADMIN),
            new SeedUser("admin.support@lms.com", "Support Admin", Role.ADMIN),
            new SeedUser(DEFAULT_TEACHER_EMAIL, "Nguyen Van Teacher", Role.TEACHER),
            new SeedUser("teacher.math@lms.com", "Tran Minh Teacher", Role.TEACHER),
            new SeedUser("teacher.assistant@lms.com", "Assistant Teacher", Role.TEACHER),
            new SeedUser("student1@lms.com", "Tran Thi Student 1", Role.STUDENT),
            new SeedUser("student2@lms.com", "Le Van Student 2", Role.STUDENT),
            new SeedUser("student3@lms.com", "Pham Minh Student 3", Role.STUDENT),
            new SeedUser("student4@lms.com", "Nguyen Anh Student 4", Role.STUDENT),
            new SeedUser("student.repeat1@lms.com", "Nguyen Anh Student", Role.STUDENT),
            new SeedUser("student.repeat2@lms.com", "Nguyen Anh Student", Role.STUDENT),
            new SeedUser("student.qa+web@lms.com", "QA Web Student", Role.STUDENT),
            new SeedUser("student.qa+mobile@lms.com", "QA Mobile Student", Role.STUDENT),
            new SeedUser("student.intern@lms.com", "Intern Student", Role.STUDENT));
    private static final List<SeedUser> DEV_USERS = buildDevUsers();

    private static final List<SeedCourse> DEV_COURSES = List.of(
            new SeedCourse("INT3306", "Lap trinh Web", 3, "Phat trien ung dung web voi Spring Boot va React"),
            new SeedCourse("INT3110", "Kien truc may tinh", 3, "Tong quan ve kien truc va to chuc may tinh"),
            new SeedCourse("INT2215", "Lap trinh huong doi tuong", 3,
                    "Tu duy OOP, thiet ke class va clean code co ban"),
            new SeedCourse("INT2204", "Cau truc du lieu va giai thuat", 4,
                    "Danh sach, cay, do thi va cac ky thuat giai bai toan"),
            new SeedCourse("INT3406", "He quan tri co so du lieu", 3, "SQL, chuan hoa du lieu va toi uu truy van"),
            new SeedCourse("INT3123", "Phat trien ung dung di dong", 3, "Xay dung ung dung mobile va lam viec voi API"),
            new SeedCourse("INT3050", "Kiem thu phan mem", 3, "Unit test, integration test va tu dong hoa kiem thu"),
            new SeedCourse("INT3105", "He dieu hanh", 4, "Process, thread, memory va dong bo trong he dieu hanh"),
            new SeedCourse("INT3407", "Nhap mon hoc may", 3, "Tien xu ly du lieu va mo hinh hoc may co ban"),
            new SeedCourse("INT3414", "An toan thong tin", 3, "Xac thuc, phan quyen va cac lo hong ung dung pho bien"));

    private static final List<SeedCourseClass> DEV_CLASSES = List.of(
            new SeedCourseClass("INT3306", "INT3306-WEB-01", "HK1-2024", DEFAULT_TEACHER_EMAIL),
            new SeedCourseClass("INT3306", "INT3306-WEB-02", "HK2-2024", "teacher.assistant@lms.com"),
            new SeedCourseClass("INT3110", "INT3110-ARCH-01", "HK1-2024", "teacher.math@lms.com"),
            new SeedCourseClass("INT2215", "INT2215-OOP-01", "HK1-2024", DEFAULT_TEACHER_EMAIL),
            new SeedCourseClass("INT2215", "INT2215-OOP-02", "HK2-2024", "teacher.assistant@lms.com"),
            new SeedCourseClass("INT2204", "INT2204-DSA-01", "HK2-2024", "teacher.math@lms.com"),
            new SeedCourseClass("INT3406", "INT3406-DB-01", "HK1-2025", "teacher.math@lms.com"),
            new SeedCourseClass("INT3123", "INT3123-MOB-01", "HK1-2025", "teacher.assistant@lms.com"),
            new SeedCourseClass("INT3050", "INT3050-TEST-01", "HK2-2025", DEFAULT_TEACHER_EMAIL),
            new SeedCourseClass("INT3105", "INT3105-OS-01", "HK2-2025", "teacher.assistant@lms.com"),
            new SeedCourseClass("INT3407", "INT3407-ML-01", "HK1-2025", "teacher.math@lms.com"),
            new SeedCourseClass("INT3414", "INT3414-SEC-01", "HK2-2025", DEFAULT_TEACHER_EMAIL));

    private static final List<SeedEnrollment> BASE_DEV_ENROLLMENTS = List.of(
            new SeedEnrollment("student1@lms.com", "INT3306-WEB-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student1@lms.com", "INT2215-OOP-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student1@lms.com", "INT3406-DB-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student2@lms.com", "INT3306-WEB-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student2@lms.com", "INT3110-ARCH-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student3@lms.com", "INT3306-WEB-02", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student3@lms.com", "INT3123-MOB-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student4@lms.com", "INT2204-DSA-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student4@lms.com", "INT3050-TEST-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student.repeat1@lms.com", "INT2215-OOP-02", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student.repeat2@lms.com", "INT3407-ML-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student.qa+web@lms.com", "INT3306-WEB-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student.qa+web@lms.com", "INT3050-TEST-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student.qa+mobile@lms.com", "INT3123-MOB-01", EnrollmentStatus.ACTIVE),
            new SeedEnrollment("student.intern@lms.com", "INT2215-OOP-01", EnrollmentStatus.LEFT));
    private static final List<SeedEnrollment> DEV_ENROLLMENTS = buildDevEnrollments();

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            CourseRepository courseRepository,
            CourseClassRepository courseClassRepository,
            EnrollmentRepository enrollmentRepository,
            CourseClassService courseClassService,
            PasswordEncoder passwordEncoder) {

        return args -> {
            log.info("Starting Data Seeder for DEV environment...");

            int createdUsers = seedUsers(userRepository, passwordEncoder);
            int createdCourses = seedCourses(courseRepository);
            int createdClasses = seedClasses(userRepository, courseRepository, courseClassRepository,
                    courseClassService);
            int createdEnrollments = seedEnrollments(userRepository, courseClassRepository, enrollmentRepository);

            log.info("Seed summary: users={}, courses={}, classes={}, enrollments={}",
                    createdUsers, createdCourses, createdClasses, createdEnrollments);
            log.info("Default seed password: '{}'", DEFAULT_SEED_PASSWORD);
            log.info("Data Seeder completed!");
        };
    }

    private int seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        int createdUsers = 0;

        for (var seedUser : DEV_USERS) {
            if (userRepository.existsByEmail(seedUser.email())) {
                continue;
            }

            var user = new User();
            user.setEmail(seedUser.email());
            user.setPassword(passwordEncoder.encode(DEFAULT_SEED_PASSWORD));
            user.setFullName(seedUser.fullName());
            user.setRole(seedUser.role());
            userRepository.save(user);
            createdUsers++;
        }

        return createdUsers;
    }

    private int seedCourses(CourseRepository courseRepository) {
        int createdCourses = 0;

        for (var seedCourse : DEV_COURSES) {
            if (courseRepository.existsByCode(seedCourse.code())) {
                continue;
            }

            var course = new Course();
            course.setCode(seedCourse.code());
            course.setName(seedCourse.name());
            course.setCredits(seedCourse.credits());
            course.setDescription(seedCourse.description());
            courseRepository.save(course);
            createdCourses++;
        }

        return createdCourses;
    }

    private int seedClasses(
            UserRepository userRepository,
            CourseRepository courseRepository,
            CourseClassRepository courseClassRepository,
            CourseClassService courseClassService) {
        int createdClasses = 0;

        for (var seedClass : DEV_CLASSES) {
            if (courseClassRepository.existsByCode(seedClass.classCode())) {
                continue;
            }

            var course = courseRepository.findByCode(seedClass.courseCode())
                    .orElseThrow(() -> new IllegalStateException("Missing seed course: " + seedClass.courseCode()));
            var teacher = userRepository.findByEmail(seedClass.teacherEmail())
                    .orElseThrow(() -> new IllegalStateException("Missing seed teacher: " + seedClass.teacherEmail()));

            var request = new CreateCourseClassRequest(
                    course.getId(),
                    seedClass.classCode(),
                    seedClass.semester(),
                    teacher.getId());
            courseClassService.createCourseClass(request);
            createdClasses++;
        }

        return createdClasses;
    }

    private int seedEnrollments(
            UserRepository userRepository,
            CourseClassRepository courseClassRepository,
            EnrollmentRepository enrollmentRepository) {
        int createdEnrollments = 0;

        for (var seedEnrollment : DEV_ENROLLMENTS) {
            var student = userRepository.findByEmail(seedEnrollment.studentEmail())
                    .orElseThrow(
                            () -> new IllegalStateException("Missing seed student: " + seedEnrollment.studentEmail()));
            var courseClass = courseClassRepository.findByCode(seedEnrollment.classCode())
                    .orElseThrow(() -> new IllegalStateException("Missing seed class: " + seedEnrollment.classCode()));

            if (enrollmentRepository.existsByStudentIdAndCourseClassId(student.getId(), courseClass.getId())) {
                continue;
            }

            var enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourseClass(courseClass);
            enrollment.setStatus(seedEnrollment.status());
            enrollmentRepository.save(enrollment);
            createdEnrollments++;
        }

        return createdEnrollments;
    }

    private static List<SeedUser> buildDevUsers() {
        return Stream.concat(
                BASE_DEV_USERS.stream(),
                IntStream.rangeClosed(1, PAGINATION_TEST_STUDENT_COUNT)
                        .mapToObj(DataSeeder::buildPaginationStudent))
                .toList();
    }

    private static SeedUser buildPaginationStudent(int index) {
        return new SeedUser(
                "student.bulk%02d@lms.com".formatted(index),
                "Pagination Student %02d".formatted(index),
                Role.STUDENT);
    }

    private static List<SeedEnrollment> buildDevEnrollments() {
        return Stream.concat(
                BASE_DEV_ENROLLMENTS.stream(),
                IntStream.rangeClosed(1, PAGINATION_TEST_STUDENT_COUNT)
                        .mapToObj(index -> new SeedEnrollment(
                                "student.bulk%02d@lms.com".formatted(index),
                                PAGINATION_TEST_CLASS_CODE,
                                EnrollmentStatus.ACTIVE)))
                .toList();
    }

    private record SeedUser(String email, String fullName, Role role) {
    }

    private record SeedCourse(String code, String name, Integer credits, String description) {
    }

    private record SeedCourseClass(String courseCode, String classCode, String semester, String teacherEmail) {
    }

    private record SeedEnrollment(String studentEmail, String classCode, EnrollmentStatus status) {
    }
}
