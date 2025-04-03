package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRoleNameRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRolePermissionRequest;
import com.example.upbeat_backend.dto.response.role.GetListOfRoleResponse;
import com.example.upbeat_backend.dto.response.role.GetRoleDetailResponse;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Value("${app.default_role_name}")
    private String defaultRoleName;

    public String addRole(@NotNull AddRoleRequest ar) {
        if (roleRepository.existsByName(ar.getName())) {
            throw new RoleException.RoleAlreadyExists(ar.getName());
        }
        try {
            Role role = Role.builder()
                    .name(ar.getName())
                    .permissions(ar.getPermissions())
                    .build();
            roleRepository.save(role);
            return "Role created successfully.";
        } catch (Exception e) {
            throw new RoleException.CreationFailed("Failed to create role: " + e.getMessage());
        }
    }

    public String deleteRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleException.RoleNotFound(roleId));
        if (role.getName().equals(defaultRoleName)) {
            throw new RoleException.ForbiddenOperation("Cannot delete default role '" + defaultRoleName + "'");
        }

        int userCount = userRepository.countByRoleId(roleId);
        if (userCount > 0) {
            throw new RoleException.DeletionConflict("Cannot delete role that is assigned to " + userCount + " users");
        }

        try {
            roleRepository.delete(role);
            return "Role deleted successfully.";
        } catch (Exception e) {
            throw new RoleException.DeletionFailed("Failed to delete role: " + e.getMessage());
        }
    }

    public String updateRoleName(@NotNull UpdateRoleNameRequest un) {
        Role role = roleRepository.findById(un.getRoleId())
                .orElseThrow(() -> new RoleException.RoleNotFound(un.getRoleId()));
        if (role.getName().equals(defaultRoleName)) {
            throw new RoleException.ForbiddenOperation("Cannot update default role '" + defaultRoleName + "'");
        }

        if (roleRepository.existsByName(un.getName())) {
            throw new RoleException.RoleAlreadyExists(un.getName());
        }

        try {
            role.setName(un.getName());
            roleRepository.save(role);
            return "Role name updated successfully.";
        } catch (DataIntegrityViolationException e) {
            throw new RoleException.UpdateFailed("Failed to update role name: " + e.getMessage());
        }
    }

    public String updateRolePermission(@NotNull UpdateRolePermissionRequest up) {
        Role role = roleRepository.findById(up.getRoleId())
                .orElseThrow(() -> new RoleException.RoleNotFound(up.getRoleId()));
        if (role.getName().equals(defaultRoleName)) {
            throw new RoleException.ForbiddenOperation("Cannot update permissions of default role '" + defaultRoleName + "'");
        }

        try {
            role.setPermissions(up.getPermissions());
            roleRepository.save(role);
            return "Role permissions updated successfully.";
        } catch (DataIntegrityViolationException e) {
            throw new RoleException.UpdateFailed("Failed to update role permissions: " + e.getMessage());
        }
    }

    public List<GetListOfRoleResponse> getListOfRole() {
        return roleRepository.findAll().stream()
                .map(role -> GetListOfRoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .build())
                .toList();
    }

    public GetRoleDetailResponse getRoleDetail(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleException.RoleNotFound(roleId));

        return GetRoleDetailResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .permissions(role.getPermissions())
                .createdAt(String.valueOf(role.getCreatedAt()))
                .updatedAt(String.valueOf(role.getUpdatedAt()))
                .build();
    }
}
