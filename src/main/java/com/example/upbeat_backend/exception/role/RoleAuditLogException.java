package com.example.upbeat_backend.exception.role;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class RoleAuditLogException {
    public static class LoggingFailed extends BaseException {
        public LoggingFailed(String message) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR, "AUDIT_LOG_CREATION_FAILED");
        }
    }
}
