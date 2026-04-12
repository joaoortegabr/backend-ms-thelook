package com.thelook.ms_creation.config;

import com.thelook.ms_creation.models.mappers.OutfitMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapStructConfig {

    @Bean
    OutfitMapper outfitMapper() {
        return Mappers.getMapper(OutfitMapper.class);
    }

}

