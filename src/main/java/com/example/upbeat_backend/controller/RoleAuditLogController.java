package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.service.RoleAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/role-audit-log")
public class RoleAuditLogController {
    private final RoleAuditLogService roleAuditLogService;

    @GetMapping("/by-role-id/{roleId}")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_VIEW)")
    public ResponseEntity<Page<RoleAuditLogResponse>> getAuditLogsByRoleId(
            @PathVariable String roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RoleAuditLogResponse> auditLogs = roleAuditLogService.getAuditLogsByRoleId(roleId, page, size);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/by-user-id/{userId}")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_VIEW)")
    public ResponseEntity<Page<RoleAuditLogResponse>> getAuditLogsByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RoleAuditLogResponse> auditLogs = roleAuditLogService.getAuditLogsByUserId(userId, page, size);
        return ResponseEntity.ok(auditLogs);
    }
}
