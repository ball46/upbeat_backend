package com.example.upbeat_backend.dto.request.role;

import com.example.upbeat_backend.validation.annotation.permission.ValidPermissions;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionRequest {
    @NotBlank
    private String roleId;

    @NotEmpty(message = "Permissions are required")
    @ValidPermissions
    private Map<String, Boolean> permissions;
}
