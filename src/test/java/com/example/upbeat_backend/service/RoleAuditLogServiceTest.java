package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.exception.role.RoleAuditLogException;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.exception.user.UserException;
import com.example.upbeat_backend.model.RoleAuditLog;
import com.example.upbeat_backend.repository.RoleAuditLogRepository;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class RoleAuditLogServiceTest {
    @Mock
    private RoleAuditLogRepository roleAuditLogRepository;

    @InjectMocks
    private RoleAuditLogService roleAuditLogService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void saveRoleAuditLog_CreateActionSuccess() {
        ActionType actionType = ActionType.CREATE;
        String roleId = "role-123";
        String roleName = "ADMIN";
        User user = User.builder()
                .id("user-123")
                .username("admin")
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(
                Role.builder().id(roleId).name(roleName).build()));

        roleAuditLogService.saveRoleAuditLog(actionType, roleId, user);

        verify(roleAuditLogRepository).save(argThat(log ->
                log.getActionType() == ActionType.CREATE &&
                log.getRole().getId().equals(roleId) &&
                log.getRole().getName().equals(roleName) &&
                log.getUser().getId().equals(user.getId()) &&
                log.getUser().getUsername().equals(user.getUsername())
        ));
    }

    @Test
    void saveRoleAuditLog_DeleteActionSuccess() {
        ActionType actionType = ActionType.DELETE;
        String roleId = "role-to-delete";
        User user = User.builder().id("user-123").username("admin").build();

        roleAuditLogService.saveRoleAuditLog(actionType, roleId, user);

        verify(roleAuditLogRepository).save(any(RoleAuditLog.class));
    }

    @Test
    void saveRoleAuditLog_UpdateNameActionSuccess() {
        ActionType actionType = ActionType.UPDATE_NAME;
        String roleId = "role-123";
        String roleName = "NEW_NAME";
        User user = User.builder().id("user-123").username("admin").build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(
                Role.builder().id(roleId).name(roleName).build()));

        roleAuditLogService.saveRoleAuditLog(actionType, roleId, user);

        verify(roleAuditLogRepository).save(argThat(log ->
                log.getActionType() == ActionType.UPDATE_NAME &&
                log.getRole().getId().equals(roleId)
        ));
    }

    @Test
    void saveRoleAuditLog_UpdatePermissionsActionSuccess() {
        ActionType actionType = ActionType.UPDATE_PERMISSIONS;
        String roleId = "role-123";
        String roleName = "ADMIN";
        User user = User.builder().id("user-123").username("admin").build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(
                Role.builder().id(roleId).name(roleName).build()));

        roleAuditLogService.saveRoleAuditLog(actionType, roleId, user);

        verify(roleAuditLogRepository).save(any(RoleAuditLog.class));
    }

    @Test
    void saveRoleAuditLog_NullUser() {
        ActionType actionType = ActionType.CREATE;
        String roleId = "role-123";

        assertThrows(RoleAuditLogException.LoggingFailed.class, () ->
                roleAuditLogService.saveRoleAuditLog(actionType, roleId, null));

        verify(roleAuditLogRepository, never()).save(any());
    }

    @Test
    void saveRoleAuditLog_NullActionType() {
        String roleId = "role-123";
        User user = User.builder().id("user-123").username("admin").build();

        assertThrows(RoleAuditLogException.LoggingFailed.class, () ->
                roleAuditLogService.saveRoleAuditLog(null, roleId, user));

        verify(roleAuditLogRepository, never()).save(any());
    }

    @Test
    void saveRoleAuditLog_EmptyRoleId() {
        ActionType actionType = ActionType.CREATE;
        String roleId = "";
        User user = User.builder().id("user-123").username("admin").build();

        assertThrows(RoleAuditLogException.LoggingFailed.class, () ->
                roleAuditLogService.saveRoleAuditLog(actionType, roleId, user));

        verify(roleAuditLogRepository, never()).save(any());
    }

    @Test
    void saveRoleAuditLog_RepositoryError() {
        ActionType actionType = ActionType.CREATE;
        String roleId = "role-123";
        String roleName = "ADMIN";
        User user = User.builder().id("user-123").username("admin").build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(
                Role.builder().id(roleId).name(roleName).build()));
        when(roleAuditLogRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RoleAuditLogException.LoggingFailed.class, () ->
                roleAuditLogService.saveRoleAuditLog(actionType, roleId, user));
    }

    @Test
    void getAuditLogsByRoleId_Success() {
        String roleId = "role-123";
        int page = 0;
        int size = 10;

        when(roleRepository.existsById(roleId)).thenReturn(true);

        List<RoleAuditLog> auditLogs = List.of(
            RoleAuditLog.builder()
                .id("log-1")
                .actionType(ActionType.CREATE)
                .role(Role.builder().id(roleId).name("ADMIN").build())
                .user(User.builder().id("user-1").username("admin").build())
                .timestamp(LocalDateTime.now())
                .build()
        );

        Page<RoleAuditLog> auditLogPage = new PageImpl<>(auditLogs, PageRequest.of(page, size), 1);

        when(roleAuditLogRepository.findByRoleId(eq(roleId), any(Pageable.class)))
            .thenReturn(auditLogPage);

        Page<RoleAuditLogResponse> result = roleAuditLogService.getAuditLogsByRoleId(roleId, page, size);

        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(1, result.getTotalPages());
        assertEquals(ActionType.CREATE, result.getContent().getFirst().getActionType());

        verify(roleAuditLogRepository).findByRoleId(eq(roleId), any(Pageable.class));
    }

    @Test
    void getAuditLogsByRoleId_EmptyResult() {
        String roleId = "role-123";
        int page = 0;
        int size = 10;

        when(roleRepository.existsById(roleId)).thenReturn(true);

        Page<RoleAuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);

        when(roleAuditLogRepository.findByRoleId(eq(roleId), any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<RoleAuditLogResponse> result = roleAuditLogService.getAuditLogsByRoleId(roleId, page, size);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAuditLogsByRoleId_Pagination() {
        String roleId = "role-123";
        int page = 1;
        int size = 5;

        when(roleRepository.existsById(roleId)).thenReturn(true);

        List<RoleAuditLog> logs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            logs.add(RoleAuditLog.builder()
                .id("log-" + (i + 6)) // Second page logs
                .actionType(ActionType.UPDATE_NAME)
                .role(Role.builder().id(roleId).name("ROLE").build())
                .user(User.builder().id("user-1").username("admin").build())
                .timestamp(LocalDateTime.now())
                .build());
        }

        Page<RoleAuditLog> secondPage = new PageImpl<>(logs, PageRequest.of(page, size), 15); // 15 total logs

        when(roleAuditLogRepository.findByRoleId(eq(roleId), any(Pageable.class)))
            .thenReturn(secondPage);

        Page<RoleAuditLogResponse> result = roleAuditLogService.getAuditLogsByRoleId(roleId, page, size);

        assertEquals(15, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(3, result.getTotalPages());
        assertEquals(5, result.getContent().size());
    }

    @Test
    void getAuditLogsByRoleId_InvalidPageSize() {
        String roleId = "role-123";
        int page = 0;
        int size = -1;

        when(roleRepository.existsById(roleId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            roleAuditLogService.getAuditLogsByRoleId(roleId, page, size));

        verify(roleAuditLogRepository, never()).findByRoleId(anyString(), any(Pageable.class));
    }

    @Test
    void getAuditLogsByRoleId_RoleNotFound() {
        String roleId = "non-existent";
        int page = 0;
        int size = 10;

        when(roleRepository.existsById(roleId)).thenReturn(false);

        assertThrows(RoleException.RoleNotFound.class, () ->
            roleAuditLogService.getAuditLogsByRoleId(roleId, page, size));
    }

    @Test
    void getAuditLogsByRoleId_RepositoryError() {
        String roleId = "role-123";
        int page = 0;
        int size = 10;

        when(roleRepository.existsById(roleId)).thenReturn(true);

        when(roleAuditLogRepository.findByRoleId(eq(roleId), any(Pageable.class)))
            .thenThrow(new RoleAuditLogException.LoggingFailed("Database error"));

        assertThrows(RoleAuditLogException.LoggingFailed.class, () ->
            roleAuditLogService.getAuditLogsByRoleId(roleId, page, size));
    }

    @Test
    void getAuditLogsByUserId_Success() {
        String userId = "user-123";
        int page = 0;
        int size = 10;

        List<RoleAuditLog> auditLogs = List.of(
            createMockRoleAuditLog("role-1", userId),
            createMockRoleAuditLog("role-2", userId)
        );
        Page<RoleAuditLog> auditLogPage = new PageImpl<>(auditLogs);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleAuditLogRepository.findByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(auditLogPage);

        Page<RoleAuditLogResponse> result = roleAuditLogService.getAuditLogsByUserId(userId, page, size);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userRepository).existsById(userId);
        verify(roleAuditLogRepository).findByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getAuditLogsByUserId_EmptyResult() {
        String userId = "user-123";
        int page = 0;
        int size = 10;

        Page<RoleAuditLog> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleAuditLogRepository.findByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<RoleAuditLogResponse> result = roleAuditLogService.getAuditLogsByUserId(userId, page, size);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(userRepository).existsById(userId);
        verify(roleAuditLogRepository).findByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getAuditLogsByUserId_Pagination() {
        String userId = "user-123";
        int page = 1;
        int size = 5;

        List<RoleAuditLog> auditLogs = List.of(
            createMockRoleAuditLog("role-1", userId),
            createMockRoleAuditLog("role-2", userId)
        );
        Page<RoleAuditLog> auditLogPage = new PageImpl<>(auditLogs);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleAuditLogRepository.findByUserId(eq(userId), any(Pageable.class)))
            .thenReturn(auditLogPage);

        Page<RoleAuditLogResponse> result = roleAuditLogService.getAuditLogsByUserId(userId, page, size);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());

        verify(userRepository).existsById(userId);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(roleAuditLogRepository).findByUserId(eq(userId), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(page, capturedPageable.getPageNumber());
        assertEquals(size, capturedPageable.getPageSize());
    }

    @Test
    void getAuditLogsByUserId_InvalidPageSize() {
        String userId = "user-123";
        int page = -1;
        int size = 0;

        when(userRepository.existsById(userId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            roleAuditLogService.getAuditLogsByUserId(userId, page, size));

        verify(userRepository).existsById(userId);
        verify(roleAuditLogRepository, never()).findByUserId(anyString(), any(Pageable.class));
    }

    @Test
    void getAuditLogsByUserId_UserNotFound() {
        String userId = "non-existent";
        int page = 0;
        int size = 10;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserException.NotFound.class, () ->
            roleAuditLogService.getAuditLogsByUserId(userId, page, size));

        verify(userRepository).existsById(userId);
        verify(roleAuditLogRepository, never()).findByUserId(anyString(), any(Pageable.class));
    }

    @Test
    void getAuditLogsByUserId_RepositoryError() {
        String userId = "user-123";
        int page = 0;
        int size = 10;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(roleAuditLogRepository.findByUserId(eq(userId), any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () ->
            roleAuditLogService.getAuditLogsByUserId(userId, page, size));

        verify(userRepository).existsById(userId);
        verify(roleAuditLogRepository).findByUserId(eq(userId), any(Pageable.class));
    }

    private RoleAuditLog createMockRoleAuditLog(String roleId, String userId) {
        Role role = Role.builder().id(roleId).name("TEST_ROLE").build();
        User user = User.builder().id(userId).username("user_" + userId).build();

        return RoleAuditLog.builder()
                .id("log-" + UUID.randomUUID().toString().substring(0, 8))
                .actionType(ActionType.CREATE)
                .role(role)
                .user(user)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
