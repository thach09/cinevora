package com.cinevora.repository;

import com.cinevora.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findByUser_IdOrderByCreatedAtAsc(Long userId);
    Optional<Profile> findByUser_IdAndDefaultProfileTrue(Long userId);
    Optional<Profile> findByIdAndUser_Id(Long id, Long userId);
    long countByUser_Id(Long userId);
}
