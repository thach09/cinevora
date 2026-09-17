package com.cinevora.dto;

import java.util.List;

public record StatisticsResponse(long totalUsers, long activeUsers, long totalMovies, long activeMovies,
                                 long totalCategories, long totalViews, long totalFavourites,
                                 List<MovieDtos.Response> topMovies) {}
