package com.cinevora.service;

import com.cinevora.dto.UserDataDtos;
import com.cinevora.entity.*;
import com.cinevora.exception.*;
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

    public UserDataService(UserRepository users, MovieRepository movies, WatchlistRepository watchlist, FavouriteRepository favourites,
                           WatchHistoryRepository history, ContinueWatchingRepository continueWatching, MovieService movieService) {
        this.users = users; this.movies = movies; this.watchlist = watchlist; this.favourites = favourites; this.history = history; this.continueWatching = continueWatching; this.movieService = movieService;
    }
    @Transactional(readOnly = true) public List<UserDataDtos.MovieRef> getWatchlist(String username) { User u = current(username); return watchlist.findByUser_IdOrderByAddedAtDesc(u.getId()).stream().map(e -> UserDataDtos.MovieRef.from(e.getMovie().getId(), e.getMovie().getTitle(), e.getMovie().getThumbnailUrl(), e.getAddedAt())).toList(); }
    @Transactional public void addWatchlist(String username, Long movieId) { User u = current(username); Movie m = activeMovie(movieId); if (!watchlist.existsByUser_IdAndMovie_Id(u.getId(), movieId)) watchlist.save(new WatchlistEntry(u, m)); }
    @Transactional public void removeWatchlist(String username, Long movieId) { User u = current(username); WatchlistId id = new WatchlistId(u.getId(), movieId); if (!watchlist.existsById(id)) throw new ResourceNotFoundException("Phim không có trong watchlist"); watchlist.deleteById(id); }
    @Transactional(readOnly = true) public List<UserDataDtos.MovieRef> getFavourites(String username) { User u = current(username); return favourites.findByUser_IdOrderByAddedAtDesc(u.getId()).stream().map(e -> UserDataDtos.MovieRef.from(e.getMovie().getId(), e.getMovie().getTitle(), e.getMovie().getThumbnailUrl(), e.getAddedAt())).toList(); }
    @Transactional public void addFavourite(String username, Long movieId) { User u = current(username); Movie m = activeMovie(movieId); if (!favourites.existsByUser_IdAndMovie_Id(u.getId(), movieId)) { favourites.save(new FavouriteEntry(u, m)); movieService.incrementFavourites(m); } }
    @Transactional public void removeFavourite(String username, Long movieId) { User u = current(username); FavouriteId id = new FavouriteId(u.getId(), movieId); if (!favourites.existsById(id)) throw new ResourceNotFoundException("Phim không có trong favourites"); FavouriteEntry entry = favourites.findById(id).orElseThrow(); favourites.delete(entry); movieService.decrementFavourites(entry.getMovie()); }
    @Transactional(readOnly = true) public List<UserDataDtos.HistoryResponse> getHistory(String username) { return history.findByUser_IdOrderByWatchedAtDesc(current(username).getId()).stream().map(UserDataDtos.HistoryResponse::from).toList(); }
    @Transactional public void addHistory(String username, Long movieId) { User u = current(username); Movie m = activeMovie(movieId); history.save(new WatchHistory(u, m)); movieService.incrementViews(m); }
    @Transactional(readOnly = true) public byte[] exportHistory(String username) { StringBuilder csv = new StringBuilder("id,movie_id,title,watched_at\n"); for (UserDataDtos.HistoryResponse h : getHistory(username)) csv.append(h.id()).append(',').append(h.movieId()).append(',').append(csv(h.title())).append(',').append(h.watchedAt()).append('\n'); return csv.toString().getBytes(StandardCharsets.UTF_8); }
    @Transactional(readOnly = true) public List<UserDataDtos.ContinueResponse> getContinue(String username) { return continueWatching.findByUser_IdOrderByUpdatedAtDesc(current(username).getId()).stream().map(UserDataDtos.ContinueResponse::from).toList(); }
    @Transactional public void updateProgress(String username, UserDataDtos.ProgressRequest request) { User u = current(username); Movie m = activeMovie(request.movieId()); ContinueWatchingId id = new ContinueWatchingId(u.getId(), m.getId()); if (request.percent() == 100) { continueWatching.deleteById(id); addHistory(username, movieId(m)); return; } ContinueWatching row = continueWatching.findById(id).orElseGet(() -> new ContinueWatching(u, m, request.percent())); row.setPercent(request.percent()); continueWatching.save(row); }
    @Transactional public void removeContinue(String username, Long movieId) { User u = current(username); ContinueWatchingId id = new ContinueWatchingId(u.getId(), movieId); if (!continueWatching.existsById(id)) throw new ResourceNotFoundException("Phim không có trong danh sách đang xem dở"); continueWatching.deleteById(id); }

    private Long movieId(Movie movie) { return movie.getId(); }
    private User current(String username) { return users.findByUsernameIgnoreCase(username).filter(User::isActive).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản hiện tại")); }
    private Movie activeMovie(Long id) { return movies.findByIdAndActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + id)); }
    private String csv(String value) { if (value == null) return ""; return '"' + value.replace("\"", "\"\"") + '"'; }
}
