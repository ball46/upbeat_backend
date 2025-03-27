package com.example.upbeat_backend.exception.user;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class UserException {

    public static class NotFound extends BaseException {
        public NotFound(String id) {
            super("User not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "USER_NOT_FOUND");
        }
    }
}