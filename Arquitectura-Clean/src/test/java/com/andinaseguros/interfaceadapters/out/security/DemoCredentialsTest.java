package com.andinaseguros.interfaceadapters.out.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DemoCredentialsTest {
    private static final String ADMIN_HASH =
            "$2y$10$A5W8u0DhZaBxlF/6Q3QpU.l3UFW5BjeyS18tZb1DZCkJUk/B1bHxq";

    @Test
    void adminDemoPasswordMatchesSeededHash() {
        var encoder = new BCryptPasswordEncoder();

        assertTrue(encoder.matches("Admin123*", ADMIN_HASH));
        assertFalse(encoder.matches("Admin123!", ADMIN_HASH));
    }
}
