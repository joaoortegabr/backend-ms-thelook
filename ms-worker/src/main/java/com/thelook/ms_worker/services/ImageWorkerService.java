package com.thelook.ms_worker.services;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageWorkerService {

    @Value("${thelook.upload.directory:/app/uploads}")
    private String uploadDir;

    public String processToWebp(String relativePath) {
        try {
            Path base = Paths.get(uploadDir).toRealPath();
            Path inputPath = base.resolve(relativePath).normalize();
            if (!inputPath.startsWith(base))
                throw new SecurityException("Path traversal detectado: " + relativePath);

            // Define o output: muda a extensão para .webp e coloca numa pasta 'processed'
            String fileName = inputPath.getFileName().toString().replaceAll("\\.[^.]+$", "") + ".webp";
            Path outputDir = inputPath.getParent().resolve("processed");

            // Garante que a pasta 'processed' existe
            Files.createDirectories(outputDir);
            Path outputPath = outputDir.resolve(fileName);

            // 2. Processamento da Imagem
            ImmutableImage.loader()
                    .fromPath(inputPath)
                    .max(1080, 1350) // Resolução ideal para redes sociais (4:5 ou 1:1)
                    .output(WebpWriter.DEFAULT, outputPath);

            // 3. Retorna o caminho relativo para ser salvo nos bancos de dados
            // Ex: "outfits/2026/04/processed/imagem.webp"
            return Paths.get(uploadDir).relativize(outputPath).toString();

        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar imagem para WebP: " + relativePath, e);
        }
    }
}