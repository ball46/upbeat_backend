package com.example.upbeat_backend.service;

import com.example.upbeat_backend.model.LoginHistory;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.LoginStatus;
import com.example.upbeat_backend.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.DeviceType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {
    private final LoginHistoryRepository loginHistoryRepository;

    public void recordLoginAttempt(User user, String ipAddress, String userAgent,
                               LoginStatus status, String failureReason) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("Login status cannot be null");
        }

        String deviceType = extractDeviceType(userAgent);
        String browser = extractBrowser(userAgent);
        String os = extractOS(userAgent);

        LoginHistory loginHistory = LoginHistory.builder()
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(deviceType)
                .browser(browser)
                .os(os)
                .status(status)
                .failureReason(failureReason)
                .build();

        loginHistoryRepository.save(loginHistory);
    }

    private String extractDeviceType(String userAgentString) {
        if (userAgentString == null) return "Unknown";

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        DeviceType deviceType = userAgent.getOperatingSystem().getDeviceType();

        return deviceType.getName();
    }

    private String extractBrowser(String userAgentString) {
        if (userAgentString == null) return "Unknown";

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        return userAgent.getBrowser().getName();
    }

    private String extractOS(String userAgentString) {
        if (userAgentString == null) return "Unknown";

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        return userAgent.getOperatingSystem().getName();
    }

    public List<LoginHistory> findByUserId(String userId) {
        return loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    public int countFailedLoginAttemptsSince(String userId, LocalDateTime since) {
        return loginHistoryRepository.countByUserIdAndStatusAndLoginTimeAfter(
            userId, LoginStatus.INVALID_CREDENTIALS, since);
    }
}