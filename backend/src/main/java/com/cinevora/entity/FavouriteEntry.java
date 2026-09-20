package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "favourites")
public class FavouriteEntry {
    @EmbeddedId private FavouriteId id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("profileId") @JoinColumn(name = "profile_id", nullable = false) private Profile profile;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("movieId") @JoinColumn(name = "movie_id") private Movie movie;
    @Column(name = "added_at", nullable = false) private Instant addedAt;
    @PrePersist void onCreate() { if (addedAt == null) addedAt = Instant.now(); }
    public FavouriteEntry() {}
    public FavouriteEntry(User user, Profile profile, Movie movie) { this.user = user; this.profile = profile; this.movie = movie; this.id = new FavouriteId(profile.getId(), movie.getId()); }
    public FavouriteId getId() { return id; }
    public User getUser() { return user; }
    public Profile getProfile() { return profile; }
    public Movie getMovie() { return movie; }
    public Instant getAddedAt() { return addedAt; }
}
