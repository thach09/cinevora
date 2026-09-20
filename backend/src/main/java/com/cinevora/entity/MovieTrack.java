package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movie_tracks")
public class MovieTrack {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "movie_id", nullable = false) private Movie movie;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private TrackKind kind;
    @Column(name = "language_code", nullable = false, length = 12) private String languageCode;
    @Column(nullable = false, length = 80) private String label;
    @Column(name = "source_url", nullable = false, length = 500) private String sourceUrl;
    @Column(name = "is_default", nullable = false) private boolean defaultTrack;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected MovieTrack() {}
    public MovieTrack(Movie movie, TrackKind kind, String languageCode, String label, String sourceUrl, boolean defaultTrack) { this.movie = movie; this.kind = kind; this.languageCode = languageCode; this.label = label; this.sourceUrl = sourceUrl; this.defaultTrack = defaultTrack; }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Movie getMovie() { return movie; }
    public TrackKind getKind() { return kind; }
    public String getLanguageCode() { return languageCode; }
    public String getLabel() { return label; }
    public String getSourceUrl() { return sourceUrl; }
    public boolean isDefaultTrack() { return defaultTrack; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
