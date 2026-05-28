package com.thelook.ms_creation.storage;

import com.thelook.exceptions.StorageException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

public class S3StorageProvider implements StorageProvider {

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageProvider(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String save(MultipartFile file, UUID creatorId, UUID outfitId, String fileName) {
        try {
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            // {creatorId}/{outfitId}/{fileName}.ext
            String objectKey = creatorId + "/" + outfitId + "/" + fileName + "." + extension;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return objectKey;
        } catch (IOException e) {
            throw new StorageException("Failed to store file in S3", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }
}