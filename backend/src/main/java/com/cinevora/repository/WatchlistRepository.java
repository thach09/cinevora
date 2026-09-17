package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, WatchlistId> {
    List<WatchlistEntry> findByUser_IdOrderByAddedAtDesc(Long userId);
    boolean existsByUser_IdAndMovie_Id(Long userId, Long movieId);
}
