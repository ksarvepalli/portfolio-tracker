package com.portfolio.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortfoioTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfoioTrackerApplication.class, args);
	}

}
