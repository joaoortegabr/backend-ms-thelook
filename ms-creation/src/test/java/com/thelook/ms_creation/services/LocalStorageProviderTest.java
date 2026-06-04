package com.thelook.ms_creation.services;

import com.thelook.exceptions.StorageException;
import com.thelook.ms_creation.storage.LocalStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageProviderTest {

    @TempDir Path tempDir;

    LocalStorageProvider provider;

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @BeforeEach
    void setUp() {
        provider = new LocalStorageProvider(tempDir.toString());
    }

    // =========================================================
    // save
    // =========================================================

    @Test
    void save_validFile_storesUnderCreatorOutfitDirectoryAndReturnsRelativePath() throws IOException {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.jpg", "image/jpeg", JPEG_BYTES);

        String result = provider.save(file, creatorId, outfitId, "main");

        assertThat(result).contains(creatorId.toString()).contains(outfitId.toString()).endsWith(".jpg");
        assertThat(tempDir.resolve(result)).exists();
    }

    @Test
    void save_fileWithoutExtension_throwsStorageException() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "noextension", "image/jpeg", JPEG_BYTES);

        assertThatThrownBy(() -> provider.save(file, creatorId, outfitId, "main"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("extensão");
    }

    @Test
    void save_pathTraversalInFileName_throwsSecurityException() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        // Need 3 ../ to escape root/creatorId/outfitId depth
        MockMultipartFile file = new MockMultipartFile("img", "../../../evil.jpg", "image/jpeg", JPEG_BYTES);

        assertThatThrownBy(() -> provider.save(file, creatorId, outfitId, "../../../evil"))
                .isInstanceOf(SecurityException.class);
    }

    // =========================================================
    // delete
    // =========================================================

    @Test
    void delete_existingRelativePath_removesFile() throws IOException {
        Path dir = tempDir.resolve("creator/outfit");
        Files.createDirectories(dir);
        Path file = dir.resolve("image.jpg");
        Files.write(file, JPEG_BYTES);
        String relativePath = tempDir.relativize(file).toString();

        provider.delete(relativePath);

        assertThat(file).doesNotExist();
    }

    @Test
    void delete_nonExistentPath_doesNotThrow() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> provider.delete("creator/outfit/missing.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    void delete_pathTraversalAttempt_throwsSecurityException() {
        assertThatThrownBy(() -> provider.delete("../../etc/passwd"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal");
    }
}
