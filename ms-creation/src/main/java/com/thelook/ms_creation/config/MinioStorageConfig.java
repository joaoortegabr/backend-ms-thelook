package com.thelook.ms_creation.config;

import com.thelook.ms_creation.storage.MinioStorageProvider;
import com.thelook.ms_creation.storage.StorageProvider;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
public class MinioStorageConfig {

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public StorageProvider storageProvider(MinioClient minioClient) {
        return new MinioStorageProvider(minioClient, bucket);
    }
}