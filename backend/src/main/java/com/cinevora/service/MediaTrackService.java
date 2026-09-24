package com.cinevora.service;

import com.cinevora.dto.MediaTrackDtos;
import com.cinevora.entity.Movie;
import com.cinevora.entity.MovieTrack;
import com.cinevora.entity.TrackKind;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.MovieRepository;
import com.cinevora.repository.MovieTrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class MediaTrackService {
    private final MovieRepository movies;
    private final MovieTrackRepository tracks;
    public MediaTrackService(MovieRepository movies, MovieTrackRepository tracks) { this.movies = movies; this.tracks = tracks; }
    @Transactional(readOnly = true) public List<MediaTrackDtos.Response> publicList(Long movieId) {
        movies.findByIdAndActiveTrue(movieId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + movieId));
        return tracks.findByMovie_IdAndActiveTrueOrderByKindAscDefaultTrackDescLabelAsc(movieId).stream().map(MediaTrackDtos.Response::from).toList();
    }
    @Transactional(readOnly = true) public List<MediaTrackDtos.Response> adminList(Long movieId) { requireMovie(movieId); return tracks.findByMovie_IdOrderByKindAscActiveDescLabelAsc(movieId).stream().map(MediaTrackDtos.Response::from).toList(); }
    @Transactional public MediaTrackDtos.Response create(Long movieId, MediaTrackDtos.Request request) {
        Movie movie = requireMovie(movieId);
        TrackKind kind = MediaTrackDtos.kind(request.kind());
        String sourceUrl = safeSourceUrl(request.sourceUrl());
        MovieTrack track = new MovieTrack(movie, kind, request.languageCode().trim().toLowerCase(), request.label().trim(), sourceUrl, request.defaultTrack());
        return MediaTrackDtos.Response.from(tracks.save(track));
    }
    @Transactional public void archive(Long movieId, Long trackId) { tracks.findByIdAndMovie_Id(trackId, movieId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media track " + trackId)).setActive(false); }
    private Movie requireMovie(Long movieId) { return movies.findById(movieId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + movieId)); }
    private String safeSourceUrl(String sourceUrl) {
        String value = sourceUrl == null ? "" : sourceUrl.trim();
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost() == null || value.length() > 500)
                throw new IllegalArgumentException("Track source URL must be an HTTP(S) URL");
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Track source URL must be an HTTP(S) URL", ex);
        }
        return value;
    }
}
