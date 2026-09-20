package com.cinevora.repository;

import com.cinevora.entity.WatchHistory;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    List<WatchHistory> findByProfile_IdOrderByWatchedAtDesc(Long profileId);
    boolean existsByProfile_IdAndMovie_Id(Long profileId, Long movieId);
    @Query("select h from WatchHistory h join fetch h.movie m where h.profile.id = :profileId order by h.watchedAt desc")
    List<WatchHistory> findRecentWithMovie(Long profileId);
    long countByProfile_Id(Long profileId);
    @Query("select h.movie.id from WatchHistory h where h.profile.id = :profileId")
    List<Long> findMovieIdsByProfile(@org.springframework.data.repository.query.Param("profileId") Long profileId);
    @Query("select h.movie.category.id, count(h) from WatchHistory h where h.profile.id = :profileId group by h.movie.category.id")
    List<Object[]> countByCategoryForProfile(@org.springframework.data.repository.query.Param("profileId") Long profileId);
}
