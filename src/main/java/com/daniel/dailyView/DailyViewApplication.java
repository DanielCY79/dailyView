package com.daniel.dailyView;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class DailyViewApplication {

	public static void main(String[] args) {
		SpringApplication.run(DailyViewApplication.class, args);
	}

}
