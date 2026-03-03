package com.example.lms_backend.specification;

import org.springframework.data.jpa.domain.Specification;

import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.Role;

/**
 * Bộ lọc Specification cho Entity User.
 * Hỗ trợ tìm kiếm theo email, fullName, role.
 */
public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> build(String email, String fullName, Role role) {
        Specification<User> spec = Specification.where((Specification<User>) null);

        if (email != null && !email.isBlank()) {
            spec = spec.and(SpecificationUtils.containsIgnoreCase("email", email));
        }
        if (fullName != null && !fullName.isBlank()) {
            spec = spec.and(SpecificationUtils.containsIgnoreCase("fullName", fullName));
        }
        if (role != null) {
            spec = spec.and(SpecificationUtils.equals("role", role));
        }

        return spec;
    }
}
