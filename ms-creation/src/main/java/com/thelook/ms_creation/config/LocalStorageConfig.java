package com.thelook.ms_creation.config;

import com.thelook.ms_creation.storage.LocalStorageProvider;
import com.thelook.ms_creation.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageConfig {

    @Value("${storage.local.directory:uploads/outfits}")
    private String directory;

    @Bean
    public StorageProvider storageProvider() {
        return new LocalStorageProvider(directory);
    }
}