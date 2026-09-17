package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "continue_watching")
public class ContinueWatching {
    @EmbeddedId private ContinueWatchingId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("movieId") @JoinColumn(name = "movie_id") private Movie movie;
    @Column(nullable = false) private Integer percent;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public ContinueWatching() {}
    public ContinueWatching(User user, Movie movie, Integer percent) { this.user = user; this.movie = movie; this.percent = percent; this.id = new ContinueWatchingId(user.getId(), movie.getId()); }
    public ContinueWatchingId getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public Integer getPercent() { return percent; }
    public void setPercent(Integer percent) { this.percent = percent; }
    public Instant getUpdatedAt() { return updatedAt; }
}
