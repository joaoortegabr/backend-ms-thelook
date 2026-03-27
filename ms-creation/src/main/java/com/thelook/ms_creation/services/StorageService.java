package com.thelook.ms_creation.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
public class StorageService {

    private final String ROOT_DIR = "uploads/outfits/";

    public String save(MultipartFile file, String creatorId, String outfitId) {

        try {
            // Cria o caminho: uploads/outfits/creatorId/outfitId/
            Path path = Paths.get(ROOT_DIR, creatorId, outfitId);
            Files.createDirectories(path);

            // Define o nome do arquivo e salva
            Path filePath = path.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo: " + e.getMessage());
        }
    }

}
