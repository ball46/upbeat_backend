package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @RestControllerAdvice
    public static class TestControllerAdvice {
        @ExceptionHandler(AuthException.InvalidCredentials.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ResponseEntity<String> handleInvalidCredentials(AuthException.InvalidCredentials ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(AuthException.DuplicateEmail.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ResponseEntity<String> handleDuplicateEmail(AuthException.DuplicateEmail ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
        }

        @ExceptionHandler(RuntimeException.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(AuthException.AccountSuspended.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<String> handleAccountSuspended(AuthException.AccountSuspended ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(AuthException.AccountDeleted.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<String> handleAccountDeleted(AuthException.AccountDeleted ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    @BeforeEach
    public void setup() {
        AuthController authController = new AuthController(authService);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void signUp_Success() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(authService.signUp(any())).thenReturn("User created successfully.");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully."));
    }

    @Test
    void signUp_DuplicateEmail() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("existing@example.com")
                .build();

        doThrow(new AuthException.DuplicateEmail("existing@example.com"))
                .when(authService).signUp(any());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void signUp_InvalidRole() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .roleId("invalid-role-id")
                .build();

        when(authService.signUp(any())).thenThrow(new RuntimeException("Role not found"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void signUp_InvalidRequest() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("te") // Too short
                .password("weak") // Not matching pattern
                .email("invalid-email") // Invalid email format
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_NullFields() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username(null)
                .password("Test123*")
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_BoundaryValues() throws Exception {
        // Minimum valid username length
        SignupRequest request = SignupRequest.builder()
                .username("abc") // Exactly 3 chars (minimum)
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(authService.signUp(any())).thenReturn("User created successfully.");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void signUp_DefaultRoleFallback() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .roleId(null) // No role specified, should use default
                .build();

        when(authService.signUp(any())).thenReturn("User created successfully.");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void signUp_GeneralException() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(authService.signUp(any())).thenThrow(new RuntimeException("Failed to create user: Database connection error"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testUser")
                .password("Test123*")
                .build();

        LoginResponse response = LoginResponse.builder()
                .id("user-id")
                .username("testUser")
                .email("test@example.com")
                .token("jwt-token")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-id"))
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("WrongPass123*")
                .build();

        when(authService.login(any())).thenThrow(new AuthException.InvalidCredentials());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_InvalidRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .password("Test123*")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("test@example.com") // Using email instead of username
                .password("Test123*")
                .build();

        LoginResponse response = LoginResponse.builder()
                .id("user-id")
                .username("testUser")
                .email("test@example.com")
                .token("jwt-token")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_EmptyBody() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty JSON body
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_AccountSuspended() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("suspended@example.com")
                .password("Test123*")
                .build();

        when(authService.login(any())).thenThrow(new AuthException.AccountSuspended());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_AccountDeleted() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("deleted@example.com")
                .password("Test123*")
                .build();

        when(authService.login(any())).thenThrow(new AuthException.AccountDeleted());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}