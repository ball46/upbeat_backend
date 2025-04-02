package com.example.upbeat_backend.validation.validator.permission;

import com.example.upbeat_backend.security.permission.PermissionConstants;
import com.example.upbeat_backend.validation.annotation.permission.ValidPermissions;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.Set;

public class PermissionsValidator implements ConstraintValidator<ValidPermissions, Map<String, Boolean>> {

    private static final Set<String> REQUIRED_PERMISSIONS = PermissionConstants.getAllPermissions();

    @Override
    public boolean isValid(Map<String, Boolean> permissions, ConstraintValidatorContext context) {
        if (permissions == null) {
            return false;
        }

        if (!permissions.keySet().containsAll(REQUIRED_PERMISSIONS)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Permissions must contain all required keys: " + REQUIRED_PERMISSIONS
            ).addConstraintViolation();
            return false;
        }

        if (!REQUIRED_PERMISSIONS.containsAll(permissions.keySet())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Permissions contain unknown keys"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
