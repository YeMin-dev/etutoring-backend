package com.a9.etutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EtutoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(EtutoringApplication.class, args);
	}

}
