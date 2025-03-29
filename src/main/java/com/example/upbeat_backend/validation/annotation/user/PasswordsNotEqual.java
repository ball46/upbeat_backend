package com.example.upbeat_backend.validation.annotation.user;

import com.example.upbeat_backend.validation.validator.user.PasswordsNotEqualValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsNotEqualValidator.class)
@Documented
public @interface PasswordsNotEqual {
    String message() default "New password must be different from old password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
