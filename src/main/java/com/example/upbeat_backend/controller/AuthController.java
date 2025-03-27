package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignupRequest sr) {
        String data = authService.signUp(sr);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest lr) {
        LoginResponse data = authService.login(lr);
        return ResponseEntity.ok(data);
    }
}
