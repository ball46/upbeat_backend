package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.dto.response.login_history.LoginHistoryResponse;
import com.example.upbeat_backend.exception.auth.LoginHistoryException;
import com.example.upbeat_backend.model.enums.LoginStatus;
import com.example.upbeat_backend.security.CurrentUser;
import com.example.upbeat_backend.security.UserPrincipal;
import com.example.upbeat_backend.service.LoginHistoryService;
import com.example.upbeat_backend.util.DateFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class LoginHistoryControllerTest {
    private MockMvc mockMvc;

    @Mock
    private LoginHistoryService loginHistoryService;

    @InjectMocks
    private LoginHistoryController loginHistoryController;

    @RestControllerAdvice
    public static class TestControllerAdvice {
        @ExceptionHandler(LoginHistoryException.NullStatusException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleNullStatusException(LoginHistoryException.NullStatusException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(LoginHistoryException.NullUserException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleNullUserException(LoginHistoryException.NullUserException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(LoginHistoryException.InvalidDateFormatException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInvalidDateFormatException(LoginHistoryException.InvalidDateFormatException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ignoredEx) {
            return new ResponseEntity<>("Access Denied", HttpStatus.FORBIDDEN);
        }
    }

    @BeforeEach
    public void setup() {
        SecurityContextHolder.clearContext();

        this.mockMvc = MockMvcBuilders
                .standaloneSetup(loginHistoryController)
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
    void getAllLoginHistories_Success() throws Exception {
        String userId = "user123";
        UserPrincipal userPrincipal = UserPrincipal.builder().id(userId).username("testUser").build();

        LoginHistoryResponse historyResponse = LoginHistoryResponse.builder()
                .date("31/03/2023")
                .time("10:30")
                .ipAddress("192.168.1.1")
                .deviceType("Computer")
                .browser("Chrome")
                .os("Windows")
                .status(LoginStatus.SUCCESS)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<LoginHistoryResponse> page = new PageImpl<>(List.of(historyResponse), pageable, 1);

        when(loginHistoryService.findByUserIdPaginated(eq(userId), eq(0), eq(10)))
                .thenReturn(page);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/login-history/limit")
                        .param("page", "0")
                        .param("size", "10")
                        .with(request -> {
                            request.setUserPrincipal(auth);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].ipAddress").value("192.168.1.1"))
                .andExpect(jsonPath("$.content[0].date").value("31/03/2023"))
                .andExpect(jsonPath("$.content[0].time").value("10:30"));
    }

    @Test
    void getAllLoginHistoriesForUser_Admin_Success() throws Exception {
        String adminId = "admin123";
        String targetUserId = "user456";
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(adminId).username("admin").build();

        LoginHistoryResponse historyResponse = LoginHistoryResponse.builder()
                .date("31/03/2023")
                .time("10:30")
                .ipAddress("192.168.1.1")
                .status(LoginStatus.SUCCESS)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<LoginHistoryResponse> page = new PageImpl<>(List.of(historyResponse), pageable, 1);

        when(loginHistoryService.findByUserIdPaginated(eq(targetUserId), eq(0), eq(10)))
                .thenReturn(page);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                adminPrincipal, null, List.of(new SimpleGrantedAuthority("Admin")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/login-history/user/{userId}/limit", targetUserId)
                        .param("page", "0")
                        .param("size", "10")
                        .with(request -> {
                            request.setUserPrincipal(auth);
                            SecurityContextHolder.getContext().setAuthentication((Authentication) request.getUserPrincipal());
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].ipAddress").value("192.168.1.1"));
    }

    @Test
    void getAllLoginHistoriesForUser_NonAdmin_Forbidden() throws Exception {
        String normalUserId = "user123";
        String targetUserId = "user456";
        UserPrincipal userPrincipal = UserPrincipal.builder().id(normalUserId).username("user").build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(loginHistoryService.findByUserIdPaginated(eq(targetUserId), eq(0), eq(10)))
                .thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/login-history/user/{userId}/limit", targetUserId)
                        .param("page", "0")
                        .param("size", "10")
                        .with(request -> {
                            request.setUserPrincipal(auth);
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFailedLoginAttempts_Success() throws Exception {
        String userId = "user123";
        String dateString = "2023-05-01";
        LocalDateTime sinceDate = LocalDateTime.of(2023, 5, 1, 0, 0);
        UserPrincipal userPrincipal = UserPrincipal.builder().id(userId).username("testUser").build();

        try (MockedStatic<DateFormat> mockedStatic = Mockito.mockStatic(DateFormat.class)) {
            mockedStatic.when(() -> DateFormat.parseDateString(dateString))
                    .thenReturn(sinceDate);

            when(loginHistoryService.countFailedLoginAttemptsSince(eq(userId), eq(sinceDate)))
                    .thenReturn(5);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            mockMvc.perform(get("/login-history/failed-login-attempts")
                            .param("since", dateString))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }
    }

    @Test
    void getFailedLoginAttemptsForUser_Admin_Success() throws Exception {
        String adminId = "admin123";
        String targetUserId = "user456";
        String dateString = "2023-05-01";
        LocalDateTime sinceDate = LocalDateTime.of(2023, 5, 1, 0, 0);
        UserPrincipal adminPrincipal = UserPrincipal.builder().id(adminId).username("admin").build();

        try (MockedStatic<DateFormat> mockedStatic = Mockito.mockStatic(DateFormat.class)) {
            mockedStatic.when(() -> DateFormat.parseDateString(dateString))
                    .thenReturn(sinceDate);

            when(loginHistoryService.countFailedLoginAttemptsSince(eq(targetUserId), eq(sinceDate)))
                    .thenReturn(3);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    adminPrincipal, null, List.of(new SimpleGrantedAuthority("Admin")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            mockMvc.perform(get("/login-history/user/{userId}/failed-login-attempts", targetUserId)
                            .param("since", dateString))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
        }
    }

    @Test
    void getFailedLoginAttemptsForUser_NonAdmin_Forbidden() throws Exception {
        String normalUserId = "user123";
        String targetUserId = "user456";
        String dateString = "2023-05-01";
        LocalDateTime sinceDate = LocalDateTime.of(2023, 5, 1, 0, 0);
        UserPrincipal userPrincipal = UserPrincipal.builder().id(normalUserId).username("user").build();

        try (MockedStatic<DateFormat> mockedStatic = Mockito.mockStatic(DateFormat.class)) {
            mockedStatic.when(() -> DateFormat.parseDateString(dateString))
                    .thenReturn(sinceDate);

            when(loginHistoryService.countFailedLoginAttemptsSince(eq(targetUserId), eq(sinceDate)))
                    .thenThrow(new AccessDeniedException("Access Denied"));

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            mockMvc.perform(get("/login-history/user/{userId}/failed-login-attempts", targetUserId)
                            .param("since", dateString)
                            .with(request -> {
                                request.setUserPrincipal(auth);
                                return request;
                            }))
                    .andExpect(status().isForbidden());
        }
    }
}