package com.example.upbeat_backend.exception.auth;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class LoginHistoryException {
    public static class NullUserException extends BaseException {
        public NullUserException() {
            super("User cannot be null when recording login history",
                    HttpStatus.BAD_REQUEST,
                    "NULL_USER_ERROR");
        }
    }

    public static class NullStatusException extends BaseException {
        public NullStatusException() {
            super("Login status cannot be null when recording login history",
                    HttpStatus.BAD_REQUEST,
                    "NULL_STATUS_ERROR");
        }
    }
}