package com.thelook.ms_auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "password.encoder", havingValue = "argon2")
public class Argon2PasswordEncoderConfig {

    private static final int SALT_LENGTH  = 16;    // bytes
    private static final int HASH_LENGTH  = 32;    // bytes
    private static final int PARALLELISM  = 1;     // threads
    private static final int MEMORY       = 16384; // KB (16 MB)
    private static final int ITERATIONS   = 2;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY, ITERATIONS);
    }
}