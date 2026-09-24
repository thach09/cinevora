package com.cinevora.service;

import com.cinevora.dto.UserDataDtos;
import com.cinevora.entity.*;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserDataService {
    private final UserRepository users;
    private final MovieRepository movies;
    private final WatchlistRepository watchlist;
    private final FavouriteRepository favourites;
    private final WatchHistoryRepository history;
    private final ContinueWatchingRepository continueWatching;
    private final MovieService movieService;
    private final WatchProgressPolicy progressPolicy;
    private final ProfileService profiles;

    public UserDataService(UserRepository users, MovieRepository movies, WatchlistRepository watchlist, FavouriteRepository favourites,
                           WatchHistoryRepository history, ContinueWatchingRepository continueWatching, MovieService movieService,
                           WatchProgressPolicy progressPolicy, ProfileService profiles) {
        this.users = users; this.movies = movies; this.watchlist = watchlist; this.favourites = favourites; this.history = history; this.continueWatching = continueWatching; this.movieService = movieService; this.progressPolicy = progressPolicy; this.profiles = profiles;
    }
    @Transactional(readOnly = true) public List<UserDataDtos.MovieRef> getWatchlist(String username, Long profileId) { Profile p = profile(username, profileId); return watchlist.findByProfile_IdOrderByAddedAtDesc(p.getId()).stream().map(e -> UserDataDtos.MovieRef.from(e.getMovie().getId(), e.getMovie().getTitle(), e.getMovie().getThumbnailUrl(), e.getAddedAt())).toList(); }
    @Transactional public void addWatchlist(String username, Long profileId, Long movieId) { User u = current(username); Profile p = profile(username, profileId); Movie m = activeMovie(movieId); if (!watchlist.existsByProfile_IdAndMovie_Id(p.getId(), movieId)) watchlist.save(new WatchlistEntry(u, p, m)); }
    @Transactional public void removeWatchlist(String username, Long profileId, Long movieId) { Profile p = profile(username, profileId); WatchlistId id = new WatchlistId(p.getId(), movieId); if (!watchlist.existsById(id)) throw new ResourceNotFoundException("Phim không có trong watchlist"); watchlist.deleteById(id); }
    @Transactional(readOnly = true) public List<UserDataDtos.MovieRef> getFavourites(String username, Long profileId) { Profile p = profile(username, profileId); return favourites.findByProfile_IdOrderByAddedAtDesc(p.getId()).stream().map(e -> UserDataDtos.MovieRef.from(e.getMovie().getId(), e.getMovie().getTitle(), e.getMovie().getThumbnailUrl(), e.getAddedAt())).toList(); }
    @Transactional public void addFavourite(String username, Long profileId, Long movieId) { User u = current(username); Profile p = profile(username, profileId); Movie m = activeMovie(movieId); if (!favourites.existsByProfile_IdAndMovie_Id(p.getId(), movieId)) { favourites.save(new FavouriteEntry(u, p, m)); movieService.incrementFavourites(m); } }
    @Transactional public void removeFavourite(String username, Long profileId, Long movieId) { Profile p = profile(username, profileId); FavouriteId id = new FavouriteId(p.getId(), movieId); if (!favourites.existsById(id)) throw new ResourceNotFoundException("Phim không có trong favourites"); FavouriteEntry entry = favourites.findById(id).orElseThrow(); favourites.delete(entry); movieService.decrementFavourites(entry.getMovie()); }
    @Transactional(readOnly = true) public List<UserDataDtos.HistoryResponse> getHistory(String username, Long profileId) { return history.findByProfile_IdOrderByWatchedAtDesc(profile(username, profileId).getId()).stream().map(UserDataDtos.HistoryResponse::from).toList(); }
    @Transactional public void addHistory(String username, Long profileId, Long movieId) { User u = current(username); Profile p = profile(username, profileId); Movie m = activeMovie(movieId); if (!history.existsByProfile_IdAndMovie_Id(p.getId(), movieId)) { history.save(new WatchHistory(u, p, m)); movieService.incrementViews(m); } }
    @Transactional(readOnly = true) public byte[] exportHistory(String username, Long profileId) { StringBuilder csv = new StringBuilder("id,movie_id,title,watched_at\n"); for (UserDataDtos.HistoryResponse h : getHistory(username, profileId)) csv.append(h.id()).append(',').append(h.movieId()).append(',').append(csv(h.title())).append(',').append(h.watchedAt()).append('\n'); return csv.toString().getBytes(StandardCharsets.UTF_8); }
    @Transactional(readOnly = true) public List<UserDataDtos.ContinueResponse> getContinue(String username, Long profileId) { return continueWatching.findByProfile_IdOrderByUpdatedAtDesc(profile(username, profileId).getId()).stream().map(UserDataDtos.ContinueResponse::from).toList(); }
    @Transactional public void updateProgress(String username, Long profileId, UserDataDtos.ProgressRequest request) {
        User u = current(username); Profile p = profile(username, profileId); Movie m = activeMovie(request.movieId());
        int fallbackPercent = progressPolicy.normalizePercent(request.percent());
        int duration = progressPolicy.resolveDuration(request.durationSeconds(), m.getDurationMinutes());
        int position = progressPolicy.resolvePosition(request.positionSeconds(), duration, fallbackPercent);
        int percent = progressPolicy.derivePercent(position, duration, fallbackPercent);
        ContinueWatchingId id = new ContinueWatchingId(p.getId(), m.getId());
        if (progressPolicy.isComplete(percent, position, duration)) { continueWatching.deleteById(id); addHistory(username, p.getId(), m.getId()); return; }
        if (!progressPolicy.shouldContinue(percent, position, duration)) { continueWatching.deleteById(id); return; }
        ContinueWatching row = continueWatching.findById(id).orElseGet(() -> new ContinueWatching(u, p, m, percent, position, duration == 0 ? null : duration));
        row.setPercent(percent); row.setPositionSeconds(position); row.setDurationSeconds(duration == 0 ? null : duration); continueWatching.save(row);
    }
    @Transactional public void removeContinue(String username, Long profileId, Long movieId) { Profile p = profile(username, profileId); ContinueWatchingId id = new ContinueWatchingId(p.getId(), movieId); if (!continueWatching.existsById(id)) throw new ResourceNotFoundException("Phim không có trong danh sách đang xem dở"); continueWatching.deleteById(id); }
    private User current(String username) { return users.findByUsernameIgnoreCase(username).filter(User::isActive).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản hiện tại")); }
    private Profile profile(String username, Long profileId) { return profiles.resolve(username, profileId); }
    private Movie activeMovie(Long id) { return movies.findByIdAndActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + id)); }
    private String csv(String value) {
        if (value == null) return "";
        String safe = value;
        if (!safe.isEmpty() && ("=+-@".indexOf(safe.stripLeading().isEmpty() ? ' ' : safe.stripLeading().charAt(0)) >= 0 || safe.charAt(0) < 32)) safe = "'" + safe;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
