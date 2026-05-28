package com.thelook.ms_creation.services;

import com.thelook.exceptions.UnprocessableRequestException;
import com.thelook.ms_creation.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock StorageProvider storageProvider;

    StorageService storageService;

    // JPEG magic bytes: FF D8 FF
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};

    @BeforeEach
    void setUp() {
        storageService = new StorageService(storageProvider);
    }

    // ── saveImage ──────────────────────────────────────────────────────────────

    @Test
    void saveImage_imagemJpegValida_delegaParaProviderERetornaCaminho() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.jpg", "image/jpeg", JPEG_BYTES);
        when(storageProvider.save(file, creatorId, outfitId, "main_look_1")).thenReturn("path/image.jpg");

        String result = storageService.saveImage(file, creatorId, outfitId, "main_look_1");

        assertThat(result).isEqualTo("path/image.jpg");
        verify(storageProvider).save(file, creatorId, outfitId, "main_look_1");
    }

    @Test
    void saveImage_formatoInvalido_lancaUnprocessableRequestExceptionSemChamarProvider() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.bmp", "image/bmp",
                new byte[]{0x42, 0x4D, 0x00, 0x00, 0x00});

        assertThatThrownBy(() -> storageService.saveImage(file, creatorId, outfitId, "main_look_1"))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("Invalid image format");

        verifyNoInteractions(storageProvider);
    }

    @Test
    void saveImage_arquivoVazio_lancaUnprocessableRequestExceptionSemChamarProvider() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("img", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.saveImage(file, creatorId, outfitId, "main_look_1"))
                .isInstanceOf(UnprocessableRequestException.class);

        verifyNoInteractions(storageProvider);
    }

    // ── deleteImage ────────────────────────────────────────────────────────────

    @Test
    void deleteImage_caminhoValido_delegaParaProvider() {
        storageService.deleteImage("path/to/image.jpg");

        verify(storageProvider).delete("path/to/image.jpg");
    }

    @Test
    void deleteImage_caminhoNulo_ignoraSemChamarProvider() {
        storageService.deleteImage(null);

        verifyNoInteractions(storageProvider);
    }

    @Test
    void deleteImage_caminhoEmBranco_ignoraSemChamarProvider() {
        storageService.deleteImage("   ");

        verifyNoInteractions(storageProvider);
    }
}
