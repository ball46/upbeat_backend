package com.example.upbeat_backend.exception.auth;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class TokenRefreshException {
    public static class TokenExpired extends BaseException {
        public TokenExpired() {
            super("Refresh token was expired",
                  HttpStatus.FORBIDDEN,
                  "REFRESH_TOKEN_EXPIRED");
        }
    }

    public static class TokenNotFound extends BaseException {
        public TokenNotFound() {
            super("Refresh token not found",
                  HttpStatus.NOT_FOUND,
                  "REFRESH_TOKEN_NOT_FOUND");
        }
    }

    public static class TokenReuse extends BaseException {
        public TokenReuse() {
            super("Refresh token reuse detected",
                  HttpStatus.FORBIDDEN,
                  "REFRESH_TOKEN_REUSED");
        }
    }
}