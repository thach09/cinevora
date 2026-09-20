package com.cinevora.service;

import com.cinevora.dto.MovieDtos;
import com.cinevora.entity.Movie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    private final MovieService movies;
    private final MediaStorageService storage;

    public MediaService(MovieService movies, MediaStorageService storage) {
        this.movies = movies;
        this.storage = storage;
    }

    @Transactional
    public MovieDtos.Response uploadPoster(Long movieId, MultipartFile file) {
        Movie movie = movies.getEntity(movieId);
        String previous = movie.getThumbnailUrl();
        MediaStorageService.StoredMedia stored = storage.uploadPoster(file);
        try {
            movie.setThumbnailUrl(stored.url());
            MovieDtos.Response response = MovieDtos.Response.from(movie);
            storage.deletePoster(previous);
            return response;
        } catch (RuntimeException ex) {
            storage.deletePoster(stored.url());
            throw ex;
        }
    }

    @Transactional
    public MovieDtos.Response removePoster(Long movieId) {
        Movie movie = movies.getEntity(movieId);
        String previous = movie.getThumbnailUrl();
        movie.setThumbnailUrl(null);
        storage.deletePoster(previous);
        return MovieDtos.Response.from(movie);
    }
}
