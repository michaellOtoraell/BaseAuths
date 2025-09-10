package com.otorael.BaseAuths.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auths implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "first name is required")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "last name is required")
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    @NotBlank(message = "email is required")
    @Email(message = "email is not valid")
    private String email;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "password is required")
    private String password;

    @Column(name = "role", nullable = false)
    @NotNull(message = "role is required")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_failed_login")
    private LocalDateTime lastFailedLogin;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (accountLockedUntil == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(accountLockedUntil);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}