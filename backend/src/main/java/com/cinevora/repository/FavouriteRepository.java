package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FavouriteRepository extends JpaRepository<FavouriteEntry, FavouriteId> {
    @Query("select f from FavouriteEntry f join fetch f.movie where f.profile.id = :profileId order by f.addedAt desc")
    List<FavouriteEntry> findByProfile_IdOrderByAddedAtDesc(@Param("profileId") Long profileId);
    boolean existsByProfile_IdAndMovie_Id(Long profileId, Long movieId);
}
