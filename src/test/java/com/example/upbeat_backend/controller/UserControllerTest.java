package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.user.ChangePassword;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.exception.user.UserException;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @RestControllerAdvice
    public static class TestControllerAdvice {
        @ExceptionHandler(UserException.NotFound.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleUserNotFound(UserException.NotFound ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(AuthException.InvalidPassword.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ResponseEntity<String> handleInvalidPassword(AuthException.InvalidPassword ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new TestControllerAdvice())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(@NotNull MethodParameter parameter) {
                        return parameter.getParameterType().equals(UserPrincipal.class) &&
                               parameter.hasParameterAnnotation(CurrentUser.class);
                    }

                    @Override
                    public Object resolveArgument(@NotNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  @NotNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    }
                })
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void changePassword_Success() throws Exception {
        String userId = "testUserId";
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(userId)
                .username("testUser")
                .build();

        ChangePassword cp = ChangePassword.builder()
                .oldPassword("OldPass123*")
                .newPassword("NewPass123*")
                .build();

        when(userService.changePassword(eq(userId), any(ChangePassword.class)))
                .thenReturn("Password changed successfully");

        mockMvc.perform(post("/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cp))
                .with(request -> {
                    request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, Collections.emptyList()));
                    SecurityContextHolder.getContext().setAuthentication(
                            (Authentication) request.getUserPrincipal());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));
    }

    @Test
    void changePassword_InvalidOldPassword() throws Exception {
        String userId = "testUserId";
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(userId)
                .username("testUser")
                .build();

        ChangePassword cp = ChangePassword.builder()
                .oldPassword("WrongPass123*")
                .newPassword("NewPass123*")
                .build();

        doThrow(new AuthException.InvalidPassword())
                .when(userService).changePassword(eq(userId), any(ChangePassword.class));

        mockMvc.perform(post("/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cp))
                .with(user(userPrincipal)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_InvalidRequest_SamePasswords() throws Exception {
        // Arrange
        String userId = "testUserId";
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(userId)
                .username("testUser")
                .build();

        ChangePassword cp = ChangePassword.builder()
                .oldPassword("SamePass123*")
                .newPassword("SamePass123*")
                .build();

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new TestControllerAdvice())
                .setValidator(new LocalValidatorFactoryBean() {
                    @Override
                    public void validate(@NotNull Object target, @NotNull Errors errors) {
                        if (target instanceof ChangePassword cp) {
                            if (cp.getOldPassword() != null && cp.getOldPassword().equals(cp.getNewPassword())) {
                                errors.reject("passwords.same", "New password must be different from old password");
                            }
                        }
                    }
                })
                .build();

        mockMvc.perform(post("/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cp))
                .with(request -> {
                    request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, Collections.emptyList()));
                    SecurityContextHolder.getContext().setAuthentication(
                            (Authentication) request.getUserPrincipal());
                    return request;
                }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_InvalidRequest_MissingFields() throws Exception {
        String userId = "testUserId";
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(userId)
                .build();

        ChangePassword cp = ChangePassword.builder()
                .newPassword("NewPass123*")
                .build();

        mockMvc.perform(post("/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cp))
                .requestAttr("currentUser", userPrincipal))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_InvalidRequest_WeakPassword() throws Exception {
        String userId = "testUserId";
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(userId)
                .build();

        ChangePassword cp = ChangePassword.builder()
                .oldPassword("OldPass123*")
                .newPassword("weak")
                .build();

        mockMvc.perform(post("/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cp))
                .requestAttr("currentUser", userPrincipal))
                .andExpect(status().isBadRequest());
    }
}