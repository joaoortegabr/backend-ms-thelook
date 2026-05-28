package com.thelook.ms_creation.storage;

import com.thelook.exceptions.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class MinioStorageProvider implements StorageProvider {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageProvider(MinioClient minioClient, String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public String save(MultipartFile file, UUID creatorId, UUID outfitId, String fileName) {
        try {
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            // {creatorId}/{outfitId}/{fileName}.ext
            String objectKey = creatorId + "/" + outfitId + "/" + fileName + "." + extension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            return objectKey;
        } catch (Exception e) {
            throw new StorageException("Failed to store file in MinIO", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete file from MinIO: " + objectKey, e);
        }
    }
}