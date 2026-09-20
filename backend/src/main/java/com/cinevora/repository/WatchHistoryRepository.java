package com.cinevora.repository;

import com.cinevora.entity.WatchHistory;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    List<WatchHistory> findByProfile_IdOrderByWatchedAtDesc(Long profileId);
    boolean existsByProfile_IdAndMovie_Id(Long profileId, Long movieId);
    @Query("select h from WatchHistory h join fetch h.movie m where h.user.id = :userId order by h.watchedAt desc")
    List<WatchHistory> findRecentWithMovie(Long profileId);
    long countByProfile_Id(Long profileId);
}
