package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.exception.role.RoleAuditLogException;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.ActionType;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.eq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import com.example.upbeat_backend.dto.request.role.UpdateRoleNameRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRolePermissionRequest;
import com.example.upbeat_backend.dto.response.role.GetListOfRoleResponse;
import com.example.upbeat_backend.dto.response.role.GetRoleDetailResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoleAuditLogService roleAuditLogService;

    @InjectMocks
    private RoleService roleService;

    @Captor
    private ArgumentCaptor<Role> roleCaptor;

    private static @NotNull Map<String, Boolean> getStringBooleanMap() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("user_view", true);
        permissions.put("user_create", false);
        permissions.put("user_edit", true);
        permissions.put("user_delete", false);
        permissions.put("role_view", true);
        permissions.put("role_create", true);
        permissions.put("role_edit", true);
        permissions.put("role_delete", true);
        permissions.put("content_view", true);
        permissions.put("content_create", true);
        permissions.put("content_edit", true);
        permissions.put("content_delete", true);
        permissions.put("system_settings", true);
        return permissions;
    }

    @Test
    void addRole_Success() {
        String roleId = "role-123";
        String roleName = "ADMIN";
        Map<String, Boolean> permissions = getStringBooleanMap();

        AddRoleRequest request = AddRoleRequest.builder()
                .name(roleName)
                .permissions(permissions)
                .build();

        User mockUser = User.builder()
                .id("user-123")
                .username("testUser")
                .build();
        
        when(roleRepository.existsByName(roleName)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role savedRole = invocation.getArgument(0);
            savedRole.setId(roleId); // Set ID to role being saved
            return savedRole;
        });
        when(userService.getCurrentUser()).thenReturn(mockUser);
        doNothing().when(roleAuditLogService).saveRoleAuditLog(any(), anyString(), any());


        String result = roleService.addRole(request);

        assertEquals("Role created successfully.", result);
        verify(roleRepository).save(roleCaptor.capture());
        Role savedRole = roleCaptor.getValue();
        assertEquals(roleName, savedRole.getName());
        assertEquals(permissions, savedRole.getPermissions());

        verify(userService).getCurrentUser();
        verify(roleAuditLogService).saveRoleAuditLog(eq(ActionType.CREATE), eq(roleId), eq(mockUser));
    }

    @Test
    void addRole_RoleAlreadyExists() {
        String roleName = "ADMIN";
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("user_view", true);
        
        AddRoleRequest request = AddRoleRequest.builder()
                .name(roleName)
                .permissions(permissions)
                .build();

        when(roleRepository.existsByName(roleName)).thenReturn(true);

        assertThrows(RoleException.RoleAlreadyExists.class, () ->
                roleService.addRole(request)
        );
        verify(roleRepository, never()).save(any());
    }

    @Test
    void addRole_DatabaseError() {
        String roleName = "ERROR_ROLE";
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("user_view", true);
        
        AddRoleRequest request = AddRoleRequest.builder()
                .name(roleName)
                .permissions(permissions)
                .build();

        when(roleRepository.existsByName(roleName)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenThrow(new RuntimeException("Database error"));

        RoleException.CreationFailed exception = assertThrows(RoleException.CreationFailed.class, () ->
                roleService.addRole(request)
        );
        
        assertTrue(exception.getMessage().contains("Failed to create role"));
    }

    @Test
    void addRole_AuditLogError() {
        String roleName = "TEST_ROLE";
        String roleId = "role-123";
        Map<String, Boolean> permissions = getStringBooleanMap();
        AddRoleRequest request = AddRoleRequest.builder()
                .name(roleName)
                .permissions(permissions)
                .build();
        User mockUser = User.builder()
                .id("user-123")
                .username("testUser")
                .build();

        when(roleRepository.existsByName(roleName)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role savedRole = invocation.getArgument(0);
            savedRole.setId(roleId);
            return savedRole;
        });
        when(userService.getCurrentUser()).thenReturn(mockUser);
        doThrow(new RuntimeException("Audit log error"))
                .when(roleAuditLogService).saveRoleAuditLog(any(), eq(roleId), any());

        RoleException.CreationFailed exception = assertThrows(RoleException.CreationFailed.class,
                () -> roleService.addRole(request));
        assertTrue(exception.getMessage().contains("Failed to create role"));

        verify(roleRepository).save(any());
        verify(roleAuditLogService).saveRoleAuditLog(any(), eq(roleId), any());
    }

    @Test
    void deleteRole_Success() {
        String roleId = "role-123";
        String roleName = "TEST_ROLE";
        Role mockRole = Role.builder().id(roleId).name(roleName).build();
        User mockUser = User.builder().id("user-123").build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(userRepository.countByRoleId(roleId)).thenReturn(0);
        when(userService.getCurrentUser()).thenReturn(mockUser);

        String result = roleService.deleteRole(roleId);

        assertEquals("Role deleted successfully.", result);
        verify(roleRepository).delete(mockRole);
        verify(roleAuditLogService).saveRoleAuditLog(eq(ActionType.DELETE), eq(roleId), eq(mockUser));
    }

    @Test
    void deleteRole_RoleNotFound() {
        String roleId = "non-existent-role";
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(RoleException.RoleNotFound.class, () -> roleService.deleteRole(roleId));
        verify(roleRepository, never()).delete(any());
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void deleteRole_DefaultRole() {
        String roleId = "default-role";
        String defaultRoleName = "DEFAULT";
        Role mockRole = Role.builder().id(roleId).name(defaultRoleName).build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        ReflectionTestUtils.setField(roleService, "defaultRoleName", defaultRoleName);

        assertThrows(RoleException.ForbiddenOperation.class, () -> roleService.deleteRole(roleId));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void deleteRole_RoleAssignedToUsers() {
        String roleId = "role-with-users";
        String roleName = "USER_ROLE";
        Role mockRole = Role.builder().id(roleId).name(roleName).build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(userRepository.countByRoleId(roleId)).thenReturn(5);

        RoleException.DeletionConflict exception = assertThrows(RoleException.DeletionConflict.class,
                () -> roleService.deleteRole(roleId));
        assertTrue(exception.getMessage().contains("Cannot delete role that is assigned to 5 users"));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void deleteRole_DeletionError() {
        String roleId = "role-error";
        String roleName = "ERROR_ROLE";
        Role mockRole = Role.builder().id(roleId).name(roleName).build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(userRepository.countByRoleId(roleId)).thenReturn(0);
        doThrow(new RuntimeException("Database error")).when(roleRepository).delete(any());

        RoleException.DeletionFailed exception = assertThrows(RoleException.DeletionFailed.class,
                () -> roleService.deleteRole(roleId));
        assertTrue(exception.getMessage().contains("Failed to delete role"));

        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRoleName_Success() {
        String roleId = "role-123";
        String oldName = "OLD_ROLE";
        String newName = "NEW_ROLE";
        Role mockRole = Role.builder().id(roleId).name(oldName).build();
        User mockUser = User.builder().id("user-123").build();
        UpdateRoleNameRequest request = UpdateRoleNameRequest.builder()
                .roleId(roleId)
                .name(newName)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleRepository.existsByName(newName)).thenReturn(false);
        when(userService.getCurrentUser()).thenReturn(mockUser);

        String result = roleService.updateRoleName(request);

        assertEquals("Role name updated successfully.", result);
        assertEquals(newName, mockRole.getName());
        verify(roleRepository).save(mockRole);
        verify(roleAuditLogService).saveRoleAuditLog(eq(ActionType.UPDATE_NAME), eq(roleId), eq(mockUser));
    }

    @Test
    void updateRoleName_RoleNotFound() {
        String roleId = "non-existent";
        UpdateRoleNameRequest request = UpdateRoleNameRequest.builder()
                .roleId(roleId)
                .name("NEW_NAME")
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(RoleException.RoleNotFound.class, () -> roleService.updateRoleName(request));
        verify(roleRepository, never()).save(any());
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRoleName_DefaultRole() {
        String roleId = "default-role";
        String defaultRoleName = "DEFAULT";
        Role mockRole = Role.builder().id(roleId).name(defaultRoleName).build();
        UpdateRoleNameRequest request = UpdateRoleNameRequest.builder()
                .roleId(roleId)
                .name("NEW_NAME")
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        ReflectionTestUtils.setField(roleService, "defaultRoleName", defaultRoleName);

        RoleException.ForbiddenOperation exception = assertThrows(RoleException.ForbiddenOperation.class,
                () -> roleService.updateRoleName(request));
        assertTrue(exception.getMessage().contains("Cannot update default role"));
        verify(roleRepository, never()).save(any());
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRoleName_NameAlreadyExists() {
        String roleId = "role-123";
        String oldName = "OLD_ROLE";
        String newName = "EXISTING_ROLE";
        Role mockRole = Role.builder().id(roleId).name(oldName).build();
        UpdateRoleNameRequest request = UpdateRoleNameRequest.builder()
                .roleId(roleId)
                .name(newName)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleRepository.existsByName(newName)).thenReturn(true);

        assertThrows(RoleException.RoleAlreadyExists.class, () -> roleService.updateRoleName(request));
        assertEquals(oldName, mockRole.getName()); // Name should not change
        verify(roleRepository, never()).save(any());
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRoleName_DatabaseError() {
        String roleId = "role-123";
        String oldName = "OLD_ROLE";
        String newName = "NEW_ROLE";
        Role mockRole = Role.builder().id(roleId).name(oldName).build();
        UpdateRoleNameRequest request = UpdateRoleNameRequest.builder()
                .roleId(roleId)
                .name(newName)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleRepository.existsByName(newName)).thenReturn(false);
        when(roleRepository.save(any())).thenThrow(new DataIntegrityViolationException("Database error"));

        RoleException.UpdateFailed exception = assertThrows(RoleException.UpdateFailed.class,
                () -> roleService.updateRoleName(request));
        assertTrue(exception.getMessage().contains("Failed to update role name"));
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRoleName_AuditLogError() {
        String roleId = "role-123";
        String oldName = "OLD_ROLE";
        String newName = "NEW_ROLE";
        Role mockRole = Role.builder().id(roleId).name(oldName).build();
        User mockUser = User.builder().id("user-123").build();
        UpdateRoleNameRequest request = UpdateRoleNameRequest.builder()
                .roleId(roleId)
                .name(newName)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleRepository.existsByName(newName)).thenReturn(false);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        doThrow(new RoleAuditLogException.LoggingFailed("Audit log error"))
                .when(roleAuditLogService).saveRoleAuditLog(any(), anyString(), any());

        assertThrows(RoleAuditLogException.LoggingFailed.class,
                () -> roleService.updateRoleName(request));

        verify(roleRepository).save(any());
        verify(roleAuditLogService).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRolePermission_Success() {
        String roleId = "role-123";
        String roleName = "TEST_ROLE";
        Map<String, Boolean> oldPermissions = new HashMap<>();
        oldPermissions.put("user_view", true);
        oldPermissions.put("user_edit", false);

        Map<String, Boolean> newPermissions = getStringBooleanMap();

        Role mockRole = Role.builder().id(roleId).name(roleName).permissions(oldPermissions).build();
        User mockUser = User.builder().id("user-123").build();
        UpdateRolePermissionRequest request = UpdateRolePermissionRequest.builder()
                .roleId(roleId)
                .permissions(newPermissions)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(userService.getCurrentUser()).thenReturn(mockUser);

        String result = roleService.updateRolePermission(request);

        assertEquals("Role permissions updated successfully.", result);
        assertEquals(newPermissions, mockRole.getPermissions());
        verify(roleRepository).save(mockRole);
        verify(roleAuditLogService).saveRoleAuditLog(eq(ActionType.UPDATE_PERMISSIONS), eq(roleId), eq(mockUser));
    }

    @Test
    void updateRolePermission_RoleNotFound() {
        String roleId = "non-existent";
        Map<String, Boolean> permissions = getStringBooleanMap();
        UpdateRolePermissionRequest request = UpdateRolePermissionRequest.builder()
                .roleId(roleId)
                .permissions(permissions)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(RoleException.RoleNotFound.class, () -> roleService.updateRolePermission(request));
        verify(roleRepository, never()).save(any());
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRolePermission_DefaultRole() {
        String roleId = "default-role";
        String defaultRoleName = "DEFAULT";
        Map<String, Boolean> permissions = getStringBooleanMap();
        Role mockRole = Role.builder().id(roleId).name(defaultRoleName).build();
        UpdateRolePermissionRequest request = UpdateRolePermissionRequest.builder()
                .roleId(roleId)
                .permissions(permissions)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        ReflectionTestUtils.setField(roleService, "defaultRoleName", defaultRoleName);

        RoleException.ForbiddenOperation exception = assertThrows(RoleException.ForbiddenOperation.class,
                () -> roleService.updateRolePermission(request));
        assertTrue(exception.getMessage().contains("Cannot update permissions of default role"));
        verify(roleRepository, never()).save(any());
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRolePermission_DatabaseError() {
        String roleId = "role-123";
        String roleName = "TEST_ROLE";
        Map<String, Boolean> permissions = getStringBooleanMap();
        Role mockRole = Role.builder().id(roleId).name(roleName).build();
        UpdateRolePermissionRequest request = UpdateRolePermissionRequest.builder()
                .roleId(roleId)
                .permissions(permissions)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleRepository.save(any())).thenThrow(new DataIntegrityViolationException("Database error"));

        RoleException.UpdateFailed exception = assertThrows(RoleException.UpdateFailed.class,
                () -> roleService.updateRolePermission(request));
        assertTrue(exception.getMessage().contains("Failed to update role permissions"));
        verify(roleAuditLogService, never()).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void updateRolePermission_AuditLogError() {
        String roleId = "role-123";
        String roleName = "TEST_ROLE";
        Map<String, Boolean> permissions = getStringBooleanMap();
        Role mockRole = Role.builder().id(roleId).name(roleName).build();
        User mockUser = User.builder().id("user-123").build();
        UpdateRolePermissionRequest request = UpdateRolePermissionRequest.builder()
                .roleId(roleId)
                .permissions(permissions)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(userService.getCurrentUser()).thenReturn(mockUser);
        doThrow(new RoleAuditLogException.LoggingFailed("Audit log error"))
                .when(roleAuditLogService).saveRoleAuditLog(any(), anyString(), any());

        assertThrows(RoleAuditLogException.LoggingFailed.class,
                () -> roleService.updateRolePermission(request));

        verify(roleRepository).save(any());
        verify(roleAuditLogService).saveRoleAuditLog(any(), anyString(), any());
    }

    @Test
    void getListOfRole_Success() {
        Role role1 = Role.builder().id("role-1").name("ADMIN").build();
        Role role2 = Role.builder().id("role-2").name("USER").build();
        List<Role> roles = List.of(role1, role2);

        when(roleRepository.findAll()).thenReturn(roles);

        List<GetListOfRoleResponse> result = roleService.getListOfRole();

        assertEquals(2, result.size());
        assertEquals("role-1", result.get(0).getId());
        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("role-2", result.get(1).getId());
        assertEquals("USER", result.get(1).getName());
    }

    @Test
    void getListOfRole_EmptyList() {
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());

        List<GetListOfRoleResponse> result = roleService.getListOfRole();

        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    void getListOfRole_RepositoryError() {
        when(roleRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> roleService.getListOfRole());
    }

    @Test
    void getRoleDetail_Success() {
        String roleId = "role-123";
        String roleName = "ADMIN";
        Map<String, Boolean> permissions = getStringBooleanMap();
        Role mockRole = Role.builder()
                .id(roleId)
                .name(roleName)
                .permissions(permissions)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<RoleAuditLogResponse> auditLogsList = List.of(
                RoleAuditLogResponse.builder()
                        .actionType(ActionType.CREATE)
                        .roleId(roleId)
                        .roleName(roleName)
                        .userId("user-1")
                        .userName("admin")
                        .timestamp(LocalDateTime.now().toString())
                        .build()
        );
        Page<RoleAuditLogResponse> auditLogsPage = new PageImpl<>(
                auditLogsList, PageRequest.of(0, 10), 1);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), eq(0), eq(10)))
                .thenReturn(auditLogsPage);

        GetRoleDetailResponse response = roleService.getRoleDetail(roleId, 0, 10);

        assertEquals(roleId, response.getId());
        assertEquals(roleName, response.getName());
        assertEquals(permissions, response.getPermissions());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertEquals(1, response.getAuditLogs().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getRoleDetail_RoleNotFound() {
        String roleId = "non-existent";
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(RoleException.RoleNotFound.class, () -> roleService.getRoleDetail(roleId, 0, 10));
        verify(roleAuditLogService, never()).getAuditLogsByRoleId(anyString(), anyInt(), anyInt());
    }

    @Test
    void getRoleDetail_NoAuditLogs() {
        String roleId = "role-123";
        String roleName = "USER";
        Map<String, Boolean> permissions = getStringBooleanMap();
        Role mockRole = Role.builder()
                .id(roleId)
                .name(roleName)
                .permissions(permissions)
                .build();

        Page<RoleAuditLogResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), eq(0), eq(10)))
                .thenReturn(emptyPage);

        GetRoleDetailResponse response = roleService.getRoleDetail(roleId, 0, 10);

        assertEquals(roleId, response.getId());
        assertTrue(response.getAuditLogs().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getRoleDetail_Pagination() {
        String roleId = "role-123";
        String roleName = "ADMIN";
        Role mockRole = Role.builder()
                .id(roleId)
                .name(roleName)
                .permissions(getStringBooleanMap())
                .build();

        List<RoleAuditLogResponse> auditLogsList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            auditLogsList.add(RoleAuditLogResponse.builder()
                    .actionType(ActionType.CREATE)
                    .roleId(roleId)
                    .userId("user-" + i)
                    .build());
        }

        Page<RoleAuditLogResponse> secondPage = new PageImpl<>(
                auditLogsList, PageRequest.of(1, 5), 15);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), eq(1), eq(5)))
                .thenReturn(secondPage);

        GetRoleDetailResponse response = roleService.getRoleDetail(roleId, 1, 5);

        assertEquals(5, response.getAuditLogs().size());
        assertEquals(1, response.getCurrentPage());
        assertEquals(3, response.getTotalPages());
        assertEquals(15, response.getTotalElements());
    }

    @Test
    void getRoleDetail_AuditLogServiceError() {
        String roleId = "role-123";
        Role mockRole = Role.builder()
                .id(roleId)
                .name("ADMIN")
                .permissions(getStringBooleanMap())
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(mockRole));
        when(roleAuditLogService.getAuditLogsByRoleId(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Error retrieving audit logs"));

        assertThrows(RuntimeException.class, () -> roleService.getRoleDetail(roleId, 0, 10));
    }
}