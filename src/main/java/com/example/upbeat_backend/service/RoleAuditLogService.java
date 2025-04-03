package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.exception.role.RoleAuditLogException;
import com.example.upbeat_backend.model.RoleAuditLog;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.ActionType;
import com.example.upbeat_backend.repository.RoleAuditLogRepository;
import com.example.upbeat_backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleAuditLogService {
    private final RoleAuditLogRepository roleAuditLogRepository;
    private final RoleRepository roleRepository;

    public void saveRoleAuditLog(ActionType type, String roeId, User user) {
        RoleAuditLog ra = RoleAuditLog.builder()
                .actionType(type)
                .role(roleRepository.findById(roeId).orElse(null))
                .user(user)
                .build();

        try {
            roleAuditLogRepository.save(ra);
        } catch (Exception e) {
            throw new RoleAuditLogException.LoggingFailed("Failed to save role audit log: " + e.getMessage());
        }
    }

    public Page<RoleAuditLog> getAuditLogsByRoleId(String roleId, Pageable pageable) {
        return roleAuditLogRepository.findByRoleId(roleId, pageable);
    }

    public Page<RoleAuditLog> getAuditLogsByUserId(String userId, Pageable pageable) {
        return roleAuditLogRepository.findByUserId(userId, pageable);
    }

    public List<RoleAuditLogResponse> transformAuditLogs(Page<RoleAuditLog> auditLogs) {
        return auditLogs.stream()
                .map(auditLog -> RoleAuditLogResponse.builder()
                        .actionType(auditLog.getActionType())
                        .roleId(auditLog.getRole().getId())
                        .roleName(auditLog.getRole().getName())
                        .userId(auditLog.getUser().getId())
                        .userName(auditLog.getUser().getUsername())
                        .timestamp(auditLog.getTimestamp().toString())
                        .build())
                .toList();
    }
}
