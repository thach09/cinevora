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
    private final UserSessionRepository sessions;
    private final long refreshDays;

    public SessionService(UserSessionRepository sessions, @Value("${app.auth.refresh-days:30}") long refreshDays) {
        this.sessions = sessions; this.refreshDays = Math.max(1, refreshDays);
    }

    @Transactional
    public String create(User user, String userAgent, String ipAddress) {
        String refreshToken = TokenService.randomToken();
        sessions.save(new UserSession(user, TokenService.sha256(refreshToken), userAgent, ipAddress, Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
        return refreshToken;
    }

    @Transactional
    public User rotate(String refreshToken, String userAgent, String ipAddress) {
        UserSession old = active(refreshToken);
        old.revoke();
        User user = old.getUser();
        create(user, userAgent, ipAddress);
        return user;
    }

    @Transactional
    public String rotateAndReturnToken(String refreshToken, String userAgent, String ipAddress) {
        UserSession old = active(refreshToken);
        old.revoke();
        return create(old.getUser(), userAgent, ipAddress);
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

    @Transactional(readOnly = true)
    public User activeUser(String refreshToken) { return active(refreshToken).getUser(); }

    private UserSession active(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new BusinessException("Refresh token không hợp lệ hoặc đã hết hạn");
        UserSession session = sessions.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(TokenService.sha256(refreshToken), Instant.now())
                .orElseThrow(() -> new BusinessException("Refresh token không hợp lệ hoặc đã hết hạn"));
        session.touch();
        return session;
    }
}
