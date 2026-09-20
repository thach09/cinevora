package com.cinevora.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MoviePreferenceId implements Serializable {
    private Long profileId;
    private Long movieId;
    public MoviePreferenceId() {}
    public MoviePreferenceId(Long profileId, Long movieId) { this.profileId = profileId; this.movieId = movieId; }
    public Long getProfileId() { return profileId; }
    public Long getMovieId() { return movieId; }
    @Override public boolean equals(Object other) { if (this == other) return true; if (!(other instanceof MoviePreferenceId id)) return false; return Objects.equals(profileId, id.profileId) && Objects.equals(movieId, id.movieId); }
    @Override public int hashCode() { return Objects.hash(profileId, movieId); }
}
