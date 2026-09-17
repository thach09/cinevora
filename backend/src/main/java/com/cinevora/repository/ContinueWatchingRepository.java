package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContinueWatchingRepository extends JpaRepository<ContinueWatching, ContinueWatchingId> {
    List<ContinueWatching> findByUser_IdOrderByUpdatedAtDesc(Long userId);
}
