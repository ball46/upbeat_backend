package com.example.upbeat_backend.validation.validator.user;

import com.example.upbeat_backend.dto.request.user.ChangePassword;
import com.example.upbeat_backend.validation.annotation.user.PasswordsNotEqual;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsNotEqualValidator implements ConstraintValidator<PasswordsNotEqual, ChangePassword> {
    @Override
    public boolean isValid(ChangePassword changePassword, ConstraintValidatorContext context) {
        return !changePassword.getOldPassword().equals(changePassword.getNewPassword());
    }
}
