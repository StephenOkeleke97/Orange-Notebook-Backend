package com.notesapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.notesapp.model.Salt;
import com.notesapp.repository.SaltRepository;
import com.notesapp.storage.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	/**
	 * Bean to initialize a salt model
	 * when the application is launched.
	 * If one does not exist, a new one is created
	 * and persisted so the same hash can
	 * be used for all passwords.
	 * 
	 * @param saltRepository the salt repository
	 * @return function to execute logic after context has started
	 */
	@Bean
	public CommandLineRunner init(SaltRepository saltRepository) {
		return (args) -> {
			if (saltRepository.findBySaltName("Default") == null) {
				Salt s = new Salt("Default");
				s.setSalt();
				saltRepository.save(s);
			}
		};
	}

}
