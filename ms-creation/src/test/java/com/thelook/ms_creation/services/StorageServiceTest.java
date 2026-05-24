package com.thelook.ms_creation.services;

import com.thelook.exceptions.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService();
    }

    @Test
    void saveImage_retornaPathComCreatorIdOutfitIdEFileName() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            mockedFiles.when(() -> Files.copy(any(), any(Path.class), any())).thenReturn(3L);

            String path = storageService.saveImage(file, creatorId, outfitId, "main_look_1");

            assertThat(path).contains(creatorId.toString());
            assertThat(path).contains(outfitId.toString());
            assertThat(path).contains("main_look_1");
            assertThat(path).endsWith(".jpg");
        }
    }

    @Test
    void saveImage_extensaoPreservadaNoNomeDoArquivo() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.png", "image/png", new byte[]{1});

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            mockedFiles.when(() -> Files.copy(any(), any(Path.class), any())).thenReturn(1L);

            String path = storageService.saveImage(file, creatorId, outfitId, "item_0");

            assertThat(path).endsWith(".png");
        }
    }

    @Test
    void saveImage_ioException_lancaStorageException() throws IOException {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getInputStream()).thenThrow(new IOException("disco cheio"));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            assertThatThrownBy(() -> storageService.saveImage(file, creatorId, outfitId, "main_look_1"))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("Failed to store file");
        }
    }

    @Test
    void saveImage_criaSubdiretoriosComCreatorIdEOutfitId() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.jpg", "image/jpeg", new byte[]{1});

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            mockedFiles.when(() -> Files.copy(any(), any(Path.class), any())).thenReturn(1L);

            storageService.saveImage(file, creatorId, outfitId, "main_look_1");

            mockedFiles.verify(() -> Files.createDirectories(argThat(p ->
                    p.toString().contains(creatorId.toString()) && p.toString().contains(outfitId.toString())
            )));
        }
    }
}