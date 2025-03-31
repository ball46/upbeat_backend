package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.response.login_history.LoginHistoryResponse;
import com.example.upbeat_backend.exception.auth.LoginHistoryException;
import com.example.upbeat_backend.model.LoginHistory;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.LoginStatus;
import com.example.upbeat_backend.repository.LoginHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginHistoryServiceTest {

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @InjectMocks
    private LoginHistoryService loginHistoryService;

    @Captor
    private ArgumentCaptor<LoginHistory> loginHistoryCaptor;

    @Test
    void recordLoginAttempt_Success() {
        User user = User.builder().id("1").username("testUser").build();
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

        when(loginHistoryRepository.save(any(LoginHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        loginHistoryService.recordLoginAttempt(
            user, ipAddress, userAgent, LoginStatus.SUCCESS, null
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        LoginHistory savedHistory = loginHistoryCaptor.getValue();

        assertEquals(user, savedHistory.getUser());
        assertEquals(ipAddress, savedHistory.getIpAddress());
        assertEquals(userAgent, savedHistory.getUserAgent());
        assertEquals(LoginStatus.SUCCESS, savedHistory.getStatus());
        assertNull(savedHistory.getFailureReason());
    }

    @Test
    void recordLoginAttempt_InvalidCredentials() {
        User user = User.builder().id("1").username("testUser").build();
        String failureReason = "Invalid password";

        loginHistoryService.recordLoginAttempt(
            user, "127.0.0.1", "Test Agent", LoginStatus.INVALID_CREDENTIALS, failureReason
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        assertEquals(LoginStatus.INVALID_CREDENTIALS, loginHistoryCaptor.getValue().getStatus());
        assertEquals(failureReason, loginHistoryCaptor.getValue().getFailureReason());
    }

    @Test
    void recordLoginAttempt_AccountSuspended() {
        User user = User.builder().id("1").username("suspendedUser").build();

        loginHistoryService.recordLoginAttempt(
            user, "127.0.0.1", "Test Agent", LoginStatus.SUSPENDED, "Account suspended"
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        assertEquals(LoginStatus.SUSPENDED, loginHistoryCaptor.getValue().getStatus());
    }

    @Test
    void recordLoginAttempt_AccountDeleted() {
        User user = User.builder().id("1").username("deletedUser").build();

        loginHistoryService.recordLoginAttempt(
            user, "127.0.0.1", "Test Agent", LoginStatus.DELETED, "Account deleted"
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        assertEquals(LoginStatus.DELETED, loginHistoryCaptor.getValue().getStatus());
    }

    @Test
    void recordLoginAttempt_UnknownError() {
        User user = User.builder().id("1").username("testUser").build();
        String errorMessage = "Database connection error";

        loginHistoryService.recordLoginAttempt(
            user, "127.0.0.1", "Test Agent", LoginStatus.UNKNOWN_ERROR, errorMessage
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        assertEquals(LoginStatus.UNKNOWN_ERROR, loginHistoryCaptor.getValue().getStatus());
        assertEquals(errorMessage, loginHistoryCaptor.getValue().getFailureReason());
    }

    @Test
    void recordLoginAttempt_ExtractsDeviceInfo() {
        User user = User.builder().id("1").username("testUser").build();
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1";

        loginHistoryService.recordLoginAttempt(
            user, "127.0.0.1", userAgent, LoginStatus.SUCCESS, null
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        LoginHistory saved = loginHistoryCaptor.getValue();

        assertNotNull(saved.getDeviceType());
        assertNotNull(saved.getBrowser());
        assertNotNull(saved.getOs());
    }

    @Test
    void recordLoginAttempt_WithSpecialIpFormats() {
        User user = User.builder().id("1").username("testUser").build();

        loginHistoryService.recordLoginAttempt(
            user, "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "TestAgent", LoginStatus.SUCCESS, null
        );

        verify(loginHistoryRepository).save(loginHistoryCaptor.capture());
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334",
                     loginHistoryCaptor.getValue().getIpAddress());
    }

    @Test
    void recordLoginAttempt_WithNullUser_ShouldThrowException() {
        assertThrows(LoginHistoryException.NullUserException.class, () -> loginHistoryService.recordLoginAttempt(
            null, "127.0.0.1", "TestAgent", LoginStatus.SUCCESS, null
        ));
    }

    @Test
    void recordLoginAttempt_WithNullStatus_ShouldThrowException() {
        User user = User.builder().id("1").username("testUser").build();

        assertThrows(LoginHistoryException.NullStatusException.class, () -> loginHistoryService.recordLoginAttempt(
            user, "127.0.0.1", "TestAgent", null, null
        ));
    }

    @Test
    void findLoginHistoryByUserIdPaginated_ReturnsCorrectHistory() {
        String userId = "1";
        User user = User.builder().id(userId).username("testUser").build();
        int page = 0;
        int size = 10;

        LocalDateTime loginTime = LocalDateTime.of(2023, 5, 15, 14, 30);
        List<LoginHistory> histories = List.of(
                LoginHistory.builder()
                        .id("history1")
                        .user(user)
                        .loginTime(loginTime)
                        .ipAddress("192.168.1.1")
                        .userAgent("Mozilla/5.0")
                        .deviceType("Computer")
                        .browser("Chrome")
                        .os("Windows")
                        .status(LoginStatus.SUCCESS)
                        .build()
        );

        Page<LoginHistory> historyPage = new PageImpl<>(histories);

        when(loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(eq(userId), any(Pageable.class)))
                .thenReturn(historyPage);

        Page<LoginHistoryResponse> result = loginHistoryService.findByUserIdPaginated(userId, page, size);

        assertEquals(1, result.getTotalElements());
        LoginHistoryResponse response = result.getContent().getFirst();
        assertEquals("15/05/2023", response.getDate());
        assertEquals("14:30", response.getTime());
        assertEquals("192.168.1.1", response.getIpAddress());

        verify(loginHistoryRepository).findByUserIdOrderByLoginTimeDesc(eq(userId), any(Pageable.class));
    }

    @Test
    void countFailedLoginAttempts_Last24Hours() {
        String userId = "1";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);

        when(loginHistoryRepository.countByUserIdAndStatusAndLoginTimeAfter(
                eq(userId),
                eq(LoginStatus.INVALID_CREDENTIALS),
                any(LocalDateTime.class)))
            .thenReturn(5);

        int count = loginHistoryService.countFailedLoginAttemptsSince(userId, yesterday);
        
        assertEquals(5, count);
    }
}