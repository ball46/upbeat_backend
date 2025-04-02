package com.example.upbeat_backend.validation.annotation.permission;

import com.example.upbeat_backend.validation.validator.permission.PermissionsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PermissionsValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPermissions {
    String message() default "Invalid permissions structure";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
