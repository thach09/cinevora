package com.cinevora.repository;

import com.cinevora.entity.MovieTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MovieTrackRepository extends JpaRepository<MovieTrack, Long> {
    List<MovieTrack> findByMovie_IdAndActiveTrueOrderByKindAscDefaultTrackDescLabelAsc(Long movieId);
    List<MovieTrack> findByMovie_IdOrderByKindAscActiveDescLabelAsc(Long movieId);
    Optional<MovieTrack> findByIdAndMovie_Id(Long id, Long movieId);
}
