package com.cinevora.repository;

import com.cinevora.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);
    List<UserSession> findByUser_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(Long userId, Instant now);
    Optional<UserSession> findByIdAndUser_IdAndRevokedAtIsNull(Long id, Long userId);
}
