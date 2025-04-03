package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.model.RefreshToken;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.model.enums.AccountStatus;
import com.example.upbeat_backend.model.enums.LoginStatus;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;

    @Value("${app.default_role_name}")
    private String defaultRoleName;

    public String signUp(SignupRequest sr) {
        try {
            Role role;
            if (sr.getRoleId() != null) {
                role = roleRepository.findById(sr.getRoleId())
                        .orElseThrow(() -> new RoleException.RoleNotFound(sr.getRoleId()));
            } else {
                role = roleRepository.findByName(defaultRoleName)
                        .orElseThrow(() -> new RoleException.RoleNotFound(defaultRoleName));
            }

            User user = User.builder()
                    .username(sr.getUsername())
                    .password(passwordEncoder.encode(sr.getPassword()))
                    .email(sr.getEmail())
                    .role(role)
                    .build();
            userRepository.save(user);
            return "User created successfully.";
        } catch (DataIntegrityViolationException e) {
            throw new AuthException.DuplicateEmail(sr.getEmail());
        } catch (Exception e) {
            if (e instanceof RoleException.RoleNotFound) throw e;
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    public LoginResponse login(LoginRequest lr) {
        return login(lr, "0.0.0.0", "Unknown");
    }

    public LoginResponse login(LoginRequest lr, String ipAddress, String userAgent) {
        try {
            User user = findAndValidateUser(lr.getUsernameOrEmail(), ipAddress, userAgent);

            validatePassword(user, lr.getPassword(), ipAddress, userAgent);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            loginHistoryService.recordLoginAttempt(user, ipAddress, userAgent, LoginStatus.SUCCESS, null);

            return buildLoginResponse(user, refreshToken);
        } catch (Exception e) {
            switch (e) {
                case AuthException.InvalidCredentials ignored -> throw e;
                case AuthException.AccountDeleted ignored -> throw e;
                case AuthException.AccountSuspended ignored -> throw e;
                default -> {
                    handleUnexpectedError(lr.getUsernameOrEmail(), ipAddress, userAgent, e);
                    throw new RuntimeException("Login failed: " + e.getMessage());
                }
            }
        }
    }

    private @NotNull User findAndValidateUser(String usernameOrEmail, String ipAddress, String userAgent) {
        User user = userRepository.findByUsernameOrEmail(
                usernameOrEmail, usernameOrEmail
        ).orElseThrow(AuthException.InvalidCredentials::new);

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            loginHistoryService.recordLoginAttempt(user, ipAddress, userAgent,
                    LoginStatus.SUSPENDED, "This account has been suspended");
            throw new AuthException.AccountSuspended();
        } else if (user.getStatus() == AccountStatus.DELETED) {
            loginHistoryService.recordLoginAttempt(user, ipAddress, userAgent,
                    LoginStatus.DELETED, "This account has been deleted");
            throw new AuthException.AccountDeleted();
        }

        return user;
    }

    private void validatePassword(@NotNull User user, String password, String ipAddress, String userAgent) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginHistoryService.recordLoginAttempt(user, ipAddress, userAgent,
                    LoginStatus.INVALID_CREDENTIALS, "Username or password is incorrect");
            throw new AuthException.InvalidCredentials();
        }
    }

    private LoginResponse buildLoginResponse(@NotNull User user, @NotNull RefreshToken refreshToken) {
        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(jwtTokenProvider.generateToken(user))
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private void handleUnexpectedError(String usernameOrEmail, String ipAddress, String userAgent, Exception e) {
        try {
            userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                    .ifPresent(user -> loginHistoryService.recordLoginAttempt(user,
                            ipAddress,
                            userAgent,
                            LoginStatus.UNKNOWN_ERROR,
                            e.getMessage()));
        } catch (Exception ignored) {}
    }
}
