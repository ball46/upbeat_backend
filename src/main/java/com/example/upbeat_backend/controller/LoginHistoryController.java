package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.response.login_history.LoginHistoryResponse;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.service.LoginHistoryService;
import com.example.upbeat_backend.util.DateFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/login-history")
public class LoginHistoryController {
    private final LoginHistoryService loginHistoryService;

    @GetMapping("/limit")
    public ResponseEntity<Page<LoginHistoryResponse>> getAllLoginHistories(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = currentUser.getId();
        Page<LoginHistoryResponse> loginHistories = loginHistoryService.findByUserIdPaginated(userId, page, size);
        return ResponseEntity.ok(loginHistories);
    }

    @GetMapping("/user/{userId}/limit")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).USER_VIEW)")
    public ResponseEntity<Page<LoginHistoryResponse>> getAllLoginHistoriesForUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<LoginHistoryResponse> loginHistories = loginHistoryService.findByUserIdPaginated(userId, page, size);
        return ResponseEntity.ok(loginHistories);
    }

    @GetMapping("/failed-login-attempts")
    public ResponseEntity<Integer> getFailedLoginAttempts(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(name = "since") String dateString) {

        LocalDateTime sinceDate = DateFormat.parseDateString(dateString);
        String userId = currentUser.getId();
        int count = loginHistoryService.countFailedLoginAttemptsSince(userId, sinceDate);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/user/{userId}/failed-login-attempts")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).USER_VIEW)")
    public ResponseEntity<Integer> getFailedLoginAttemptsForUser(
            @PathVariable String userId,
            @RequestParam(name = "since") String dateString) {

        LocalDateTime sinceDate = DateFormat.parseDateString(dateString);
        int count = loginHistoryService.countFailedLoginAttemptsSince(userId, sinceDate);
        return ResponseEntity.ok(count);
    }
}
