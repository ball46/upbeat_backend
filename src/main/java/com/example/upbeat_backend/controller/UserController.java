package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.user.ChangePassword;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePassword cp, @CurrentUser UserPrincipal currentUser) {
        String userId = currentUser.getId();
        String response = userService.changePassword(userId, cp);
        return ResponseEntity.ok(response);
    }
}
