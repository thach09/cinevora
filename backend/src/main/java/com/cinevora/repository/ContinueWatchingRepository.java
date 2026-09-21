package com.cinevora.repository;

import com.cinevora.entity.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContinueWatchingRepository extends JpaRepository<ContinueWatching, ContinueWatchingId> {
    @Query("select c from ContinueWatching c join fetch c.movie where c.profile.id = :profileId order by c.updatedAt desc")
    List<ContinueWatching> findByProfile_IdOrderByUpdatedAtDesc(@Param("profileId") Long profileId);
}
