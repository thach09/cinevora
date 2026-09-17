package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavouriteRepository extends JpaRepository<FavouriteEntry, FavouriteId> {
    List<FavouriteEntry> findByUser_IdOrderByAddedAtDesc(Long userId);
    boolean existsByUser_IdAndMovie_Id(Long userId, Long movieId);
}
