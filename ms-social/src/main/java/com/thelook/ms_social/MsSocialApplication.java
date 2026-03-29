package com.thelook.ms_social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MsSocialApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsSocialApplication.class, args);
	}

}
