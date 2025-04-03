package com.example.upbeat_backend.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleNameRequest {
    @NotBlank
    private String roleId;

    @NotBlank
    private String name;
}
