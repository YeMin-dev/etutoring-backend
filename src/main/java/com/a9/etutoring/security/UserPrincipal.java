package com.a9.etutoring.security;

import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final UserRole role;
    private final boolean isActive;
    private final boolean isLocked;

    public UserPrincipal(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String password,
        UserRole role,
        boolean isActive,
        boolean isLocked
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
        this.isLocked = isLocked;
    }

    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPassword(),
            user.getRole(),
            Boolean.TRUE.equals(user.getIsActive()),
            Boolean.TRUE.equals(user.getIsLocked())
        );
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public boolean getIsLocked() {
        return isLocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
