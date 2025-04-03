package com.example.upbeat_backend.dto.response.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRoleDetailResponse {
    private String id;
    private String name;
    private Map<String, Boolean> permissions;
    private String createdAt;
    private String updatedAt;
}
