package com.example.upbeat_backend.dto.response.role;

import com.example.upbeat_backend.model.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuditLogResponse {
    private ActionType actionType;
    private String userId;
    private String userName;
    private String roleId;
    private String roleName;
    private String timestamp;
}
