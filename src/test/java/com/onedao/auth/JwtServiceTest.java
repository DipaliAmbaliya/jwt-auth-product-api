package com.onedao.auth;

import com.onedao.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789-0123456789-0123456789-01".getBytes());

    private UserDetails user(String role) {
        return User.withUsername("john_doe_1").password("hash").roles(role).build();
    }

    @Test
    void generatesAndParsesToken() {
        JwtService svc = new JwtService(SECRET, 5);
        var claims = svc.parse(svc.generateToken(user("ADMIN"))).orElseThrow();
        assertEquals("john_doe_1", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("role"));
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());
    }

    @Test
    void rejectsExpiredAndTamperedTokens() {
        JwtService expired = new JwtService(SECRET, -1);
        assertTrue(expired.parse(expired.generateToken(user("USER"))).isEmpty());

        JwtService svc = new JwtService(SECRET, 5);
        String good = svc.generateToken(user("USER"));
        assertTrue(svc.parse(good + "x").isEmpty());
        assertTrue(svc.parse("garbage").isEmpty());
    }
}
