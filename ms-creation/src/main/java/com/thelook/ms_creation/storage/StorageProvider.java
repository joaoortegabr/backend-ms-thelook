package com.thelook.ms_creation.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageProvider {
    String save(MultipartFile file, UUID creatorId, UUID outfitId, String fileName);
    void delete(String path);
}