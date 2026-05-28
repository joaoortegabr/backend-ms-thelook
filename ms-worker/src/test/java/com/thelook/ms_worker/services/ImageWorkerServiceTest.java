package com.thelook.ms_worker.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImageWorkerServiceTest {

    @TempDir
    Path tempDir;

    ImageWorkerService imageWorkerService;

    @BeforeEach
    void setUp() {
        imageWorkerService = new ImageWorkerService();
        ReflectionTestUtils.setField(imageWorkerService, "uploadDir", tempDir.toString());
    }

    // ── processToWebp ──────────────────────────────────────────────────────────

    @Test
    void processToWebp_pathTraversal_throwsSecurityException() {
        assertThatThrownBy(() -> imageWorkerService.processToWebp("../outside.jpg"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal");
    }

    @Test
    void processToWebp_arquivoInexistente_throwsRuntimeException() {
        assertThatThrownBy(() -> imageWorkerService.processToWebp("nonexistent.jpg"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao processar imagem para WebP");
    }

    @Test
    void processToWebp_pathTraversalAbsoluto_throwsSecurityException() {
        assertThatThrownBy(() -> imageWorkerService.processToWebp("../../etc/passwd"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal");
    }
}
