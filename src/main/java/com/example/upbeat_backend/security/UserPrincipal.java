package com.example.upbeat_backend.security;

import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.model.enums.AccountStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {
    @Getter
    private final String id;
    private final String username;
    @Getter
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final AccountStatus status;

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));

            Map<String, Boolean> permissions = user.getRole().getPermissions();
            if (permissions != null) {
                permissions.forEach((permission, enabled) -> {
                    if (Boolean.TRUE.equals(enabled)) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }
                });
            }
        }

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.getStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != AccountStatus.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }
}
