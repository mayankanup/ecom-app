package com.ecom.app.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "dev-only-secret-key-change-me-please-32bytes-min";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

    @Test
    void generateToken_thenExtractValidEmail_roundTrips() {
        String token = jwtUtil.generateToken("user@example.com");

        Optional<String> email = jwtUtil.extractValidEmail(token);

        assertThat(email).contains("user@example.com");
    }

    @Test
    void extractValidEmail_returnsEmpty_forGarbageToken() {
        Optional<String> email = jwtUtil.extractValidEmail("not-a-real-token");

        assertThat(email).isEmpty();
    }

    @Test
    void extractValidEmail_returnsEmpty_forExpiredToken() {
        JwtUtil shortLivedUtil = new JwtUtil(SECRET, -1000);

        String token = shortLivedUtil.generateToken("user@example.com");

        assertThat(shortLivedUtil.extractValidEmail(token)).isEmpty();
    }

    @Test
    void extractValidEmail_returnsEmpty_forTokenSignedWithDifferentKey() {
        String tamperedToken = Jwts.builder()
                .subject("user@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("a-completely-different-secret-key-32-bytes+".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtUtil.extractValidEmail(tamperedToken)).isEmpty();
    }
}
