package com.cinevora.service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {
    StoredMedia uploadPoster(MultipartFile file);
    void deletePoster(String mediaUrl);

    record StoredMedia(String url, String filename) {}
}
