package com.cinevora.service;

import com.cinevora.entity.Movie;
import com.cinevora.entity.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.*;

class MediaTransactionTest {
    @AfterEach void clear() { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }

    @Test void replacementPreservesOldObjectUntilCommitAndRemovesNewObjectOnRollback() {
        for (int status : new int[]{TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK}) {
            var storage = mock(MediaStorageService.class);
            var movies = mock(MovieService.class);
            var movie = new Movie();
            var category = new Category(); category.setName("test");
            movie.setCategory(category); movie.setThumbnailUrl("https://media.test/old.png");
            when(movies.getEntity(1L)).thenReturn(movie);
            var file = new MockMultipartFile("file", new byte[]{1});
            when(storage.uploadPoster(file)).thenReturn(new MediaStorageService.StoredMedia("https://media.test/new.png", "new.png"));
            TransactionSynchronizationManager.initSynchronization();
            new MediaService(movies, storage).uploadPoster(1L, file);
            verify(storage, never()).deletePoster(anyString());
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(status));
            verify(storage).deletePoster(status == TransactionSynchronization.STATUS_COMMITTED ? "https://media.test/old.png" : "https://media.test/new.png");
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
