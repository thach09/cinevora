package com.cinevora.service;

import com.cinevora.entity.User;
import com.cinevora.entity.UserSession;
import com.cinevora.dto.AccountDtos;
import com.cinevora.exception.BusinessException;
import com.cinevora.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SessionService {
    @org.springframework.beans.factory.annotation.Value("${app.auth.block-demo-identities:false}") private boolean blockDemo;
    private final UserSessionRepository sessions;
    private final long refreshDays;

    public SessionService(UserSessionRepository sessions, @Value("${app.auth.refresh-days:30}") long refreshDays) {
        this.sessions = sessions; this.refreshDays = Math.max(1, refreshDays);
    }

    @Transactional
    public String create(User user, String userAgent, String ipAddress) {
        String refreshToken = TokenService.randomToken();
        sessions.save(new UserSession(user, TokenService.sha256(refreshToken), bounded(userAgent, 500), bounded(ipAddress, 64), Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
        return refreshToken;
    }

    public record Rotation(User user, String token) {}

    /** The database lock serializes consumption across threads and processes. */
    @Transactional
    public Rotation consume(String refreshToken, String userAgent, String ipAddress) {
        if (refreshToken == null || !refreshToken.matches("[A-Za-z0-9_-]{64}"))
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid refresh credential");
        UserSession old = sessions.lockActive(TokenService.sha256(refreshToken), Instant.now())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid refresh credential"));
        if (!old.getUser().isActive() || (blockDemo && old.getUser().getId() <= 11))
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid refresh credential");
        old.revoke();
        return new Rotation(old.getUser(), create(old.getUser(), userAgent, ipAddress));
    }

    private String bounded(String value, int maximum) {
        return value == null ? null : value.replaceAll("[\\r\\n\\p{Cntrl}]", " ").substring(0, Math.min(value.length(), maximum));
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(TokenService.sha256(refreshToken), Instant.now()).ifPresent(UserSession::revoke);
    }

    @Transactional
    public void revokeAll(User user) {
        sessions.findByUser_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(user.getId(), Instant.now()).forEach(UserSession::revoke);
    }

    @Transactional(readOnly = true)
    public List<AccountDtos.SessionResponse> list(User user) {
        return sessions.findByUser_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(user.getId(), Instant.now()).stream()
                .map(s -> new AccountDtos.SessionResponse(s.getId(), s.getUserAgent(), s.getIpAddress(), s.getCreatedAt(), s.getLastUsedAt(), s.getExpiresAt())).toList();
    }

    @Transactional
    public void revoke(User user, Long id) {
        sessions.findByIdAndUser_IdAndRevokedAtIsNull(id, user.getId()).ifPresent(UserSession::revoke);
    }

}
