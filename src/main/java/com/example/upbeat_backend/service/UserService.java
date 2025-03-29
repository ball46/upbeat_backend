package com.example.upbeat_backend.service;

import com.example.upbeat_backend.dto.request.user.ChangePassword;
import com.example.upbeat_backend.exception.auth.AuthException;
import com.example.upbeat_backend.exception.user.UserException;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String changePassword(String userId, ChangePassword cp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException.NotFound(userId));

        if (!passwordEncoder.matches(cp.getOldPassword(), user.getPassword())) {
            throw new AuthException.InvalidPassword();
        }

        user.setPassword(passwordEncoder.encode(cp.getNewPassword()));
        userRepository.save(user);

        return "Password changed successfully";
    }
}
