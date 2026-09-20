package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "watchlist")
public class WatchlistEntry {
    @EmbeddedId private WatchlistId id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("profileId") @JoinColumn(name = "profile_id", nullable = false) private Profile profile;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("movieId") @JoinColumn(name = "movie_id") private Movie movie;
    @Column(name = "added_at", nullable = false) private Instant addedAt;
    @PrePersist void onCreate() { if (addedAt == null) addedAt = Instant.now(); }
    public WatchlistEntry() {}
    public WatchlistEntry(User user, Profile profile, Movie movie) { this.user = user; this.profile = profile; this.movie = movie; this.id = new WatchlistId(profile.getId(), movie.getId()); }
    public WatchlistId getId() { return id; }
    public User getUser() { return user; }
    public Profile getProfile() { return profile; }
    public Movie getMovie() { return movie; }
    public Instant getAddedAt() { return addedAt; }
}
