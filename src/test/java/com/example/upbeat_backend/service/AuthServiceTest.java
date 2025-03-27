package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.repository.UserRepository;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void signUp_Success() {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        User savedUser = User.builder()
                .id("1")
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .build();

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(savedUser);

        String result = authService.signUp(request);
        assertEquals("User created successfully.", result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signUp_DuplicateEmail() {
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
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testUser");
        request.setPassword("Test123*");

        User user = User.builder()
                .id(String.valueOf(1L))
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(String.valueOf(1L), response.getId());
        assertEquals("testUser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void login_SuccessWithEmail() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password");

        User user = User.builder()
                .id("1")
                .username("testUser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        when(userRepository.findByUsernameOrEmail(eq("test@example.com"), eq("test@example.com")))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("password"), eq("encodedPassword"))).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("1", response.getId());
        assertEquals("testUser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
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
}
