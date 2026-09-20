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
        var builder = Jwts.builder().subject(user.getUsername()).claim("role", user.getRole().name());
        if (profileId != null) builder.claim("profileId", profileId);
        return builder
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key).compact();
    }
    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean isValid(String token) {
        try { return extractUsername(token) != null; } catch (RuntimeException ex) { return false; }
    }
    public long getExpirationSeconds() { return expirationSeconds; }
}
