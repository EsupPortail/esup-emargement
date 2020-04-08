package org.esupportail.emargement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import ch.rasc.sse.eventbus.config.EnableSseEventBus;

@SpringBootApplication
@EnableSseEventBus
public class EmargementJavaApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(EmargementJavaApplication.class, args);
	}
	
}
