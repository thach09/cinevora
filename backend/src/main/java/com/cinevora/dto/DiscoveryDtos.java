package com.cinevora.dto;
import com.cinevora.entity.MoviePreference;
import com.cinevora.entity.PreferenceSignal;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
public final class DiscoveryDtos {
    private DiscoveryDtos() {}
    public record PreferenceRequest(@NotBlank String signal) {}
    public record PreferenceResponse(Long movieId, String signal) { public static PreferenceResponse from(MoviePreference preference) { return new PreferenceResponse(preference.getId().getMovieId(), preference.getSignal().name()); } }
    public record Suggestion(Long id, String title, String thumbnailUrl, Integer releaseYear) {}
    public record SearchEntry(Long id, String query, Instant searchedAt) {}
    public record PopularSearch(String query, long count) {}
    public record HomeResponse(List<MovieDtos.Response> topPicks, List<MovieDtos.Response> trending, List<UserDataDtos.ContinueResponse> continueWatching) {}
    public static PreferenceSignal signal(String value) { try { return PreferenceSignal.valueOf(value.trim().toUpperCase()); } catch (RuntimeException ex) { throw new com.cinevora.exception.BusinessException("Signal phải là LIKE hoặc DISLIKE"); } }
}
