package com.example.upbeat_backend.security;

import com.example.upbeat_backend.security.jwt.JwtAuthenticationFilter;
import com.example.upbeat_backend.security.jwt.JwtTokenProvider;
import com.example.upbeat_backend.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilterInternal_ValidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn("1");

        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserById("1")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_NoToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenProvider, never()).getUserIdFromToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(anyString());
    }

    @Test
    void doFilterInternal_MalformedToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("NotBearer token");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenProvider, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternal_ExceptionThrown() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn("1");
        when(customUserDetailsService.loadUserById("1")).thenThrow(new RuntimeException("User not found"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_SetsAuthenticationInContext() throws ServletException, IOException {
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn("1");

        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserById("1")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }
}
