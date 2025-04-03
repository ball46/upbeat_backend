package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRoleNameRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRolePermissionRequest;
import com.example.upbeat_backend.dto.response.role.GetListOfRoleResponse;
import com.example.upbeat_backend.dto.response.role.GetRoleDetailResponse;
import com.example.upbeat_backend.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/role")
public class RoleController {
    private final RoleService roleService;

    @PostMapping("/add")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_CREATE)")
    public ResponseEntity<String> addRole(@Valid @RequestBody AddRoleRequest ar) {
        String data = roleService.addRole(ar);
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_DELETE)")
    public ResponseEntity<String> deleteRole(@RequestParam String roleId) {
        String data = roleService.deleteRole(roleId);
        return ResponseEntity.ok(data);
    }

    @PatchMapping("/update-name")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_EDIT)")
    public ResponseEntity<String> updateRole(@RequestBody UpdateRoleNameRequest un) {
        String data = roleService.updateRoleName(un);
        return ResponseEntity.ok(data);
    }

    @PatchMapping("/update-permission")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_EDIT)")
    public ResponseEntity<String> updateRolePermission(@RequestBody UpdateRolePermissionRequest up) {
        String data = roleService.updateRolePermission(up);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/get-list")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_VIEW)")
    public ResponseEntity<List<GetListOfRoleResponse>> getListOfRole() {
        List<GetListOfRoleResponse> data = roleService.getListOfRole();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/get-detail")
    @PreAuthorize("hasPermission(null, T(com.example.upbeat_backend.security.permission.PermissionConstants).ROLE_VIEW)")
    public ResponseEntity<GetRoleDetailResponse> getRoleDetail(
            @RequestParam String roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        GetRoleDetailResponse data = roleService.getRoleDetail(roleId, page, size);
        return ResponseEntity.ok(data);
    }
}
