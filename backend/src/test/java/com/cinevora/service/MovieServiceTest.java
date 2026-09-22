package com.cinevora.service;

import com.cinevora.dto.MovieDtos;
import com.cinevora.entity.Category;
import com.cinevora.entity.Movie;
import com.cinevora.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {
    @Mock MovieRepository movies;
    @Mock CategoryService categoryService;
    @InjectMocks MovieService service;

    @Test
    void createStoresWatchNowAndTrailerSourcesSeparately() {
        Category category = new Category();
        when(categoryService.get(2L)).thenReturn(category);
        when(movies.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieDtos.Request request = new MovieDtos.Request(
                "  Demo movie  ", 2L, "Director", "Actor", 2024,
                new BigDecimal("8.2"), 120,
                "https://cdn.example/watch.mp4",
                "https://www.youtube.com/watch?v=JfVOs4VSpmA",
                "https://cdn.example/poster.jpg", "Description");

        MovieDtos.Response response = service.create(request);

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(movies).save(captor.capture());
        Movie saved = captor.getValue();
        assertEquals("https://cdn.example/watch.mp4", saved.getVideoUrl());
        assertEquals("https://www.youtube.com/watch?v=JfVOs4VSpmA", saved.getTrailerUrl());
        assertEquals("https://www.youtube.com/watch?v=JfVOs4VSpmA", response.trailerUrl());
    }
}
