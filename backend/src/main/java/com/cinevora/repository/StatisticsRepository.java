package com.cinevora.repository;

import com.cinevora.entity.Movie;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface StatisticsRepository extends Repository<Movie, Long> {
    @Query("select coalesce(sum(m.views), 0) from Movie m") long totalViews();
    @Query("select coalesce(sum(m.favouritesCount), 0) from Movie m") long totalFavourites();
    @Query("select m from Movie m join fetch m.category where m.active = true order by m.views desc") List<Movie> topMovies();
}
