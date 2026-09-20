package com.cinevora.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContinueWatchingId implements Serializable {
    private Long profileId;
    private Long movieId;
    public ContinueWatchingId() {}
    public ContinueWatchingId(Long profileId, Long movieId) { this.profileId = profileId; this.movieId = movieId; }
    public Long getProfileId() { return profileId; }
    public Long getMovieId() { return movieId; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ContinueWatchingId i)) return false; return Objects.equals(profileId, i.profileId) && Objects.equals(movieId, i.movieId); }
    @Override public int hashCode() { return Objects.hash(profileId, movieId); }
}
