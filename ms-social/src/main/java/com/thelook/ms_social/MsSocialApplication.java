package com.thelook.ms_social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.thelook.ms_social.repositories.jpa")
@EnableNeo4jRepositories(basePackages = "com.thelook.ms_social.repositories.neo4j")
@EnableRedisRepositories(basePackages = "com.thelook.ms_social.repositories.redis")
public class MsSocialApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsSocialApplication.class, args);
	}

}
