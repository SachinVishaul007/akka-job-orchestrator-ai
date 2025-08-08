package com.joborchestratorai.akkajoborchestratorai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;
@SpringBootApplication
public class AkkaJobOrchestratorAiApplication {
    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .filename(".env")
                .load();

        // Set system properties from .env file
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
        SpringApplication.run(AkkaJobOrchestratorAiApplication.class, args);
    }
}