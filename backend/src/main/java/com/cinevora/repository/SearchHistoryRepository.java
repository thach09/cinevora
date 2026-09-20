package com.cinevora.repository;

import com.cinevora.entity.SearchHistory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findTop20ByProfile_IdOrderBySearchedAtDesc(Long profileId);
    long countByProfile_Id(Long profileId);
    @Query("select lower(h.query), count(h) from SearchHistory h group by lower(h.query) order by count(h) desc")
    List<Object[]> findPopularQueries(org.springframework.data.domain.Pageable pageable);
    @Query("select h from SearchHistory h where h.profile.id = :profileId order by h.searchedAt desc")
    List<SearchHistory> findAllRecent(@Param("profileId") Long profileId);
}
