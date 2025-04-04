package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.exception.role.RoleAuditLogException;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.exception.user.UserException;
import com.example.upbeat_backend.model.enums.ActionType;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.security.permission.CustomPermissionEvaluator;
import com.example.upbeat_backend.service.RoleAuditLogService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class RoleAuditLogControllerTest {
    private MockMvc mockMvc;

    @Mock
    private RoleAuditLogService roleAuditLogService;

    @InjectMocks
    private RoleAuditLogController roleAuditLogController;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public static PermissionEvaluator permissionEvaluator() {
            return new CustomPermissionEvaluator();
        }

        @Bean
        public static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(permissionEvaluator());
            return expressionHandler;
        }
    }

    @RestControllerAdvice
    public static class TestControllerAdvice {
        @ExceptionHandler(RoleException.RoleNotFound.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleRoleNotFound(RoleException.RoleNotFound ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

        @ExceptionHandler(UserException.NotFound.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleUserNotFound(UserException.NotFound ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

        @ExceptionHandler(RoleAuditLogException.LoggingFailed.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleLoggingFailed(RoleAuditLogException.LoggingFailed ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleGenericException(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.clearContext();

        roleAuditLogController = spy(new RoleAuditLogController(roleAuditLogService));

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_view"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleAuditLogController).getAuditLogsByRoleId(anyString(), anyInt(), anyInt());

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_view"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleAuditLogController).getAuditLogsByUserId(anyString(), anyInt(), anyInt());

        this.mockMvc = MockMvcBuilders
                .standaloneSetup(roleAuditLogController)
                .setControllerAdvice(new TestControllerAdvice())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(@NotNull MethodParameter parameter) {
                        return parameter.getParameterType().equals(UserPrincipal.class) &&
                                parameter.hasParameterAnnotation(CurrentUser.class);
                    }

                    @Override
                    public Object resolveArgument(@NotNull MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  @NotNull NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null) {
                            return auth.getPrincipal();
                        }
                        return null;
                    }
                })
                .build();
    }

    @Test
    void getAuditLogsByRoleId_Success() throws Exception {
        String roleId = "role-123";
        int page = 0;
        int size = 10;

        List<RoleAuditLogResponse> auditLogs = List.of(
                RoleAuditLogResponse.builder()
                        .actionType(ActionType.CREATE)
                        .roleId(roleId)
                        .roleName("ADMIN")
                        .userId("user-1")
                        .userName("admin")
                        .timestamp(LocalDateTime.now().toString())
                        .build(),
                RoleAuditLogResponse.builder()
                        .actionType(ActionType.UPDATE_NAME)
                        .roleId(roleId)
                        .roleName("ADMIN")
                        .userId("user-2")
                        .userName("moderator")
                        .timestamp(LocalDateTime.now().minusDays(1).toString())
                        .build()
        );

        Page<RoleAuditLogResponse> auditLogPage = new PageImpl<>(
                auditLogs, PageRequest.of(page, size), 2);

        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), eq(page), eq(size)))
                .thenReturn(auditLogPage);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().id("user-123").username("admin").build(),
                null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log//by-role-id/{roleId}", roleId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].actionType").value("CREATE"))
                .andExpect(jsonPath("$.content[0].roleId").value(roleId))
                .andExpect(jsonPath("$.content[1].actionType").value("UPDATE_NAME"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(roleAuditLogService).getAuditLogsByRoleId(roleId, page, size);
    }

    @Test
    void getAuditLogsByRoleId_EmptyResult() throws Exception {
        String roleId = "role-123";
        int page = 0;
        int size = 10;

        Page<RoleAuditLogResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(page, size), 0);

        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), eq(page), eq(size)))
                .thenReturn(emptyPage);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().id("user-123").username("admin").build(),
                null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log//by-role-id/{roleId}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAuditLogsByRoleId_WithPagination() throws Exception {
        String roleId = "role-123";
        int page = 2;
        int size = 5;

        Page<RoleAuditLogResponse> secondPage = new PageImpl<>(
                Collections.singletonList(RoleAuditLogResponse.builder().build()),
                PageRequest.of(page, size),
                15); // 15 total elements

        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), eq(page), eq(size)))
                .thenReturn(secondPage);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log//by-role-id/{roleId}", roleId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getAuditLogsByRoleId_WithoutPermission() throws Exception {
        String roleId = "role-123";

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().id("user-123").username("user").build(),
                null,
                Collections.emptyList()); // No permissions
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log//by-role-id/{roleId}", roleId))
                .andExpect(status().isForbidden());

        verify(roleAuditLogService, never()).getAuditLogsByRoleId(anyString(), anyInt(), anyInt());
    }

    @Test
    void getAuditLogsByRoleId_RoleNotFound() throws Exception {
        String roleId = "non-existent";

        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), anyInt(), anyInt()))
                .thenThrow(new RoleException.RoleNotFound(roleId));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log//by-role-id/{roleId}", roleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAuditLogsByRoleId_ServerError() throws Exception {
        String roleId = "role-error";

        when(roleAuditLogService.getAuditLogsByRoleId(eq(roleId), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log//by-role-id/{roleId}", roleId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAuditLogsByUserId_Success() throws Exception {
        String userId = "user-123";
        int page = 0;
        int size = 10;

        List<RoleAuditLogResponse> logs = List.of(
                RoleAuditLogResponse.builder()
                        .actionType(ActionType.CREATE)
                        .roleId("role-1")
                        .roleName("ADMIN")
                        .userId(userId)
                        .userName("admin")
                        .timestamp(LocalDateTime.now().toString())
                        .build(),
                RoleAuditLogResponse.builder()
                        .actionType(ActionType.DELETE)
                        .roleId("role-2")
                        .roleName("MODERATOR")
                        .userId(userId)
                        .userName("admin")
                        .timestamp(LocalDateTime.now().minusDays(1).toString())
                        .build()
        );

        Page<RoleAuditLogResponse> logsPage = new PageImpl<>(logs, PageRequest.of(page, size), 2);

        when(roleAuditLogService.getAuditLogsByUserId(eq(userId), eq(page), eq(size)))
                .thenReturn(logsPage);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log/by-user-id/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(roleAuditLogService).getAuditLogsByUserId(userId, page, size);
    }

    @Test
    void getAuditLogsByUserId_EmptyResult() throws Exception {
        String userId = "user-123";
        int page = 0;
        int size = 10;

        Page<RoleAuditLogResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(page, size), 0);

        when(roleAuditLogService.getAuditLogsByUserId(eq(userId), eq(page), eq(size)))
                .thenReturn(emptyPage);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log/by-user-id/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(roleAuditLogService).getAuditLogsByUserId(userId, page, size);
    }

    @Test
    void getAuditLogsByUserId_WithPagination() throws Exception {
        String userId = "user-123";
        int page = 2;
        int size = 5;

        Page<RoleAuditLogResponse> secondPage = new PageImpl<>(
                Collections.singletonList(RoleAuditLogResponse.builder().build()),
                PageRequest.of(page, size),
                15); // 15 total elements

        when(roleAuditLogService.getAuditLogsByUserId(eq(userId), eq(page), eq(size)))
                .thenReturn(secondPage);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log/by-user-id/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(roleAuditLogService).getAuditLogsByUserId(userId, page, size);
    }

    @Test
    void getAuditLogsByUserId_WithoutPermission() throws Exception {
        String userId = "user-123";

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                Collections.emptyList()); // No permissions
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log/by-user-id/{userId}", userId))
                .andExpect(status().isForbidden());

        verify(roleAuditLogService, never()).getAuditLogsByUserId(anyString(), anyInt(), anyInt());
    }

    @Test
    void getAuditLogsByUserId_UserNotFound() throws Exception {
        String userId = "non-existent";

        when(roleAuditLogService.getAuditLogsByUserId(eq(userId), anyInt(), anyInt()))
                .thenThrow(new UserException.NotFound(userId));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log/by-user-id/{userId}", userId))
                .andExpect(status().isNotFound());

        verify(roleAuditLogService).getAuditLogsByUserId(eq(userId), anyInt(), anyInt());
    }

    @Test
    void getAuditLogsByUserId_ServerError() throws Exception {
        String userId = "user-error";

        when(roleAuditLogService.getAuditLogsByUserId(eq(userId), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role-audit-log/by-user-id/{userId}", userId))
                .andExpect(status().isInternalServerError());

        verify(roleAuditLogService).getAuditLogsByUserId(eq(userId), anyInt(), anyInt());
    }
    
}
