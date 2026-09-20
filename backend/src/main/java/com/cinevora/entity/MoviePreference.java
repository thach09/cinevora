package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movie_preferences")
public class MoviePreference {
    @EmbeddedId private MoviePreferenceId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("profileId") @JoinColumn(name = "profile_id") private Profile profile;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("movieId") @JoinColumn(name = "movie_id") private Movie movie;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private PreferenceSignal signal;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected MoviePreference() {}
    public MoviePreference(Profile profile, Movie movie, PreferenceSignal signal) { this.profile = profile; this.movie = movie; this.signal = signal; this.id = new MoviePreferenceId(profile.getId(), movie.getId()); }
    @PrePersist void onCreate() { updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public MoviePreferenceId getId() { return id; }
    public Movie getMovie() { return movie; }
    public PreferenceSignal getSignal() { return signal; }
    public void setSignal(PreferenceSignal signal) { this.signal = signal; }
}
