package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.security.permission.CustomPermissionEvaluator;
import com.example.upbeat_backend.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RoleControllerTest {
    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

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
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(RoleException.RoleAlreadyExists.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ResponseEntity<String> handleRoleAlreadyExists(RoleException.RoleAlreadyExists ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
        }

        @ExceptionHandler(RoleException.CreationFailed.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleCreationFailed(RoleException.CreationFailed ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.clearContext();

        roleController = spy(new RoleController(roleService));

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_create"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleController).addRole(any(AddRoleRequest.class));

        this.mockMvc = MockMvcBuilders
                .standaloneSetup(roleController)
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

    private static Map<String, Boolean> getStringBooleanMap() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("user_view", true);
        permissions.put("user_create", false);
        permissions.put("user_edit", true);
        permissions.put("user_delete", false);
        permissions.put("role_view", true);
        permissions.put("role_create", true);
        permissions.put("role_edit", true);
        permissions.put("role_delete", true);
        permissions.put("content_view", true);
        permissions.put("content_create", true);
        permissions.put("content_edit", true);
        permissions.put("content_delete", true);
        permissions.put("system_settings", true);
        return permissions;
    }

    @Test
    void addRole_Success() throws Exception {
        String roleName = "ADMIN";
        Map<String, Boolean> permissions = getStringBooleanMap();
        AddRoleRequest request = new AddRoleRequest(roleName, permissions);

        when(roleService.addRole(any(AddRoleRequest.class))).thenReturn("Role created successfully.");
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().id("user123").username("admin").build(),
                null,
                List.of(new SimpleGrantedAuthority("role_create")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        mockMvc.perform(post("/role/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role created successfully."));

        verify(roleService).addRole(any(AddRoleRequest.class));
    }

    @Test
    void addRole_ValidationFailure() throws Exception {
        AddRoleRequest invalidRequest = new AddRoleRequest("", Collections.emptyMap());

        mockMvc.perform(post("/role/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).addRole(any());
    }

    @Test
    void addRole_RoleAlreadyExists() throws Exception {
        AddRoleRequest request = new AddRoleRequest("ADMIN", getStringBooleanMap());

        when(roleService.addRole(any()))
                .thenThrow(new RoleException.RoleAlreadyExists("ADMIN"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_create")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/role/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void addRole_WithoutPermission() throws Exception {
        AddRoleRequest request = new AddRoleRequest("USER", getStringBooleanMap());
        UserPrincipal userPrincipal = UserPrincipal.builder().id("user123").username("user").build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/role/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roleService, never()).addRole(any());
    }

    @Test
    void addRole_ServerError() throws Exception {
        AddRoleRequest request = new AddRoleRequest("ERROR_ROLE", getStringBooleanMap());

        when(roleService.addRole(any()))
                .thenThrow(new RoleException.CreationFailed("Error creating role"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_create")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/role/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
