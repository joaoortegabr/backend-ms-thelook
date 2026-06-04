package com.thelook.ms_creation.storage;

import com.thelook.exceptions.StorageException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


public class LocalStorageProvider implements StorageProvider {

    private final Path root;

    public LocalStorageProvider(String directory) {
        this.root = Paths.get(directory);
    }

    @Override
    public String save(MultipartFile file, UUID creatorId, UUID outfitId, String fileName) {
        try {
            // {directory}/{creatorId}/{outfitId}/{fileName}.ext
            Path directory = root.resolve(creatorId.toString()).resolve(outfitId.toString());
            Files.createDirectories(directory);

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            if (extension == null || extension.isBlank()) {
                throw new StorageException("Arquivo sem extensão não é permitido");
            }
            Path filePath = directory.resolve(fileName + "." + extension).normalize();

            if (!filePath.startsWith(root)) {
                throw new SecurityException("Path traversal detectado: " + filePath);
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return root.relativize(filePath).toString();
        } catch (IOException e) {
            throw new StorageException("Failed to store file locally", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path filePath = root.resolve(path).normalize();
            if (!filePath.startsWith(root)) {
                throw new SecurityException("Path traversal detectado ao deletar: " + path);
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete local file: " + path, e);
        }
    }
}