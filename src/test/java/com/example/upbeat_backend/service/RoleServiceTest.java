package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.repository.RoleRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Captor
    private ArgumentCaptor<Role> roleCaptor;

    @Test
    void addRole_Success() {
        // Arrange
        String roleName = "ADMIN";
        Map<String, Boolean> permissions = getStringBooleanMap();

        AddRoleRequest request = AddRoleRequest.builder()
                .name(roleName)
                .permissions(permissions)
                .build();
        
        when(roleRepository.existsByName(roleName)).thenReturn(false);

        String result = roleService.addRole(request);

        assertEquals("Role created successfully.", result);
        verify(roleRepository).save(roleCaptor.capture());
        Role savedRole = roleCaptor.getValue();
        assertEquals(roleName, savedRole.getName());
        assertEquals(permissions, savedRole.getPermissions());
    }

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
}