package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.user.ChangePassword;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.exception.user.UserException;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void ChangePassword_Success() {
        String userId = "testUserId";
        ChangePassword cp = ChangePassword.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        User user = User.builder()
                .id(userId)
                .username("testUser")
                .password("encodedOldPassword")
                .build();

        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches(cp.getOldPassword(), user.getPassword())).thenReturn(true);
        Mockito.when(passwordEncoder.encode(cp.getNewPassword())).thenReturn("encodedNewPassword");

        String result = userService.changePassword(userId, cp);

        assertEquals("Password changed successfully", result);
        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_UserNotFound() {
        String userId = "nonExistentId";
        ChangePassword cp = ChangePassword.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserException.NotFound.class, () ->
                userService.changePassword(userId, cp)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_InvalidOldPassword() {
        // Arrange
        String userId = "testUserId";
        ChangePassword cp = ChangePassword.builder()
                .oldPassword("wrongPassword")
                .newPassword("newPassword")
                .build();

        User user = User.builder()
                .id(userId)
                .username("testUser")
                .password("encodedOldPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(AuthException.InvalidPassword.class, () ->
                userService.changePassword(userId, cp)
        );
        verify(userRepository, never()).save(any());
    }
}
