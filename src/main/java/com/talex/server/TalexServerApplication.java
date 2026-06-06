package com.talex.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TalexServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TalexServerApplication.class, args);
	}

}
