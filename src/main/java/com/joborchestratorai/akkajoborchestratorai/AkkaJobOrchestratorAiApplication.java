package com.joborchestratorai.akkajoborchestratorai;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AkkaJobOrchestratorAiApplication {

    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .filename(".env")
                .load();
        
        // Set system properties from .env file
        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
        
        // Start the Spring application
        ConfigurableApplicationContext context = SpringApplication.run(AkkaJobOrchestratorAiApplication.class, args);
    }
}
