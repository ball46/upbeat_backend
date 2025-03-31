package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.response.login_history.LoginHistoryResponse;
import com.example.upbeat_backend.exception.auth.LoginHistoryException;
import com.example.upbeat_backend.model.LoginHistory;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.LoginStatus;
import com.example.upbeat_backend.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.DeviceType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {
    private final LoginHistoryRepository loginHistoryRepository;

    public void recordLoginAttempt(User user, String ipAddress, String userAgent,
                               LoginStatus status, String failureReason) {
        if (user == null) throw new LoginHistoryException.NullUserException();

        if (status == null) throw new LoginHistoryException.NullStatusException();

        ipAddress = (ipAddress != null) ? ipAddress : "0.0.0.0";
        userAgent = (userAgent != null) ? userAgent : "Unknown";

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

    public Page<LoginHistoryResponse> findByUserIdPaginated(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginHistory> historyPage = loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId, pageable);

        return historyPage.map(this::mapToResponse);
    }

    private LoginHistoryResponse mapToResponse(@NotNull LoginHistory history) {
        String dateFormatted = history.getLoginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeFormatted = history.getLoginTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        return LoginHistoryResponse.builder()
                .ipAddress(history.getIpAddress())
                .deviceType(history.getDeviceType())
                .browser(history.getBrowser())
                .os(history.getOs())
                .status(history.getStatus())
                .failureReason(history.getFailureReason())
                .date(dateFormatted)
                .time(timeFormatted)
                .build();
    }

    public int countFailedLoginAttemptsSince(String userId, LocalDateTime since) {
        return loginHistoryRepository.countByUserIdAndStatusAndLoginTimeAfter(
            userId, LoginStatus.INVALID_CREDENTIALS, since);
    }
}