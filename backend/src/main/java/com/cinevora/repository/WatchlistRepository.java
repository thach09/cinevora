package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, WatchlistId> {
    @Query("select w from WatchlistEntry w join fetch w.movie where w.profile.id = :profileId order by w.addedAt desc")
    List<WatchlistEntry> findByProfile_IdOrderByAddedAtDesc(@Param("profileId") Long profileId);
    boolean existsByProfile_IdAndMovie_Id(Long profileId, Long movieId);
}
