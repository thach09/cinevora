package com.cinevora.dto;

import com.cinevora.entity.Movie;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class MovieDtos {
    private MovieDtos() {}
    public record Request(@NotBlank @Size(max = 255) String title,
                          @NotNull Long categoryId,
                          @NotBlank @Size(max = 100) String director,
                          @NotBlank String actors,
                          @NotNull @Min(1888) Integer releaseYear,
                          @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal rating,
                          @Positive Integer durationMinutes,
                          @Size(max = 500) String videoUrl,
                          @Size(max = 500) String trailerUrl,
                          @Size(max = 500) String thumbnailUrl,
                          String description) {}
    public record Response(Long id, Long categoryId, String categoryName, String title, String director,
                           String actors, Integer releaseYear, BigDecimal rating, Long views, Long favouritesCount,
                           Integer durationMinutes, String videoUrl, String trailerUrl, String thumbnailUrl,
                           String description, boolean active) {
        public static Response from(Movie m) {
            return new Response(m.getId(), m.getCategory().getId(), m.getCategory().getName(), m.getTitle(), m.getDirector(),
                    m.getActors(), m.getReleaseYear(), m.getRating(), m.getViews(), m.getFavouritesCount(),
                    m.getDurationMinutes(), m.getVideoUrl(), m.getTrailerUrl(), m.getThumbnailUrl(),
                    m.getDescription(), m.isActive());
        }
    }
}
