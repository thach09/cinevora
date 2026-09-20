package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, WatchlistId> {
    List<WatchlistEntry> findByProfile_IdOrderByAddedAtDesc(Long profileId);
    boolean existsByProfile_IdAndMovie_Id(Long profileId, Long movieId);
}
