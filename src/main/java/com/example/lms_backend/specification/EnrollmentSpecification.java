package com.example.lms_backend.specification;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.enums.EnrollmentStatus;

public final class EnrollmentSpecification {
    private static final char LIKE_ESCAPE = '\\';

    private EnrollmentSpecification() {
    }

    public static Specification<Enrollment> buildForClassStudents(UUID courseClassId, String q) {
        Specification<Enrollment> spec = Specification
                .where(hasCourseClassId(courseClassId))
                .and(hasStatus(EnrollmentStatus.ACTIVE));

        String normalizedQuery = normalizeQuery(q);
        if (normalizedQuery != null) {
            spec = spec.and(matchesStudentQuery(normalizedQuery));
        }

        return spec;
    }

    private static Specification<Enrollment> hasCourseClassId(UUID courseClassId) {
        return (root, query, cb) -> cb.equal(root.get("courseClass").get("id"), courseClassId);
    }

    private static Specification<Enrollment> hasStatus(EnrollmentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Enrollment> matchesStudentQuery(String q) {
        return (root, query, cb) -> {
            var student = root.join("student");
            String pattern = "%" + escapeLikePattern(q.toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                    cb.like(cb.lower(student.get("fullName")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(student.get("email")), pattern, LIKE_ESCAPE));
        };
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
