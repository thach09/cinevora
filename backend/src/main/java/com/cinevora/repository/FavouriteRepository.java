package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavouriteRepository extends JpaRepository<FavouriteEntry, FavouriteId> {
    List<FavouriteEntry> findByProfile_IdOrderByAddedAtDesc(Long profileId);
    boolean existsByProfile_IdAndMovie_Id(Long profileId, Long movieId);
}
