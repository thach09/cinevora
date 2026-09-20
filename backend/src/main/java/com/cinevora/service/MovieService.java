package com.cinevora.service;

import com.cinevora.common.PageResponse;
import com.cinevora.dto.MovieDtos;
import com.cinevora.entity.*;
import com.cinevora.exception.*;
import com.cinevora.repository.MovieRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Year;
import java.util.Locale;
import java.util.List;

@Service
public class MovieService {
    private final MovieRepository movies;
    private final CategoryService categoryService;
    public MovieService(MovieRepository movies, CategoryService categoryService) { this.movies = movies; this.categoryService = categoryService; }

    @Transactional(readOnly = true)
    public PageResponse<MovieDtos.Response> search(String q, Long categoryId, Integer minYear, Integer maxYear, BigDecimal minRating, int page, int size, String sort, String direction) {
        if (page < 0 || size < 1 || size > 50) throw new BusinessException("Tham số phân trang không hợp lệ");
        String property = switch (sort == null ? "popularity" : sort.toLowerCase(Locale.ROOT)) {
            case "title" -> "title"; case "rating" -> "rating"; case "year", "releaseyear" -> "releaseYear"; case "views", "popularity" -> "views"; default -> throw new BusinessException("Trường sort không được hỗ trợ");
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, property));
        // Keep the optional text parameter typed as String when it reaches PostgreSQL.
        // Binding null in the JPQL `:q is null` branch can be inferred as bytea by
        // PostgreSQL, which makes lower(:q) fail at runtime. An empty query means
        // "no text filter" and avoids that driver/type inference issue.
        String cleanQuery = q == null || q.isBlank() ? "" : q.trim();
        return PageResponse.from(movies.searchActive(cleanQuery, categoryId, minYear, maxYear, minRating, pageable).map(MovieDtos.Response::from));
    }
    @Transactional(readOnly = true) public MovieDtos.Response getActive(Long id) { return MovieDtos.Response.from(movies.findByIdAndActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + id))); }
    @Transactional(readOnly = true) public Movie getEntity(Long id) { return movies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + id)); }
    @Transactional public MovieDtos.Response create(MovieDtos.Request request) { Movie m = new Movie(); apply(m, request); return MovieDtos.Response.from(movies.save(m)); }
    @Transactional public MovieDtos.Response update(Long id, MovieDtos.Request request) { Movie m = getEntity(id); apply(m, request); return MovieDtos.Response.from(m); }
    @Transactional public void delete(Long id) { getEntity(id).setActive(false); }
    @Transactional public MovieDtos.Response restore(Long id) { Movie m = getEntity(id); m.setActive(true); return MovieDtos.Response.from(m); }
    @Transactional(readOnly = true) public PageResponse<MovieDtos.Response> trending(int page, int size) { return PageResponse.from(movies.findTrending(PageRequest.of(page, Math.min(size, 50))).map(MovieDtos.Response::from)); }
    @Transactional(readOnly = true) public List<com.cinevora.dto.DiscoveryDtos.Suggestion> suggestions(String q, int limit) {
        if (q == null || q.trim().length() < 2) return List.of();
        int take = Math.max(1, Math.min(limit, 6));
        return movies.searchActive(q.trim(), null, null, null, null, PageRequest.of(0, take, Sort.by(Sort.Direction.ASC, "title"))).getContent().stream().map(m -> new com.cinevora.dto.DiscoveryDtos.Suggestion(m.getId(), m.getTitle(), m.getThumbnailUrl(), m.getReleaseYear())).toList();
    }
    @Transactional public void incrementViews(Movie movie) { movie.setViews(movie.getViews() + 1); }
    @Transactional public void incrementFavourites(Movie movie) { movie.setFavouritesCount(movie.getFavouritesCount() + 1); }
    @Transactional public void decrementFavourites(Movie movie) { movie.setFavouritesCount(Math.max(0, movie.getFavouritesCount() - 1)); }

    private void apply(Movie m, MovieDtos.Request r) {
        if (r.releaseYear() > Year.now().getValue()) throw new BusinessException("Năm phát hành không được lớn hơn năm hiện tại");
        Category category = categoryService.get(r.categoryId());
        if (!category.isActive()) throw new BusinessException("Không thể gắn phim vào thể loại đã bị vô hiệu hóa");
        m.setCategory(category); m.setTitle(r.title().trim()); m.setDirector(r.director().trim()); m.setActors(r.actors().trim()); m.setReleaseYear(r.releaseYear());
        m.setRating(r.rating() == null ? BigDecimal.ZERO : r.rating()); m.setDurationMinutes(r.durationMinutes()); m.setVideoUrl(clean(r.videoUrl())); m.setThumbnailUrl(clean(r.thumbnailUrl())); m.setDescription(clean(r.description()));
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
