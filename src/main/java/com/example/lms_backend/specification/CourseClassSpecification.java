package com.example.lms_backend.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.enums.EnrollmentStatus;

/**
 * Bộ lọc Specification cho Entity CourseClass.
 * Xử lý JOIN sang Course và Teacher, luôn lọc isDeleted = false.
 */
public final class CourseClassSpecification {

    private CourseClassSpecification() {
    }

    /**
     * Build Specification cho ADMIN — xem tất cả class (chưa bị xóa).
     */
    public static Specification<CourseClass> buildForAdmin(String code, String semester, String courseName,
            String teacherName) {
        return buildBase(code, semester, courseName, teacherName);
    }

    /**
     * Build Specification cho TEACHER — chỉ xem class mình dạy.
     */
    public static Specification<CourseClass> buildForTeacher(UUID teacherId, String code, String semester,
            String courseName) {
        Specification<CourseClass> spec = buildBase(code, semester, courseName, null);
        spec = spec.and((root, query, cb) -> cb.equal(root.get("teacher").get("id"), teacherId));
        return spec;
    }

    /**
     * Build Specification cho STUDENT — chỉ xem class đã enroll (ACTIVE).
     * Dùng subquery vì CourseClass entity không có trường @OneToMany enrollments.
     */
    public static Specification<CourseClass> buildForStudent(UUID studentId, String code, String semester,
            String courseName) {
        Specification<CourseClass> spec = buildBase(code, semester, courseName, null);
        spec = spec.and((root, query, cb) -> {
            var subquery = query.subquery(UUID.class);
            var enrollment = subquery.from(Enrollment.class);
            subquery.select(enrollment.get("courseClass").get("id"))
                    .where(cb.and(
                            cb.equal(enrollment.get("student").get("id"), studentId),
                            cb.equal(enrollment.get("status"), EnrollmentStatus.ACTIVE)));
            return root.get("id").in(subquery);
        });
        return spec;
    }

    private static Specification<CourseClass> buildBase(String code, String semester, String courseName,
            String teacherName) {
        // Luôn lọc isDeleted = false
        Specification<CourseClass> spec = Specification
                .where((root, query, cb) -> cb.isFalse(root.get("isDeleted")));

        if (code != null && !code.isBlank()) {
            spec = spec.and(SpecificationUtils.containsIgnoreCase("code", code));
        }
        if (semester != null && !semester.isBlank()) {
            spec = spec.and(SpecificationUtils.containsIgnoreCase("semester", semester));
        }
        if (courseName != null && !courseName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.join("course").get("name")),
                    "%" + courseName.toLowerCase() + "%"));
        }
        if (teacherName != null && !teacherName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.join("teacher").get("fullName")),
                    "%" + teacherName.toLowerCase() + "%"));
        }

        return spec;
    }
}
