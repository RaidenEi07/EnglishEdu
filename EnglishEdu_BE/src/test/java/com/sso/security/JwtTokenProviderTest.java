package com.sso.security;

import com.sso.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // HS256 requires at least 32 bytes (256 bits)
        props.setSecret("super-secret-key-that-is-at-least-32-bytes-long!!");
        props.setExpiration(3_600_000L); // 1 hour
        jwtTokenProvider = new JwtTokenProvider(props);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtTokenProvider.generateToken(1L, "testuser");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_producesThreeParts() {
        // JWT has three dot-separated parts: header.payload.signature
        String token = jwtTokenProvider.generateToken(1L, "testuser");

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void getUserIdFromToken_returnsCorrectId() {
        String token = jwtTokenProvider.generateToken(42L, "testuser");

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void getUserIdFromToken_returnsCorrectIdForDifferentUsers() {
        String token1 = jwtTokenProvider.generateToken(1L, "alice");
        String token2 = jwtTokenProvider.generateToken(99L, "bob");

        assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo(99L);
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "testuser");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForTamperedSignature() {
        String token = jwtTokenProvider.generateToken(1L, "testuser");
        // Corrupt the signature (last part)
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot + 1) + "invalidSignatureXYZ";

        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_returnsFalseForBlankToken() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_returnsFalseForRandomString() {
        assertThat(jwtTokenProvider.validateToken("this.is.not.a.jwt")).isFalse();
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtTokenProvider.getExpirationMs()).isEqualTo(3_600_000L);
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("super-secret-key-that-is-at-least-32-bytes-long!!");
        expiredProps.setExpiration(-1000L); // expired 1 second ago
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

        String token = expiredProvider.generateToken(1L, "testuser");

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }
}
