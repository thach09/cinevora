package com.cinevora.repository;

import com.cinevora.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndReadAtIsNull(Long userId);
    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);
    List<Notification> findByUser_IdAndReadAtIsNull(Long userId);
}
