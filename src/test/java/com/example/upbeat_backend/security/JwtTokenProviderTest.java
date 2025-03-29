package com.example.upbeat_backend.security;

import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        ReflectionTestUtils.setField(jwtTokenProvider, "key", key);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", 3600000);
    }

    @Test
    void generateToken_Success() {
        User user = User.builder()
                .id("1")
                .username("testUser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_Success() {
        User user = User.builder()
                .id("1")
                .username("testUser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);
        boolean isValid = jwtTokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void getUserIdFromToken_Success() {
        User user = User.builder()
                .id("1")
                .username("testUser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals("1", extractedUserId);
    }

    @Test
    void validateToken_InvalidToken() {
        boolean isValid = jwtTokenProvider.validateToken("invalid.token.string");
        assertFalse(isValid);
    }

    @Test
    void validateToken_ExpiredToken() {
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", -10000);
        User user = User.builder().id("1").username("testUser").build();

        String token = jwtTokenProvider.generateToken(user);
        boolean isValid = jwtTokenProvider.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    void getUserIdFromToken_InvalidToken() {
        assertThrows(Exception.class, () -> jwtTokenProvider.getUserIdFromToken("invalid.token"));
    }
}
