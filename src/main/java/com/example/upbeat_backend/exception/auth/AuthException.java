package com.example.upbeat_backend.exception.auth;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class AuthException {
    public static class DuplicateEmail extends BaseException {
        public DuplicateEmail(String email) {
            super("User with email " + email + " already exists",
                    HttpStatus.CONFLICT,
                    "USER_EMAIL_DUPLICATE");
        }
    }

    public static class InvalidCredentials extends BaseException {
        public InvalidCredentials() {
            super("Invalid username or password",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS");
        }
    }

    public static class InvalidPassword extends BaseException {
        public InvalidPassword() {
            super("Current password is incorrect",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PASSWORD");
        }
    }
}