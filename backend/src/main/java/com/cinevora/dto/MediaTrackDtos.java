package com.cinevora.dto;

import com.cinevora.entity.MovieTrack;
import com.cinevora.entity.TrackKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MediaTrackDtos {
    private MediaTrackDtos() {}
    public record Request(@NotBlank String kind, @NotBlank @Size(max = 12) String languageCode,
                          @NotBlank @Size(max = 80) String label, @NotBlank @Size(max = 500) String sourceUrl,
                          boolean defaultTrack) {}
    public record Response(Long id, Long movieId, String kind, String languageCode, String label,
                           String sourceUrl, boolean defaultTrack, boolean active) {
        public static Response from(MovieTrack track) { return new Response(track.getId(), track.getMovie().getId(), track.getKind().name(), track.getLanguageCode(), track.getLabel(), track.getSourceUrl(), track.isDefaultTrack(), track.isActive()); }
    }
    public static TrackKind kind(String value) { try { return TrackKind.valueOf(value.trim().toUpperCase()); } catch (RuntimeException ex) { throw new com.cinevora.exception.BusinessException("Track kind phải là SUBTITLE hoặc AUDIO"); } }
}
