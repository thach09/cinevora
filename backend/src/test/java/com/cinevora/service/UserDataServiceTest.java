package com.cinevora.service;

import com.cinevora.entity.Movie;
import com.cinevora.entity.Profile;
import com.cinevora.entity.WatchHistory;
import com.cinevora.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDataServiceTest {
    @Mock UserRepository users;
    @Mock MovieRepository movies;
    @Mock WatchlistRepository watchlist;
    @Mock FavouriteRepository favourites;
    @Mock WatchHistoryRepository history;
    @Mock ContinueWatchingRepository continueWatching;
    @Mock MovieService movieService;
    @Mock WatchProgressPolicy progressPolicy;
    @Mock ProfileService profiles;

    @Test
    void exportedTitlesNeutralizeSpreadsheetFormulas() {
        Profile profile = mock(Profile.class);
        Movie movie = mock(Movie.class);
        WatchHistory row = mock(WatchHistory.class);
        when(profile.getId()).thenReturn(7L);
        when(profiles.resolve("customer", 7L)).thenReturn(profile);
        when(history.findByProfile_IdOrderByWatchedAtDesc(7L)).thenReturn(List.of(row));
        when(row.getId()).thenReturn(9L);
        when(row.getMovie()).thenReturn(movie);
        when(movie.getId()).thenReturn(61L);
        when(movie.getTitle()).thenReturn("=HYPERLINK(\"https://evil.example\")");
        when(row.getWatchedAt()).thenReturn(Instant.parse("2026-09-23T00:00:00Z"));

        UserDataService service = new UserDataService(users, movies, watchlist, favourites, history, continueWatching, movieService, progressPolicy, profiles);
        String csv = new String(service.exportHistory("customer", 7L));

        assertTrue(csv.contains("\"'=HYPERLINK(\"\"https://evil.example\"\")\""));
    }
}
