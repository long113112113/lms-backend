package com.example.lms_backend.specification;

import org.springframework.data.jpa.domain.Specification;

import com.example.lms_backend.entity.Course;

/**
 * Bộ lọc Specification cho Entity Course.
 * Hỗ trợ tìm kiếm theo code, name, khoảng credits.
 */
public final class CourseSpecification {

    private CourseSpecification() {
    }

    public static Specification<Course> build(String code, String name, Integer minCredits, Integer maxCredits) {
        Specification<Course> spec = Specification.where((Specification<Course>) null);

        if (code != null && !code.isBlank()) {
            spec = spec.and(SpecificationUtils.containsIgnoreCase("code", code));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(SpecificationUtils.containsIgnoreCase("name", name));
        }
        if (minCredits != null) {
            spec = spec.and(SpecificationUtils.greaterThanOrEqual("credits", minCredits));
        }
        if (maxCredits != null) {
            spec = spec.and(SpecificationUtils.lessThanOrEqual("credits", maxCredits));
        }

        return spec;
    }
}
