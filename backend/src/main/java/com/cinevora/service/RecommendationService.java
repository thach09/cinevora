package com.cinevora.service;
import com.cinevora.dto.DiscoveryDtos;
import com.cinevora.dto.MovieDtos;
import com.cinevora.dto.UserDataDtos;
import com.cinevora.entity.Movie;
import com.cinevora.entity.Profile;
import com.cinevora.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Service public class RecommendationService {
    private static final Sort CANDIDATE_ORDER = Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("views"), Sort.Order.asc("id"));
    private final ProfileService profiles; private final MoviePreferenceRepository preferences; private final WatchHistoryRepository history; private final MovieRepository movies; private final ContinueWatchingRepository continueWatching;
    public RecommendationService(ProfileService profiles, MoviePreferenceRepository preferences, WatchHistoryRepository history, MovieRepository movies, ContinueWatchingRepository continueWatching) { this.profiles = profiles; this.preferences = preferences; this.history = history; this.movies = movies; this.continueWatching = continueWatching; }
    @Transactional(readOnly = true) public List<MovieDtos.Response> recommend(String username, Long profileId, int limit) {
        Profile profile = profiles.resolve(username, profileId); int take = Math.max(1, Math.min(limit, 24)); Set<Long> excluded = new HashSet<>(history.findMovieIdsByProfile(profile.getId())); excluded.addAll(preferences.findDislikedMovieIds(profile.getId()));
        Map<Long, Long> categoryCounts = history.countByCategoryForProfile(profile.getId()).stream().collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).longValue())); List<Long> likedCategories = preferences.findLikedCategoryIds(profile.getId());
        PageRequest candidatePage = PageRequest.of(0, 100, CANDIDATE_ORDER); List<Movie> candidates = excluded.isEmpty() ? movies.findActiveCandidates(candidatePage) : movies.findActiveCandidatesExcluding(excluded, candidatePage); double maxViews = Math.max(1d, candidates.stream().map(Movie::getViews).mapToDouble(value -> Math.log1p(value == null ? 0 : value)).max().orElse(1));
        return candidates.stream().map(movie -> new Scored(movie, score(movie, categoryCounts, likedCategories, maxViews))).sorted(Comparator.comparingDouble(Scored::score).reversed().thenComparingLong(scored -> scored.movie().getId())).limit(take).map(scored -> MovieDtos.Response.from(scored.movie())).toList();
    }
    @Transactional(readOnly = true) public DiscoveryDtos.HomeResponse home(String username, Long profileId) { Profile profile = profiles.resolve(username, profileId); return new DiscoveryDtos.HomeResponse(recommend(username, profile.getId(), 12), movies.findTrending(PageRequest.of(0, 12)).getContent().stream().map(MovieDtos.Response::from).toList(), continueWatching.findByProfile_IdOrderByUpdatedAtDesc(profile.getId()).stream().map(UserDataDtos.ContinueResponse::from).toList()); }
    private double score(Movie movie, Map<Long, Long> categoryCounts, List<Long> likedCategories, double maxViews) { double affinity = Math.min(1, categoryCounts.getOrDefault(movie.getCategory().getId(), 0L) / 5d); double rating = movie.getRating() == null ? 0 : movie.getRating().doubleValue() / 10d; double popularity = Math.log1p(movie.getViews() == null ? 0 : movie.getViews()) / maxViews; double liked = likedCategories.contains(movie.getCategory().getId()) ? 1 : 0; return affinity * 0.45 + rating * 0.25 + popularity * 0.15 + liked * 0.15; }
    private record Scored(Movie movie, double score) {}
}
