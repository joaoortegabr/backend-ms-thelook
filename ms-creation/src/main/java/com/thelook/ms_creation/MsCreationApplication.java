package com.thelook.ms_creation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsCreationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCreationApplication.class, args);
	}

}
