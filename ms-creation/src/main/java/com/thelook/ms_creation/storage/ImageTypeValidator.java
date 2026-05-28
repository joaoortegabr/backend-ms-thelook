package com.thelook.ms_creation.storage;

import com.thelook.exceptions.UnprocessableRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class ImageTypeValidator {

    private static final byte[] JPEG_MAGIC  = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC   = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    // WebP: bytes 0-3 = "RIFF", bytes 8-11 = "WEBP"
    private static final byte[] RIFF_MAGIC  = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};

    private ImageTypeValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnprocessableRequestException("Image file is empty");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (!isJpeg(header) && !isPng(header) && !isWebP(header)) {
                throw new UnprocessableRequestException(
                        "Invalid image format. Accepted: JPEG, PNG, WebP");
            }
        } catch (UnprocessableRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new UnprocessableRequestException("Could not read image file");
        }
    }

    private static boolean isJpeg(byte[] h) {
        return h.length >= 3
                && h[0] == JPEG_MAGIC[0]
                && h[1] == JPEG_MAGIC[1]
                && h[2] == JPEG_MAGIC[2];
    }

    private static boolean isPng(byte[] h) {
        if (h.length < 8) return false;
        for (int i = 0; i < 8; i++) {
            if (h[i] != PNG_MAGIC[i]) return false;
        }
        return true;
    }

    private static boolean isWebP(byte[] h) {
        if (h.length < 12) return false;
        for (int i = 0; i < 4; i++) {
            if (h[i] != RIFF_MAGIC[i] || h[i + 8] != WEBP_MARKER[i]) return false;
        }
        return true;
    }
}