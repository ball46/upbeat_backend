package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.model.enums.AccountStatus;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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
        User user = userRepository.findByUsernameOrEmail(
                lr.getUsernameOrEmail(),
                lr.getUsernameOrEmail()
        ).orElseThrow(AuthException.InvalidCredentials::new);

        if (user.getStatus() == AccountStatus.SUSPENDED) throw new AuthException.AccountSuspended();
        else if (user.getStatus() == AccountStatus.DELETED) throw new AuthException.AccountDeleted();

        if (!passwordEncoder.matches(lr.getPassword(), user.getPassword())) {
            throw new AuthException.InvalidCredentials();
        }

        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(jwtTokenProvider.generateToken(user))
                .build();
    }
}
