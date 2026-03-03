package com.example.lms_backend.specification;

import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class chứa các hàm static tạo Specification dùng chung cho mọi
 * Entity.
 */
public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /**
     * Tìm kiếm LIKE không phân biệt hoa thường: WHERE LOWER(field) LIKE '%value%'
     */
    public static <T> Specification<T> containsIgnoreCase(String field, String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get(field)),
                "%" + value.toLowerCase() + "%");
    }

    /**
     * So sánh bằng: WHERE field = value
     */
    public static <T> Specification<T> equals(String field, Object value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /**
     * So sánh >= : WHERE field >= value
     */
    public static <T, Y extends Comparable<Y>> Specification<T> greaterThanOrEqual(String field, Y value) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), value);
    }

    /**
     * So sánh <= : WHERE field <= value
     */
    public static <T, Y extends Comparable<Y>> Specification<T> lessThanOrEqual(String field, Y value) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), value);
    }
}
