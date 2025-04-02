package com.example.upbeat_backend.security.permission;

import com.example.upbeat_backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(CustomPermissionEvaluator.class);
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        try {
            if (auth == null) {
                logger.warn("Authentication object is null");
                return false;
            }

            if (!(auth.getPrincipal() instanceof UserPrincipal)) {
                logger.warn("Principal is not UserPrincipal, it is: {}",
                        auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
                return false;
            }

            String permissionStr = permission.toString();
            logger.info("Checking permission: {} for user: {}", permissionStr, auth.getName());
            logger.info("User authorities: {}", auth.getAuthorities());

            boolean hasPermission = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals(permissionStr));

            logger.info("Permission check result: {}", hasPermission);
            return hasPermission;
        } catch (Exception e) {
            logger.error("Error evaluating permission", e);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return hasPermission(auth, null, permission);
    }
}