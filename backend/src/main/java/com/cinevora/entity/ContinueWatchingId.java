package com.cinevora.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContinueWatchingId implements Serializable {
    private Long userId;
    private Long movieId;
    public ContinueWatchingId() {}
    public ContinueWatchingId(Long userId, Long movieId) { this.userId = userId; this.movieId = movieId; }
    public Long getUserId() { return userId; }
    public Long getMovieId() { return movieId; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ContinueWatchingId i)) return false; return Objects.equals(userId, i.userId) && Objects.equals(movieId, i.movieId); }
    @Override public int hashCode() { return Objects.hash(userId, movieId); }
}
