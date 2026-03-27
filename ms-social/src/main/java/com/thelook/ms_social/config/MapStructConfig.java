package com.thelook.ms_social.config;

import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.thelook.ms_social.mappers.CreatorMapper;

@Configuration
public class MapStructConfig {

    @Bean
    CreatorMapper creatorMapper() {
        return Mappers.getMapper(CreatorMapper.class);
    }

}
