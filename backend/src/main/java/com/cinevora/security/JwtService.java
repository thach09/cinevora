package com.cinevora.security;

import com.cinevora.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }
    public String generate(User user) { return generate(user, null); }
    public String generate(User user, Long profileId) {
        Instant now = Instant.now();
        var builder = Jwts.builder().subject(user.getUsername()).issuer("cinevora")
                .audience().add("cinevora-api").and().id(java.util.UUID.randomUUID().toString())
                .claim("sv", user.getSecurityVersion()).claim("role", user.getRole().name());
        if (profileId != null) builder.claim("profileId", profileId);
        return builder
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key).compact();
    }
    public String extractUsername(String token) {
        return claims(token).getSubject();
    }
    private io.jsonwebtoken.Claims claims(String token) {
        if (token == null || token.length() > 4096) throw new IllegalArgumentException("Invalid token");
        return Jwts.parser().verifyWith(key).requireIssuer("cinevora").requireAudience("cinevora-api")
                .build().parseSignedClaims(token).getPayload();
    }
    public boolean matchesUser(String token, User user) {
        try { return java.util.Objects.equals(claims(token).get("sv", Long.class), user.getSecurityVersion()); }
        catch (RuntimeException ex) { return false; }
    }
    public boolean isValid(String token) {
        try { return extractUsername(token) != null; } catch (RuntimeException ex) { return false; }
    }
    public long getExpirationSeconds() { return expirationSeconds; }
}
