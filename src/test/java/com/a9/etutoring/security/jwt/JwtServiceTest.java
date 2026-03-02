package com.a9.etutoring.security.jwt;

import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.security.UserPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void shouldGenerateAndValidateToken() {
        JwtService jwtService = new JwtService("test-secret-key-test-secret-key-test-secret-key-12345", 3600);
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(),
            "alice",
            "alice@example.com",
            "Alice",
            "Tan",
            "encoded-password",
            UserRole.STUDENT,
            true,
            false
        );

        String token = jwtService.generateToken(principal);

        assertEquals(principal.getUsername(), jwtService.extractUsername(token));
        assertEquals(principal.getId(), jwtService.extractUserId(token));
        assertTrue(jwtService.isTokenValid(token, principal));
    }
}
