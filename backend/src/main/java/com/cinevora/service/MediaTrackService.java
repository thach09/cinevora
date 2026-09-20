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
import java.util.List;

@Service
public class MediaTrackService {
    private final MovieRepository movies;
    private final MovieTrackRepository tracks;
    public MediaTrackService(MovieRepository movies, MovieTrackRepository tracks) { this.movies = movies; this.tracks = tracks; }
    @Transactional(readOnly = true) public List<MediaTrackDtos.Response> publicList(Long movieId) { return tracks.findByMovie_IdAndActiveTrueOrderByKindAscDefaultTrackDescLabelAsc(movieId).stream().map(MediaTrackDtos.Response::from).toList(); }
    @Transactional(readOnly = true) public List<MediaTrackDtos.Response> adminList(Long movieId) { requireMovie(movieId); return tracks.findByMovie_IdOrderByKindAscActiveDescLabelAsc(movieId).stream().map(MediaTrackDtos.Response::from).toList(); }
    @Transactional public MediaTrackDtos.Response create(Long movieId, MediaTrackDtos.Request request) { Movie movie = requireMovie(movieId); TrackKind kind = MediaTrackDtos.kind(request.kind()); MovieTrack track = new MovieTrack(movie, kind, request.languageCode().trim().toLowerCase(), request.label().trim(), request.sourceUrl().trim(), request.defaultTrack()); return MediaTrackDtos.Response.from(tracks.save(track)); }
    @Transactional public void archive(Long movieId, Long trackId) { tracks.findByIdAndMovie_Id(trackId, movieId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media track " + trackId)).setActive(false); }
    private Movie requireMovie(Long movieId) { return movies.findById(movieId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + movieId)); }
}
