package com.cinevora.service;

import com.cinevora.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.media.storage", havingValue = "s3")
public class ObjectStorageMediaStorageService implements MediaStorageService {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageMediaStorageService.class);
    private final S3Client s3;
    private final PosterValidator validator;
    private final String bucket;
    private final String publicBase;

    public ObjectStorageMediaStorageService(S3Client s3, PosterValidator validator,
            @Value("${app.media.s3.bucket}") String bucket,
            @Value("${app.media.s3.public-base-url}") String publicBase) {
        this.s3 = s3;
        this.validator = validator;
        this.bucket = bucket;
        URI uri = URI.create(publicBase);
        if (bucket.isBlank() || uri.getHost() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null)
            throw new IllegalStateException("Invalid object storage bucket/public URL configuration");
        this.publicBase = publicBase.replaceAll("/+$", "");
    }

    @Override
    public StoredMedia uploadPoster(MultipartFile file) {
        var poster = validator.validate(file);
        String filename = UUID.randomUUID() + poster.extension();
        String key = "posters/" + filename;
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(poster.contentType()).cacheControl("public, max-age=31536000, immutable").build(),
                    RequestBody.fromBytes(poster.bytes()));
            return new StoredMedia(publicBase + "/" + key, filename);
        } catch (SdkException ex) {
            log.warn("Object storage poster upload failed ({})", ex.getClass().getSimpleName());
            throw new BusinessException("Không thể lưu poster; vui lòng thử lại");
        }
    }

    @Override
    public void deletePoster(String mediaUrl) {
        String prefix = publicBase + "/posters/";
        if (mediaUrl == null || !mediaUrl.startsWith(prefix)) return;
        String filename = mediaUrl.substring(prefix.length());
        if (!filename.matches("[a-f0-9-]{36}\\.(jpg|png|webp)")) return;
        try { s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key("posters/" + filename).build()); }
        catch (SdkException ex) { log.warn("Object storage poster cleanup failed ({})", ex.getClass().getSimpleName()); }
    }
}
