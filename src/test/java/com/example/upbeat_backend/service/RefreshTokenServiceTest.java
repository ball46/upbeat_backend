package com.example.upbeat_backend.service;

import com.example.upbeat_backend.exception.auth.TokenRefreshException;
import com.example.upbeat_backend.model.RefreshToken;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.repository.RefreshTokenRepository;
import com.example.upbeat_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 86400000); // 1 day
    }

    @Test
    void createRefreshToken_Success() {
        // Arrange
        String userId = "1";
        User user = User.builder().id(userId).username("testUser").build();

        RefreshToken newToken = RefreshToken.builder()
                .id("token-id")
                .token("new-refresh-token")
                .user(user)
                .expiryDate(Instant.now().plusMillis(86400000))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(userId);

        // Assert
        assertNotNull(result);
        assertEquals("new-refresh-token", result.getToken());
        assertEquals(user, result.getUser());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_DeletesExistingToken() {
        // Arrange
        String userId = "1";
        User user = User.builder().id(userId).username("testUser").build();

        RefreshToken existingToken = RefreshToken.builder()
                .id("existing-id")
                .token("existing-token")
                .user(user)
                .build();

        RefreshToken newToken = RefreshToken.builder()
                .id("new-id")
                .token("new-token")
                .user(user)
                .expiryDate(Instant.now().plusMillis(86400000))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);

        RefreshToken result = refreshTokenService.createRefreshToken(userId);

        assertNotNull(result);
        assertEquals("new-id", result.getId());
        assertEquals("new-token", result.getToken());
        assertEquals(user, result.getUser());
        assertTrue(result.getExpiryDate().isAfter(Instant.now()));

        verify(refreshTokenRepository).delete(existingToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_UserNotFound() {
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> refreshTokenService.createRefreshToken(userId));
    }

    @Test
    void verifyExpiration_ValidToken() {
        // Arrange
        User user = User.builder().id("1").username("testUser").build();
        RefreshToken validToken = RefreshToken.builder()
                .id("token-id")
                .token("valid-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600)) // Future time
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> refreshTokenService.verifyExpiration(validToken));
    }

    @Test
    void verifyExpiration_ExpiredToken() {
        // Arrange
        User user = User.builder().id("1").username("testUser").build();
        RefreshToken expiredToken = RefreshToken.builder()
                .id("token-id")
                .token("expired-token")
                .user(user)
                .expiryDate(Instant.now().minusSeconds(3600))
                .build();

        // Act & Assert
        assertThrows(TokenRefreshException.TokenExpired.class,
                () -> refreshTokenService.verifyExpiration(expiredToken));
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void findByToken_TokenExists() {
        // Arrange
        String tokenValue = "existing-token";
        User user = User.builder().id("1").username("testUser").build();
        RefreshToken token = RefreshToken.builder()
                .id("token-id")
                .token(tokenValue)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        // Act
        RefreshToken result = refreshTokenService.findByToken(tokenValue);

        // Assert
        assertNotNull(result);
        assertEquals(tokenValue, result.getToken());
    }

    @Test
    void findByToken_TokenNotFound() {
        // Arrange
        String tokenValue = "nonexistent-token";
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TokenRefreshException.TokenNotFound.class,
                () -> refreshTokenService.findByToken(tokenValue));
    }

    @Test
    void deleteByUserId_Success() {
        // Arrange
        String userId = "1";

        // Act
        refreshTokenService.deleteByUserId(userId);

        // Assert
        verify(refreshTokenRepository).deleteByUserId(userId);
    }
}