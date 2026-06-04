package com.thelook.ms_social;

import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = CreatorRepository.class)
@EnableNeo4jRepositories(basePackageClasses = CreatorNodeRepository.class)
public class MsSocialApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsSocialApplication.class, args);
	}

}
