package com.cinevora.repository;

import com.cinevora.entity.WatchHistory;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    List<WatchHistory> findByUser_IdOrderByWatchedAtDesc(Long userId);
    @Query("select h from WatchHistory h join fetch h.movie m where h.user.id = :userId order by h.watchedAt desc")
    List<WatchHistory> findRecentWithMovie(Long userId);
    long countByUser_Id(Long userId);
}
