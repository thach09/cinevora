package com.cinevora.dto;

import com.cinevora.entity.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class UserDataDtos {
    private UserDataDtos() {}
    public record MovieRef(Long movieId, String title, String thumbnailUrl, Instant addedAt) {
        public static MovieRef from(Long id, String title, String thumbnail, Instant at) { return new MovieRef(id, title, thumbnail, at); }
    }
    public record HistoryResponse(Long id, Long movieId, String title, Instant watchedAt) {
        public static HistoryResponse from(WatchHistory h) { return new HistoryResponse(h.getId(), h.getMovie().getId(), h.getMovie().getTitle(), h.getWatchedAt()); }
    }
    public record ContinueResponse(Long movieId, String title, String thumbnailUrl, Integer percent, Integer positionSeconds, Integer durationSeconds, Instant updatedAt) {
        public static ContinueResponse from(ContinueWatching c) {
            int position = c.getPositionSeconds() == null ? 0 : c.getPositionSeconds();
            int duration = c.getDurationSeconds() == null ? 0 : c.getDurationSeconds();
            int derivedPercent = duration > 0 ? Math.max(0, Math.min(100, Math.round((position * 100f) / duration))) : c.getPercent();
            return new ContinueResponse(c.getMovie().getId(), c.getMovie().getTitle(), c.getMovie().getThumbnailUrl(), derivedPercent, position, duration == 0 ? null : duration, c.getUpdatedAt());
        }
    }
    public record ProgressRequest(@NotNull Long movieId, @Min(0) @Max(100) Integer percent,
                                  @Min(0) Integer positionSeconds, @Min(1) Integer durationSeconds) {}
}
