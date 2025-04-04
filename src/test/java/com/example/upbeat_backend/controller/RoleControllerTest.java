package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.request.role.AddRoleRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRoleNameRequest;
import com.example.upbeat_backend.dto.request.role.UpdateRolePermissionRequest;
import com.example.upbeat_backend.dto.response.role.GetListOfRoleResponse;
import com.example.upbeat_backend.dto.response.role.GetRoleDetailResponse;
import com.example.upbeat_backend.dto.response.role.RoleAuditLogResponse;
import com.example.upbeat_backend.exception.role.RoleException;
import com.example.upbeat_backend.model.enums.ActionType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

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

        @ExceptionHandler(RoleException.ForbiddenOperation.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<String> handleForbiddenOperation(RoleException.ForbiddenOperation ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }

        @ExceptionHandler(RoleException.DeletionConflict.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ResponseEntity<String> handleDeletionConflict(RoleException.DeletionConflict ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }

        @ExceptionHandler(RoleException.DeletionFailed.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleDeletionFailed(RoleException.DeletionFailed ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

        @ExceptionHandler(RoleException.UpdateFailed.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleUpdateFailed(RoleException.UpdateFailed ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ResponseEntity<String> handleGenericException(Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
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

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_delete"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleController).deleteRole(anyString());

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_edit"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleController).updateRole(any(UpdateRoleNameRequest.class));

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_edit"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleController).updateRolePermission(any(UpdateRolePermissionRequest.class));

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_view"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleController).getListOfRole();

        lenient().doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("role_view"))) {
                throw new AccessDeniedException("Access denied");
            }
            return invocation.callRealMethod();
        }).when(roleController).getRoleDetail(anyString(), anyInt(), anyInt());

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

    private static @NotNull Map<String, Boolean> getStringBooleanMap() {
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

    @Test
    void deleteRole_Success() throws Exception {
        String roleId = "role-123";
        when(roleService.deleteRole(roleId)).thenReturn("Role deleted successfully.");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_delete")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/role/delete")
                .param("roleId", roleId))
                .andExpect(status().isOk())
                .andExpect(content().string("Role deleted successfully."));

        verify(roleService).deleteRole(roleId);
    }

    @Test
    void deleteRole_WithoutPermission() throws Exception {
        String roleId = "role-123";

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/role/delete")
                .param("roleId", roleId))
                .andExpect(status().isForbidden());

        verify(roleService, never()).deleteRole(anyString());
    }

    @Test
    void deleteRole_RoleNotFound() throws Exception {
        String roleId = "non-existent";

        when(roleService.deleteRole(roleId))
                .thenThrow(new RoleException.RoleNotFound(roleId));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_delete")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/role/delete")
                .param("roleId", roleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRole_DefaultRole() throws Exception {
        String roleId = "default-role";

        when(roleService.deleteRole(roleId))
                .thenThrow(new RoleException.ForbiddenOperation("Cannot delete default role"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_delete")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/role/delete")
                .param("roleId", roleId))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRole_RoleAssignedToUsers() throws Exception {
        String roleId = "role-with-users";

        when(roleService.deleteRole(roleId))
                .thenThrow(new RoleException.DeletionConflict("Cannot delete role that is assigned to users"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_delete")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/role/delete")
                .param("roleId", roleId))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteRole_ServerError() throws Exception {
        String roleId = "role-error";

        when(roleService.deleteRole(roleId))
                .thenThrow(new RoleException.DeletionFailed("Failed to delete role"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_delete")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/role/delete")
                .param("roleId", roleId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateRole_Success() throws Exception {
        UpdateRoleNameRequest request = new UpdateRoleNameRequest("role-123", "NEW_ROLE_NAME");

        when(roleService.updateRoleName(any(UpdateRoleNameRequest.class)))
            .thenReturn("Role name updated successfully.");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role name updated successfully."));

        verify(roleService).updateRoleName(any(UpdateRoleNameRequest.class));
    }

    @Test
    void updateRole_WithoutPermission() throws Exception {
        UpdateRoleNameRequest request = new UpdateRoleNameRequest("role-123", "NEW_NAME");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roleService, never()).updateRoleName(any());
    }

    @Test
    void updateRole_RoleNotFound() throws Exception {
        UpdateRoleNameRequest request = new UpdateRoleNameRequest("non-existent", "NEW_NAME");

        when(roleService.updateRoleName(any(UpdateRoleNameRequest.class)))
                .thenThrow(new RoleException.RoleNotFound("non-existent"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRole_DefaultRole() throws Exception {
        UpdateRoleNameRequest request = new UpdateRoleNameRequest("default-role", "NEW_NAME");

        when(roleService.updateRoleName(any(UpdateRoleNameRequest.class)))
                .thenThrow(new RoleException.ForbiddenOperation("Cannot update default role"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_NameAlreadyExists() throws Exception {
        UpdateRoleNameRequest request = new UpdateRoleNameRequest("role-123", "EXISTING_NAME");

        when(roleService.updateRoleName(any(UpdateRoleNameRequest.class)))
                .thenThrow(new RoleException.RoleAlreadyExists("EXISTING_NAME"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateRole_UpdateFailed() throws Exception {
        UpdateRoleNameRequest request = new UpdateRoleNameRequest("role-123", "NEW_NAME");

        when(roleService.updateRoleName(any(UpdateRoleNameRequest.class)))
                .thenThrow(new RoleException.UpdateFailed("Database error"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateRolePermission_Success() throws Exception {
        String roleId = "role-123";
        Map<String, Boolean> permissions = getStringBooleanMap();
        UpdateRolePermissionRequest request = new UpdateRolePermissionRequest(roleId, permissions);

        when(roleService.updateRolePermission(any(UpdateRolePermissionRequest.class)))
            .thenReturn("Role permissions updated successfully.");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role permissions updated successfully."));

        verify(roleService).updateRolePermission(any(UpdateRolePermissionRequest.class));
    }

    @Test
    void updateRolePermission_WithoutPermission() throws Exception {
        UpdateRolePermissionRequest request = new UpdateRolePermissionRequest("role-123", getStringBooleanMap());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roleService, never()).updateRolePermission(any());
    }

    @Test
    void updateRolePermission_RoleNotFound() throws Exception {
        UpdateRolePermissionRequest request = new UpdateRolePermissionRequest("non-existent", getStringBooleanMap());

        when(roleService.updateRolePermission(any(UpdateRolePermissionRequest.class)))
                .thenThrow(new RoleException.RoleNotFound("non-existent"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRolePermission_DefaultRole() throws Exception {
        UpdateRolePermissionRequest request = new UpdateRolePermissionRequest("default-role", getStringBooleanMap());

        when(roleService.updateRolePermission(any(UpdateRolePermissionRequest.class)))
                .thenThrow(new RoleException.ForbiddenOperation("Cannot update permissions of default role"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRolePermission_UpdateFailed() throws Exception {
        UpdateRolePermissionRequest request = new UpdateRolePermissionRequest("role-123", getStringBooleanMap());

        when(roleService.updateRolePermission(any(UpdateRolePermissionRequest.class)))
                .thenThrow(new RoleException.UpdateFailed("Database error"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_edit")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/role/update-permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getListOfRole_Success() throws Exception {
        List<GetListOfRoleResponse> mockRoles = List.of(
                new GetListOfRoleResponse("role-1", "ADMIN"),
                new GetListOfRoleResponse("role-2", "USER")
        );

        when(roleService.getListOfRole()).thenReturn(mockRoles);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/get-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("role-1"))
                .andExpect(jsonPath("$[0].name").value("ADMIN"))
                .andExpect(jsonPath("$[1].id").value("role-2"))
                .andExpect(jsonPath("$[1].name").value("USER"));

        verify(roleService).getListOfRole();
    }

    @Test
    void getListOfRole_WithoutPermission() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/get-list"))
                .andExpect(status().isForbidden());

        verify(roleService, never()).getListOfRole();
    }

    @Test
    void getListOfRole_EmptyList() throws Exception {
        when(roleService.getListOfRole()).thenReturn(Collections.emptyList());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/get-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(roleService).getListOfRole();
    }

    @Test
    void getListOfRole_ServerError() throws Exception {
        when(roleService.getListOfRole()).thenThrow(new RuntimeException("Database error"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/get-list"))
                .andExpect(status().isInternalServerError());

        verify(roleService).getListOfRole();
    }

    @Test
    void getRoleDetail_Success() throws Exception {
        String roleId = "role-123";
        Map<String, Boolean> permissions = getStringBooleanMap();
        GetRoleDetailResponse mockResponse = GetRoleDetailResponse.builder()
                .id(roleId)
                .name("ADMIN")
                .permissions(permissions)
                .createdAt("2023-01-01T10:00:00")
                .updatedAt("2023-01-02T10:00:00")
                .auditLogs(List.of(
                        RoleAuditLogResponse.builder()
                                .actionType(ActionType.CREATE)
                                .roleId(roleId)
                                .roleName("ADMIN")
                                .userId("user-1")
                                .userName("admin")
                                .timestamp("2023-01-01T10:00:00")
                                .build()
                ))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1)
                .build();

        when(roleService.getRoleDetail(eq(roleId), eq(0), eq(10)))
                .thenReturn(mockResponse);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/{roleId}/get-detail", roleId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleId))
                .andExpect(jsonPath("$.name").value("ADMIN"))
                .andExpect(jsonPath("$.permissions").exists())
                .andExpect(jsonPath("$.auditLogs", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(roleService).getRoleDetail(roleId, 0, 10);
    }

    @Test
    void getRoleDetail_RoleNotFound() throws Exception {
        String roleId = "non-existent";

        when(roleService.getRoleDetail(eq(roleId), anyInt(), anyInt()))
                .thenThrow(new RoleException.RoleNotFound(roleId));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/{roleId}/get-detail", roleId))
                .andExpect(status().isNotFound());

        verify(roleService).getRoleDetail(roleId, 0, 10);
    }

    @Test
    void getRoleDetail_WithoutPermission() throws Exception {
        String roleId = "role-123";

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/{roleId}/get-detail", roleId))
                .andExpect(status().isForbidden());

        verify(roleService, never()).getRoleDetail(anyString(), anyInt(), anyInt());
    }

    @Test
    void getRoleDetail_EmptyAuditLogs() throws Exception {
        String roleId = "role-123";
        Map<String, Boolean> permissions = getStringBooleanMap();
        GetRoleDetailResponse mockResponse = GetRoleDetailResponse.builder()
                .id(roleId)
                .name("USER")
                .permissions(permissions)
                .createdAt("2023-01-01T10:00:00")
                .updatedAt("2023-01-01T10:00:00")
                .auditLogs(Collections.emptyList())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .build();

        when(roleService.getRoleDetail(eq(roleId), eq(0), eq(10)))
                .thenReturn(mockResponse);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/{roleId}/get-detail", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditLogs", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getRoleDetail_WithPagination() throws Exception {
        String roleId = "role-123";
        int page = 2;
        int size = 5;

        GetRoleDetailResponse mockResponse = GetRoleDetailResponse.builder()
                .id(roleId)
                .name("ADMIN")
                .currentPage(page)
                .totalPages(4)
                .totalElements(20)
                .auditLogs(List.of(/* some audit logs */))
                .build();

        when(roleService.getRoleDetail(roleId, page, size)).thenReturn(mockResponse);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/{roleId}/get-detail", roleId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.totalElements").value(20));

        verify(roleService).getRoleDetail(roleId, page, size);
    }

    @Test
    void getRoleDetail_ServerError() throws Exception {
        String roleId = "role-error";

        when(roleService.getRoleDetail(eq(roleId), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Server error"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().build(), null,
                List.of(new SimpleGrantedAuthority("role_view")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/role/{roleId}/get-detail", roleId))
                .andExpect(status().isInternalServerError());
    }
}
