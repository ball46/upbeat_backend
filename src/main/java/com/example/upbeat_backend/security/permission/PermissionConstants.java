package com.example.upbeat_backend.security.permission;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public final class PermissionConstants {
    public static final String USER_VIEW = "user_view";
    public static final String USER_CREATE = "user_create";
    public static final String USER_EDIT = "user_edit";
    public static final String USER_DELETE = "user_delete";

    public static final String ROLE_VIEW = "role_view";
    public static final String ROLE_CREATE = "role_create";
    public static final String ROLE_EDIT = "role_edit";
    public static final String ROLE_DELETE = "role_delete";

    public static final String CONTENT_VIEW = "content_view";
    public static final String CONTENT_CREATE = "content_create";
    public static final String CONTENT_EDIT = "content_edit";
    public static final String CONTENT_DELETE = "content_delete";

    public static final String SYSTEM_SETTINGS = "system_settings";

    private PermissionConstants() {}

    public static Set<String> getAllPermissions() {
        Set<String> permissions = new HashSet<>();
        for (Field field : PermissionConstants.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers()) &&
                    field.getType() == String.class) {
                try {
                    permissions.add((String) field.get(null));
                } catch (IllegalAccessException ignored) {}
            }
        }
        return permissions;
    }
}