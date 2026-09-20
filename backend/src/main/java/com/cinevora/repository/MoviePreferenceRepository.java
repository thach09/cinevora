package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MoviePreferenceRepository extends JpaRepository<MoviePreference, MoviePreferenceId> {
    List<MoviePreference> findByProfile_Id(Long profileId);
    Optional<MoviePreference> findByProfile_IdAndMovie_Id(Long profileId, Long movieId);
    @Query("select p.movie.id from MoviePreference p where p.profile.id = :profileId and p.signal = com.cinevora.entity.PreferenceSignal.DISLIKE")
    List<Long> findDislikedMovieIds(@Param("profileId") Long profileId);
    @Query("select distinct p.movie.category.id from MoviePreference p where p.profile.id = :profileId and p.signal = com.cinevora.entity.PreferenceSignal.LIKE")
    List<Long> findLikedCategoryIds(@Param("profileId") Long profileId);
}
