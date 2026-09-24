package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "security_version", nullable = false) private long securityVersion;
    public long getSecurityVersion() { return securityVersion; }
    public void invalidateCredentials() { securityVersion++; }
    @Column(nullable = false, length = 50, unique = true) private String username;
    @Column(nullable = false, length = 100, unique = true) private String email;
    @Column(nullable = false, length = 255) private String password;
    @Column(name = "full_name", nullable = false, length = 100) private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified = false;
    @Column(name = "email_verification_token_hash", length = 64) private String emailVerificationTokenHash;
    @Column(name = "email_verification_expires_at") private Instant emailVerificationExpiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getEmailVerificationTokenHash() { return emailVerificationTokenHash; }
    public void setEmailVerificationTokenHash(String value) { this.emailVerificationTokenHash = value; }
    public Instant getEmailVerificationExpiresAt() { return emailVerificationExpiresAt; }
    public void setEmailVerificationExpiresAt(Instant value) { this.emailVerificationExpiresAt = value; }
}
