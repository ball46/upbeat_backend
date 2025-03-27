package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/role")
public class RoleController {
    private final RoleService roleService;

    @PostMapping("/add")
    public ResponseEntity<String> addRole(@Valid @RequestBody AddRoleRequest ar) {
        String data = roleService.addRole(ar);
        return ResponseEntity.ok(data);
    }
}
