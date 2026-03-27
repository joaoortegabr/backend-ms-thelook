package com.thelook.ms_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.thelook.ms_auth.repositories")
public class MsAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAuthApplication.class, args);
	}

}
