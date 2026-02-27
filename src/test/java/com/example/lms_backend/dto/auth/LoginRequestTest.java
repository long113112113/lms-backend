package com.example.lms_backend.dto.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidation() {
        var oversized = new LoginRequest("a".repeat(101), "a".repeat(73), "a".repeat(101), "a".repeat(101));
        assertFalse(validator.validate(oversized).isEmpty(), "Should fail oversized inputs");

        var valid = new LoginRequest("test@test.com", "pass123", "device1", "device1");
        assertTrue(validator.validate(valid).isEmpty(), "Should pass valid inputs");
    }
}
