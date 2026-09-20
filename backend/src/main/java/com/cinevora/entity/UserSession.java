package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "last_used_at", nullable = false) private Instant lastUsedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;

    protected UserSession() {}
    public UserSession(User user, String tokenHash, String userAgent, String ipAddress, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.userAgent = userAgent; this.ipAddress = ipAddress; this.expiresAt = expiresAt;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); lastUsedAt = createdAt; }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
    public void touch() { lastUsedAt = Instant.now(); }
    public void revoke() { revokedAt = Instant.now(); }
}
