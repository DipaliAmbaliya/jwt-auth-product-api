package com.onedao.auth;

import com.onedao.auth.dto.SignupRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignupValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private int violations(String u, String e, String p, String t) {
        return validator.validate(new SignupRequest(u, e, p, t)).size();
    }

    @Test
    void validRequestPasses() {
        assertEquals(0, violations("john_doe_1", "john_doe@mail.com", "Abcd@123", "Standard User"));
        assertEquals(0, violations("administrator", "admin_1@mail.com", "Abcd@1234567".substring(0, 12), "Administrator"));
    }

    @Test
    void usernameLength() {
        assertTrue(violations("short", "a_b@mail.com", "Abcd@123", "Standard User") > 0);
        assertTrue(violations("x".repeat(51), "a_b@mail.com", "Abcd@123", "Standard User") > 0);
    }

    @Test
    void emailNeedsAtAndUnderscore() {
        assertTrue(violations("john_doe_1", "johndoe@mail.com", "Abcd@123", "Standard User") > 0);
        assertTrue(violations("john_doe_1", "john_doe.mail.com", "Abcd@123", "Standard User") > 0);
    }

    @Test
    void passwordRules() {
        assertTrue(violations("john_doe_1", "a_b@mail.com", "abcd@123", "Standard User") > 0);   // no capital
        assertTrue(violations("john_doe_1", "a_b@mail.com", "ABCD@123", "Standard User") > 0);   // no small
        assertTrue(violations("john_doe_1", "a_b@mail.com", "Abcdef@g", "Standard User") > 0);   // no digit
        assertTrue(violations("john_doe_1", "a_b@mail.com", "Abcdef12", "Standard User") > 0);   // no special
        assertTrue(violations("john_doe_1", "a_b@mail.com", "Ab@1", "Standard User") > 0);       // too short
        assertTrue(violations("john_doe_1", "a_b@mail.com", "Abcd@1234567890", "Standard User") > 0); // too long
    }

    @Test
    void userTypeMustBeKnown() {
        assertTrue(violations("john_doe_1", "a_b@mail.com", "Abcd@123", "Root") > 0);
    }
}
