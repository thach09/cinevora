package com.cinevora.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_history")
public class SearchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "profile_id") private Profile profile;
    @Column(name = "query_text", nullable = false, length = 120) private String query;
    @Column(name = "searched_at", nullable = false) private Instant searchedAt;
    protected SearchHistory() {}
    public SearchHistory(Profile profile, String query) { this.profile = profile; this.query = query; }
    @PrePersist void onCreate() { searchedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getQuery() { return query; }
    public Instant getSearchedAt() { return searchedAt; }
}
