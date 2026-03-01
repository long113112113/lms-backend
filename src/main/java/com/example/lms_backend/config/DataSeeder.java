package com.example.lms_backend.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.lms_backend.dto.course.CreateCourseClassRequest;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.UserRepository;
import com.example.lms_backend.service.CourseClassService;

@Configuration
@Profile("dev")
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            CourseRepository courseRepository,
            CourseClassService courseClassService,
            PasswordEncoder passwordEncoder) {

        return args -> {
            log.info("Starting Data Seeder for DEV environment...");

            if (userRepository.count() == 0) {
                var admin = new User();
                admin.setEmail("admin@lms.com");
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setFullName("System Admin");
                admin.setRole(Role.ADMIN);

                var teacher = new User();
                teacher.setEmail("teacher@lms.com");
                teacher.setPassword(passwordEncoder.encode("123456"));
                teacher.setFullName("Nguyen Van Teacher");
                teacher.setRole(Role.TEACHER);

                var student1 = new User();
                student1.setEmail("student1@lms.com");
                student1.setPassword(passwordEncoder.encode("123456"));
                student1.setFullName("Tran Thi Student 1");
                student1.setRole(Role.STUDENT);

                var student2 = new User();
                student2.setEmail("student2@lms.com");
                student2.setPassword(passwordEncoder.encode("123456"));
                student2.setFullName("Le Van Student 2");
                student2.setRole(Role.STUDENT);

                userRepository.saveAll(List.of(admin, teacher, student1, student2));
                log.info("Seeded 4 test users (admin, teacher, student1, student2). Password: '123456'");
            } else {
                log.info("Users already exists, skipping user seed...");
            }

            if (courseRepository.count() == 0) {
                var c1 = new Course();
                c1.setCode("INT3306");
                c1.setName("Lập trình Web");
                c1.setCredits(3);
                c1.setDescription("Môn học phát triển ứng dụng Web với Spring Boot & React");

                var c2 = new Course();
                c2.setCode("INT3110");
                c2.setName("Kiến trúc máy tính");
                c2.setCredits(3);
                c2.setDescription("Tìm hiểu về kiến trúc và tổ chức máy tính");

                courseRepository.saveAll(List.of(c1, c2));
                log.info("Seeded 2 courses (Lập trình Web, Kiến trúc máy tính).");

                var teacherUser = userRepository.findByEmail("teacher@lms.com").get();

                var c1Id = courseRepository.findByCode("INT3306").get().getId();

                var cl1Req = new CreateCourseClassRequest(c1Id, "INT3306 1", "Học Kỳ 1 - 2024", teacherUser.getId());
                var cl2Req = new CreateCourseClassRequest(c1Id, "INT3306 2", "Học Kỳ 1 - 2024", teacherUser.getId());

                var class1 = courseClassService.createCourseClass(cl1Req);
                var class2 = courseClassService.createCourseClass(cl2Req);

                log.info("Seeded 2 classes for INT3306. Join Codes: cl1=[{}], cl2=[{}]", class1.joinCode(),
                        class2.joinCode());
            } else {
                log.info("Courses already exists, skipping course seed...");
            }

            log.info("Data Seeder completed!");
        };
    }
}
