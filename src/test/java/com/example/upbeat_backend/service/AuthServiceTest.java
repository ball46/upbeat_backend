package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.model.RefreshToken;
import com.example.upbeat_backend.model.Role;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.AccountStatus;
import com.example.upbeat_backend.repository.RoleRepository;
import com.example.upbeat_backend.repository.UserRepository;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void signUp_Success() {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .roleId("1")
                .build();

        Role role = Role.builder()
                .id("1")
                .name("USER")
                .build();

        User savedUser = User.builder()
                .id("1")
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .role(role)
                .build();

        when(roleRepository.findById("1")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(savedUser);

        String result = authService.signUp(request);
        assertEquals("User created successfully.", result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signUp_WithDefaultRoleName() {
        ReflectionTestUtils.setField(authService, "defaultRoleName", "USER");

        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .roleId(null) // No role ID provided
                .build();

        Role defaultRole = Role.builder()
                .id("default-role-id")
                .name("USER")
                .build();

        User user = User.builder()
                .id("1")
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .role(defaultRole)
                .build();

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(user);

        String result = authService.signUp(request);
        assertEquals("User created successfully.", result);
        verify(roleRepository).findByName("USER");
    }

    @Test
    void signUp_DuplicateEmail() {
        ReflectionTestUtils.setField(authService, "defaultRoleName", "USER");

        Role defaultRole = Role.builder().id("default-id").name("USER").build();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));

        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThrows(AuthException.DuplicateEmail.class, () -> authService.signUp(request));
    }

    @Test
    void signUp_GenericException() {
        ReflectionTestUtils.setField(authService, "defaultRoleName", "USER");

        Role defaultRole = Role.builder().id("default-id").name("USER").build();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));

        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenThrow(new RuntimeException("Database connection error"));

        assertThrows(RuntimeException.class, () -> authService.signUp(request));
    }

    @Test
    void login_SuccessWithUsername() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testUser")
                .password("Test123*")
                .build();

        User user = User.builder()
                .id(String.valueOf(1L))
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(String.valueOf(2L))
                .token("refresh-token")
                .user(user)
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(anyString())).thenReturn(refreshToken);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(String.valueOf(1L), response.getId());
        assertEquals("testUser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void login_SuccessWithEmail() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("test@example.com")
                .password("password")
                .build();

        User user = User.builder()
                .id("1")
                .username("testUser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id("2")
                .token("refresh-token")
                .user(user)
                .build();

        when(userRepository.findByUsernameOrEmail(eq("test@example.com"), eq("test@example.com")))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("password"), eq("encodedPassword"))).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(eq("1"))).thenReturn(refreshToken);

        LoginResponse response = authService.login(request);

        assertEquals("1", response.getId());
        assertEquals("testUser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void login_InvalidCredentials_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testUser1");
        request.setPassword("Test123@");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(AuthException.InvalidCredentials.class, () -> authService.login(request));
    }

    @Test
    void login_InvalidCredentials_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testUser");
        request.setPassword("Test123@");

        User user = User.builder()
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(AuthException.InvalidCredentials.class, () -> authService.login(request));
    }

    @Test
    void login_AccountSuspended() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("suspended@example.com");
        request.setPassword("password");

        User user = User.builder()
                .id("1")
                .username("suspendedUser")
                .email("suspended@example.com")
                .password("encodedPassword")
                .status(AccountStatus.SUSPENDED)
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(user));

        assertThrows(AuthException.AccountSuspended.class, () -> authService.login(request));
    }

    @Test
    void login_AccountDeleted() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("deleted@example.com");
        request.setPassword("password");

        User user = User.builder()
                .id("1")
                .username("deletedUser")
                .email("deleted@example.com")
                .password("encodedPassword")
                .status(AccountStatus.DELETED)
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(user));

        assertThrows(AuthException.AccountDeleted.class, () -> authService.login(request));
    }
}
