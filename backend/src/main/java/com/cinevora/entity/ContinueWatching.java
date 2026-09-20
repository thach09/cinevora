package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "continue_watching")
public class ContinueWatching {
    @EmbeddedId private ContinueWatchingId id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("profileId") @JoinColumn(name = "profile_id", nullable = false) private Profile profile;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("movieId") @JoinColumn(name = "movie_id") private Movie movie;
    @Column(nullable = false) private Integer percent;
    @Column(name = "position_seconds", nullable = false) private Integer positionSeconds = 0;
    @Column(name = "duration_seconds") private Integer durationSeconds;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public ContinueWatching() {}
    public ContinueWatching(User user, Profile profile, Movie movie, Integer percent) {
        this(user, profile, movie, percent, null, null);
    }
    public ContinueWatching(User user, Profile profile, Movie movie, Integer percent, Integer positionSeconds, Integer durationSeconds) {
        this.user = user; this.profile = profile;
        this.movie = movie;
        this.percent = percent;
        this.positionSeconds = positionSeconds == null ? 0 : positionSeconds;
        this.durationSeconds = durationSeconds;
        this.id = new ContinueWatchingId(profile.getId(), movie.getId());
    }
    public ContinueWatchingId getId() { return id; }
    public User getUser() { return user; }
    public Profile getProfile() { return profile; }
    public Movie getMovie() { return movie; }
    public Integer getPercent() { return percent; }
    public void setPercent(Integer percent) { this.percent = percent; }
    public Integer getPositionSeconds() { return positionSeconds; }
    public void setPositionSeconds(Integer positionSeconds) { this.positionSeconds = positionSeconds; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public Instant getUpdatedAt() { return updatedAt; }
}
