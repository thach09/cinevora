package com.cinevora.service;

import com.cinevora.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Component
public class PosterValidator {
    private static final Map<String, String> TYPES = Map.of("JPEG", "image/jpeg", "PNG", "image/png", "WEBP", "image/webp");
    private final long maxSize;

    public PosterValidator(@Value("${app.media.poster.max-size-bytes:5242880}") long maxSize) {
        if (maxSize <= 0 || maxSize > 5L * 1024 * 1024) throw new IllegalArgumentException("Poster limit must be within 1 byte to 5 MB");
        this.maxSize = maxSize;
    }

    public ValidatedPoster validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("Poster không được để trống");
        if (file.getSize() > maxSize) throw new BusinessException("Poster vượt quá kích thước cho phép");
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes((int) maxSize + 1);
            if (bytes.length > maxSize) throw new BusinessException("Poster vượt quá kích thước cho phép");
            try (var imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw invalid();
                ImageReader reader = readers.next();
                try {
                    String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                    String contentType = TYPES.get(format);
                    if (contentType == null || !contentType.equalsIgnoreCase(file.getContentType())) throw invalid();
                    reader.setInput(imageInput, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 4096 || height > 4096) throw invalid();
                    if (reader.read(0) == null) throw invalid();
                    return new ValidatedPoster(bytes, contentType, "JPEG".equals(format) ? ".jpg" : "." + format.toLowerCase(Locale.ROOT));
                } finally { reader.dispose(); }
            }
        } catch (IOException ex) { throw invalid(); }
    }

    private BusinessException invalid() { return new BusinessException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP hợp lệ, tối đa 4096x4096"); }
    public record ValidatedPoster(byte[] bytes, String contentType, String extension) {}
}
