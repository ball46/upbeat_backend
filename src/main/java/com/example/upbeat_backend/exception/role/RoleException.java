package com.example.upbeat_backend.exception.role;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

public class RoleException extends Throwable {
    @Value("${app.default_role_name}")
    private static String defaultRoleName;

    public static class RoleNotFound extends BaseException {
        public RoleNotFound(String id) {
            super(determineMessage(id), HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND");
        }
        private static String determineMessage(String id) {
            return id.equals(defaultRoleName) ?
                    "Default role '" + defaultRoleName + "' not found" :
                    "Role with id " + id + " not found";
        }
    }

    public static class RoleAlreadyExists extends BaseException {
        public RoleAlreadyExists(String name) {
            super("Role with name '" + name + "' already exists", HttpStatus.CONFLICT, "ROLE_ALREADY_EXISTS");
        }
    }

    public static class CreationFailed extends BaseException {
        public CreationFailed(String name) {
            super("Failed to create role '" + name + "'", HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_CREATION_FAILED");
        }
    }


}
