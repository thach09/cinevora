package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "watch_history")
public class WatchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "movie_id") private Movie movie;
    @Column(name = "watched_at", nullable = false) private Instant watchedAt;
    @PrePersist void onCreate() { if (watchedAt == null) watchedAt = Instant.now(); }
    public WatchHistory() {}
    public WatchHistory(User user, Movie movie) { this.user = user; this.movie = movie; }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public Instant getWatchedAt() { return watchedAt; }
}
