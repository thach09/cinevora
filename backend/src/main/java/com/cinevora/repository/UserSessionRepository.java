package com.cinevora.repository;

import com.cinevora.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from UserSession s where s.tokenHash = :hash and s.revokedAt is null and s.expiresAt > :now")
    Optional<UserSession> lockActive(@org.springframework.data.repository.query.Param("hash") String hash,
                                    @org.springframework.data.repository.query.Param("now") Instant now);
    Optional<UserSession> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);
    List<UserSession> findByUser_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(Long userId, Instant now);
    Optional<UserSession> findByIdAndUser_IdAndRevokedAtIsNull(Long id, Long userId);
}
