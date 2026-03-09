package com.example.lms_backend.dto.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class JoinClassRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptValidJoinCode() {
        var request = new JoinClassRequest("ABC1234");

        assertTrue(validator.validate(request).isEmpty(), "Expected generated join-code format to be valid");
    }

    @Test
    void shouldRejectNullJoinCode() {
        var violations = validator.validate(new JoinClassRequest(null));

        assertSingleViolation(violations, "joinCode", "Join code is required");
    }

    @Test
    void shouldRejectBlankJoinCode() {
        var violations = validator.validate(new JoinClassRequest("   "));

        assertContainsViolation(violations, "joinCode", "Join code is required");
    }

    @Test
    void shouldRejectJoinCodesOutsideExpectedFormat() {
        assertInvalidFormat("ABC123");
        assertInvalidFormat("ABC12345");
        assertInvalidFormat("abc1234");
        assertInvalidFormat("ABC12$4");
    }

    private void assertInvalidFormat(String joinCode) {
        var violations = validator.validate(new JoinClassRequest(joinCode));

        assertFalse(violations.isEmpty(), "Expected join code '%s' to be rejected".formatted(joinCode));
        assertEquals("joinCode", violations.iterator().next().getPropertyPath().toString());
        assertEquals("Join code must be exactly 7 uppercase letters or digits",
                violations.iterator().next().getMessage());
    }

    private void assertSingleViolation(Set<ConstraintViolation<JoinClassRequest>> violations, String field, String message) {
        assertEquals(1, violations.size());
        var violation = violations.iterator().next();
        assertEquals(field, violation.getPropertyPath().toString());
        assertEquals(message, violation.getMessage());
    }

    private void assertContainsViolation(Set<ConstraintViolation<JoinClassRequest>> violations, String field, String message) {
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(field)
                && violation.getMessage().equals(message)));
    }
}
