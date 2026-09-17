package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "favourites")
public class FavouriteEntry {
    @EmbeddedId private FavouriteId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("movieId") @JoinColumn(name = "movie_id") private Movie movie;
    @Column(name = "added_at", nullable = false) private Instant addedAt;
    @PrePersist void onCreate() { if (addedAt == null) addedAt = Instant.now(); }
    public FavouriteEntry() {}
    public FavouriteEntry(User user, Movie movie) { this.user = user; this.movie = movie; this.id = new FavouriteId(user.getId(), movie.getId()); }
    public FavouriteId getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public Instant getAddedAt() { return addedAt; }
}
