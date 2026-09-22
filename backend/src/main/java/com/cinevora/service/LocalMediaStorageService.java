package com.cinevora.service;

import com.cinevora.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.media.storage", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorageService implements MediaStorageService {
    private final Path posterRoot;
    private final PosterValidator validator;

    public LocalMediaStorageService(@Value("${app.media.storage-root:uploads/media}") String storageRoot, PosterValidator validator) {
        this.posterRoot = Paths.get(storageRoot).toAbsolutePath().normalize().resolve("posters");
        this.validator = validator;
    }

    @PostConstruct
    void createDirectories() {
        try { Files.createDirectories(posterRoot); }
        catch (IOException ex) { throw new IllegalStateException("Cannot initialize media storage", ex); }
    }

    @Override
    public StoredMedia uploadPoster(MultipartFile file) {
        var poster = validator.validate(file);
        String filename = UUID.randomUUID() + poster.extension();
        try {
            Files.write(posterRoot.resolve(filename), poster.bytes(), StandardOpenOption.CREATE_NEW);
            return new StoredMedia("/media/posters/" + filename, filename);
        } catch (IOException ex) { throw new BusinessException("Cannot save poster"); }
    }

    @Override
    public void deletePoster(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.startsWith("/media/posters/")) return;
        String filename = mediaUrl.substring("/media/posters/".length());
        if (!filename.matches("[a-f0-9-]{36}\\.(jpg|png|webp)")) return;
        try { Files.deleteIfExists(posterRoot.resolve(filename)); }
        catch (IOException ignored) { /* cleanup is best effort */ }
    }
}
