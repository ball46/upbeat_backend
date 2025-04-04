package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.exception.role.RoleAuditLogException;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.exception.user.UserException;
import com.example.upbeat_backend.model.RoleAuditLog;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.ActionType;
import com.example.upbeat_backend.repository.RoleAuditLogRepository;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleAuditLogService {
    private final RoleAuditLogRepository roleAuditLogRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public void saveRoleAuditLog(ActionType type, String roleId, User user) {
        if (type == null) {
            throw new RoleAuditLogException.LoggingFailed("ActionType cannot be null");
        }

        if (roleId == null || roleId.trim().isEmpty()) {
            throw new RoleAuditLogException.LoggingFailed("Role ID cannot be null or empty");
        }

        if (user == null) {
            throw new RoleAuditLogException.LoggingFailed("User cannot be null");
        }

        RoleAuditLog ra = RoleAuditLog.builder()
                .actionType(type)
                .role(roleRepository.findById(roleId).orElse(null))
                .user(user)
                .build();

        try {
            roleAuditLogRepository.save(ra);
        } catch (Exception e) {
            throw new RoleAuditLogException.LoggingFailed("Failed to save role audit log: " + e.getMessage());
        }
    }

    public Page<RoleAuditLogResponse> getAuditLogsByRoleId(String roleId, int page, int size) {
        if (!roleRepository.existsById(roleId)) {
            throw new RoleException.RoleNotFound(roleId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<RoleAuditLog> auditLogsPage = roleAuditLogRepository.findByRoleId(roleId, pageable);
        return auditLogsPage.map(this::mapToResponse);
    }

    public Page<RoleAuditLogResponse> getAuditLogsByUserId(String userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new UserException.NotFound(userId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<RoleAuditLog> auditLogsPage = roleAuditLogRepository.findByUserId(userId, pageable);
        return auditLogsPage.map(this::mapToResponse);
    }

    private RoleAuditLogResponse mapToResponse(@NotNull RoleAuditLog auditLog) {
        return RoleAuditLogResponse.builder()
                .actionType(auditLog.getActionType())
                .roleId(auditLog.getRole().getId())
                .roleName(auditLog.getRole().getName())
                .userId(auditLog.getUser().getId())
                .userName(auditLog.getUser().getUsername())
                .timestamp(auditLog.getTimestamp().toString())
                .build();
    }
}
