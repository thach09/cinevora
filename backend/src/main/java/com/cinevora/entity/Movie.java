package com.cinevora.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id", nullable = false) private Category category;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, length = 100) private String director;
    @Column(nullable = false, columnDefinition = "TEXT") private String actors;
    @Column(name = "release_year", nullable = false) private Integer releaseYear;
    @Column(nullable = false, precision = 3, scale = 1) private BigDecimal rating = BigDecimal.ZERO;
    @Column(nullable = false) private Long views = 0L;
    @Column(name = "favourites_count", nullable = false) private Long favouritesCount = 0L;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(name = "video_url", length = 500) private String videoUrl;
    @Column(name = "thumbnail_url", length = 500) private String thumbnailUrl;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors; }
    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Long getViews() { return views; }
    public void setViews(Long views) { this.views = views; }
    public Long getFavouritesCount() { return favouritesCount; }
    public void setFavouritesCount(Long favouritesCount) { this.favouritesCount = favouritesCount; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
