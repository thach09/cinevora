package com.cinevora.service;

import com.cinevora.dto.MovieDtos;
import com.cinevora.dto.StatisticsResponse;
import com.cinevora.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
    private final UserRepository users; private final MovieRepository movies; private final CategoryRepository categories; private final StatisticsRepository statistics;
    public StatisticsService(UserRepository users, MovieRepository movies, CategoryRepository categories, StatisticsRepository statistics) { this.users = users; this.movies = movies; this.categories = categories; this.statistics = statistics; }
    @Transactional(readOnly = true)
    public StatisticsResponse get() {
        return new StatisticsResponse(users.count(), users.countByActiveTrue(), movies.count(), movies.countByActiveTrue(), categories.countByActiveTrue(), statistics.totalViews(), statistics.totalFavourites(), statistics.topMovies().stream().limit(10).map(MovieDtos.Response::from).toList());
    }
}
