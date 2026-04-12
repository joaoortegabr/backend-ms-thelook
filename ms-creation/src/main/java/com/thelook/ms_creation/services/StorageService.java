package com.thelook.ms_creation.services;

import com.thelook.exceptions.StorageException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class StorageService {

    private final Path root = Paths.get("uploads/outfits");

    public String saveImage(MultipartFile file, UUID creatorId, UUID outfitId, String fileName) {
        try {
            // uploads/outfits/{creatorId}/{outfitId}/{fileName}.ext
            Path directory = root.resolve(creatorId.toString()).resolve(outfitId.toString());
            Files.createDirectories(directory);

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            Path filePath = directory.resolve(fileName + "." + extension);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

}
