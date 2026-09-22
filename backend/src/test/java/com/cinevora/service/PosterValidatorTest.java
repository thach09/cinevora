package com.cinevora.service;

import com.cinevora.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PosterValidatorTest {
    private final PosterValidator validator = new PosterValidator(5 * 1024 * 1024);

    static byte[] image(String format, int width, int height) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test void acceptsDecodedPngAndJpegIndependentOfFilename() throws Exception {
        for (String format : new String[]{"png", "jpeg"}) {
            var poster = validator.validate(new MockMultipartFile("file", "../../evil.svg", "image/" + format, image(format, 2, 2)));
            assertEquals("image/" + format, poster.contentType());
        }
    }

    @Test void rejectsMimeMismatchSvgAndTruncatedWebp() throws Exception {
        assertThrows(BusinessException.class, () -> validator.validate(new MockMultipartFile("file", "x.jpg", "image/jpeg", image("png", 1, 1))));
        assertThrows(BusinessException.class, () -> validator.validate(new MockMultipartFile("file", "x.png", "image/png", "<svg/>".getBytes())));
        assertThrows(BusinessException.class, () -> validator.validate(new MockMultipartFile("file", "x.webp", "image/webp", "RIFF0000WEBP".getBytes())));
    }

    @Test void rejectsEmptyOversizedAndExtremeDimensionsBeforeDecode() throws Exception {
        assertThrows(BusinessException.class, () -> validator.validate(new MockMultipartFile("file", new byte[0])));
        assertThrows(BusinessException.class, () -> validator.validate(new MockMultipartFile("file", new byte[5 * 1024 * 1024 + 1])));
        assertThrows(BusinessException.class, () -> validator.validate(new MockMultipartFile("file", "x.png", "image/png", image("png", 4097, 1))));
    }
}
