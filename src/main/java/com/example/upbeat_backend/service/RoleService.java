package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public String addRole(AddRoleRequest ar) {
        try {
            Role role = Role.builder()
                    .name(ar.getName())
                    .permissions(ar.getPermissions())
                    .build();
            roleRepository.save(role);
            return "Role created successfully.";
        } catch (Exception e) {
            throw new RuntimeException("Failed to create role: " + e.getMessage());
        }
    }
}
