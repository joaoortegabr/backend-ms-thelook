package com.thelook.ms_creation.services;

import com.thelook.ms_creation.storage.ImageTypeValidator;
import com.thelook.ms_creation.storage.StorageProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class StorageService {

    private final StorageProvider storageProvider;

    public StorageService(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String saveImage(MultipartFile file, UUID creatorId, UUID outfitId, String fileName) {
        ImageTypeValidator.validate(file);
        return storageProvider.save(file, creatorId, outfitId, fileName);
    }

    public void deleteImage(String path) {
        if (path != null && !path.isBlank()) {
            storageProvider.delete(path);
        }
    }
}