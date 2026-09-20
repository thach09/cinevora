package com.cinevora.service;

import com.cinevora.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalMediaStorageService implements MediaStorageService {
    private static final long DEFAULT_MAX_SIZE = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private final Path root;
    private final long maxSize;

    public LocalMediaStorageService(
            @Value("${app.media.storage-root:uploads/media}") String storageRoot,
            @Value("${app.media.poster.max-size-bytes:5242880}") long maxSize) {
        this.root = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.maxSize = maxSize > 0 ? maxSize : DEFAULT_MAX_SIZE;
    }

    @PostConstruct
    void createDirectories() {
        try {
            Files.createDirectories(root.resolve("posters"));
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể khởi tạo media storage", ex);
        }
    }

    @Override
    public StoredMedia uploadPoster(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("Poster không được để trống");
        if (file.getSize() > maxSize) throw new BusinessException("Poster vượt quá kích thước cho phép");

        DetectedImage detected = detectImage(file);
        String filename = UUID.randomUUID() + detected.extension();
        Path target = root.resolve("posters").resolve(filename).normalize();
        if (!target.startsWith(root.resolve("posters"))) throw new BusinessException("Tên file không hợp lệ");
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredMedia("/media/posters/" + filename, filename);
        } catch (IOException ex) {
            throw new BusinessException("Không thể lưu poster");
        }
    }

    @Override
    public void deletePoster(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.startsWith("/media/posters/")) return;
        String filename = mediaUrl.substring("/media/posters/".length());
        Path posterRoot = root.resolve("posters").normalize();
        Path target = posterRoot.resolve(filename).normalize();
        if (!target.startsWith(posterRoot) || filename.contains("/")) return;
        try { Files.deleteIfExists(target); } catch (IOException ignored) { /* cleanup is best effort */ }
    }

    private DetectedImage detectImage(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            DetectedImage detected = detectSignature(header);
            if (detected == null) throw new BusinessException("Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WEBP hợp lệ");
            if (detected.requiresDecode()) {
                try (InputStream imageInput = file.getInputStream()) {
                    BufferedImage image = ImageIO.read(imageInput);
                    if (image == null || image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION)
                        throw new BusinessException("Poster không phải ảnh hợp lệ hoặc vượt giới hạn kích thước");
                }
            }
            return detected;
        } catch (IOException ex) {
            throw new BusinessException("Không thể đọc poster");
        }
    }

    private DetectedImage detectSignature(byte[] header) {
        if (header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff)
            return new DetectedImage(".jpg", true);
        if (header.length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G')
            return new DetectedImage(".png", true);
        if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P')
            return new DetectedImage(".webp", false);
        return null;
    }

    private record DetectedImage(String extension, boolean requiresDecode) {}
}
