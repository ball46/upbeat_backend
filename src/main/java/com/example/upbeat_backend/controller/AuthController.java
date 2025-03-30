package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.dto.response.auth.TokenRefreshResponse;
import com.example.upbeat_backend.model.RefreshToken;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import com.example.upbeat_backend.service.AuthService;
import com.example.upbeat_backend.service.RefreshTokenService;
import com.example.upbeat_backend.util.DataHeader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignupRequest sr) {
        String data = authService.signUp(sr);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest lr, HttpServletRequest request) {
        String ipAddress = DataHeader.getIpAddress(request);
        String userAgent = DataHeader.getUserAgent(request);
        LoginResponse data = authService.login(lr, ipAddress, userAgent);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestHeader(value = "Refresh-Token" ) String rt) {
        RefreshToken refreshToken = refreshTokenService.findByToken(rt);
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateToken(user);

        TokenRefreshResponse tokenRefreshResponse = TokenRefreshResponse.builder()
                .AccessToken(newAccessToken)
                .build();

        return ResponseEntity.ok(tokenRefreshResponse);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(@CurrentUser UserPrincipal currentUser) {
        refreshTokenService.deleteByUserId(currentUser.getId());
        return ResponseEntity.ok("Logout successful");
    }
}
