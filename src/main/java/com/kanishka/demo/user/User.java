package com.kanishka.demo.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails {

    // ===============================
    // PRIMARY KEY
    // ===============================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===============================
    // BASIC INFO
    // ===============================

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ===============================
    // PROFILE PHOTO
    // ===============================

    @Lob
    @Column(columnDefinition = "VARBINARY(MAX)")
    private byte[] profilePhoto;

    // ===============================
    // ACCOUNT STATUS
    // ===============================

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = false;   // false until email verified

    // ===============================
    // EMAIL VERIFICATION
    // ===============================

    @Builder.Default
    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(unique = true)
    private String verificationToken;

    private LocalDateTime verificationTokenExpiry;

    // ===============================
    // CONTACT DETAILS
    // ===============================

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    // ===============================
    // WHOLESALE FLAGS
    // ===============================

    @Builder.Default
    @Column(nullable = false)
    private Boolean wholesaleRequested = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean wholesaleApproved = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean wholesalePayLater = true;

    // ===============================
    // TIMESTAMPS
    // ===============================

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (enabled == null)            enabled = false;
        if (emailVerified == null)      emailVerified = false;
        if (wholesaleRequested == null) wholesaleRequested = false;
        if (wholesaleApproved == null)  wholesaleApproved = false;
        if (wholesalePayLater == null)  wholesalePayLater = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===============================
    // SPRING SECURITY METHODS
    // ===============================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(role.name())
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
}