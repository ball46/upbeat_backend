package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.auth.LoginRequest;
import com.example.upbeat_backend.dto.request.auth.SignupRequest;
import com.example.upbeat_backend.dto.response.auth.LoginResponse;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
@Import(AuthService.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @Test
    void signUp_Success() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .roleId(null)
                .build();

        when(authService.signUp(any())).thenReturn("User created successfully.");

        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully."));
    }

    @Test
    void signUp_MissingRequiredFields() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("")
                .password("")
                .email("")
                .build();

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_InvalidUsername_TooShort() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("ab")
                .password("Test123*")
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_InvalidUsername_TooLong() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("thisusernameiswaytoolongandshouldfail")
                .password("Test123*")
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_InvalidPassword_MissingRequirements() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("password")
                .email("test@example.com")
                .build();

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_InvalidEmail_Format() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_DuplicateEmail() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(authService.signUp(any())).thenThrow(new AuthException.DuplicateEmail("test@example.com"));

        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void signUp_ServerError() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("testUser")
                .password("Test123*")
                .email("test@example.com")
                .build();

        when(authService.signUp(any())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void login_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testUser");
        request.setPassword("Test123@");

        LoginResponse response = LoginResponse.builder()
                .id(String.valueOf(1L))
                .username("testUser")
                .email("test@example.com")
                .token("jwt-token")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(String.valueOf(1L)))
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testUser");
        request.setPassword("Test123@");

        when(authService.login(any())).thenThrow(new AuthException.InvalidCredentials());

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_MissingRequiredFields() throws Exception {
        LoginRequest request = new LoginRequest();
        // ไม่ได้ตั้งค่า usernameOrEmail และ password

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ServerError() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testUser");
        request.setPassword("Test123@");

        when(authService.login(any())).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
