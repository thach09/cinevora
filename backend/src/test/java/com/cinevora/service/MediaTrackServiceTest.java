package com.cinevora.service;

import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.MovieRepository;
import com.cinevora.repository.MovieTrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaTrackServiceTest {
    @Mock MovieRepository movies;
    @Mock MovieTrackRepository tracks;
    @InjectMocks MediaTrackService service;

    @Test
    void publicTracksRejectArchivedMovie() {
        when(movies.findByIdAndActiveTrue(61L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.publicList(61L));
        verifyNoInteractions(tracks);
    }
}
