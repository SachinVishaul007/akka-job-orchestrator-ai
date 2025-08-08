package com.joborchestratorai.akkajoborchestratorai;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class DependencyCheckApplication {
    private static final Logger log = LoggerFactory.getLogger(DependencyCheckApplication.class);

    @GetMapping("/check")
    public String checkDependencies() {
        StringBuilder result = new StringBuilder("<pre>Dependency Check:\n\n");

        // 1. Check Akka
        try {
            ActorSystem<String> system = ActorSystem.create(
                    Behaviors.receive(String.class)
                            .onMessage(String.class, msg -> {
                                log.info("Akka message: {}", msg);
                                return Behaviors.same();
                            })
                            .build(),
                    "TestSystem"
            );
            system.tell("Akka works!");
            result.append("✅ Akka Actors: OK\n");
            system.terminate();
        } catch (Exception e) {
            result.append("❌ Akka: ").append(e.getMessage()).append("\n");
        }

        // 2. Check Apache POI (Excel support)
        try {
            Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook");
            result.append("✅ Apache POI (Excel): OK\n");
        } catch (ClassNotFoundException e) {
            result.append("❌ Apache POI: Not found\n");
        }

        // 3. Check PDFBox
        try {
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            result.append("✅ PDFBox: OK\n");
        } catch (ClassNotFoundException e) {
            result.append("❌ PDFBox: Not found\n");
        }

        // 4. Check OpenAI API Key
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            result.append("✅ OpenAI API Key: Set (").append(apiKey.substring(0, 7)).append("...)\n");
        } else {
            result.append("⚠️ OpenAI API Key: Not set - Set OPENAI_API_KEY environment variable\n");
        }

        // 5. Check Spring Boot
        result.append("✅ Spring Boot: Running\n");

        // 6. Check Jackson (JSON processing)
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            result.append("✅ Jackson (JSON): OK\n");
        } catch (ClassNotFoundException e) {
            result.append("❌ Jackson: Not found\n");
        }

        result.append("\n=== System is ready for OpenAI-based resume search ===\n");
        result.append("\nEndpoints:\n");
        result.append("- GET  /           - Web UI\n");
        result.append("- POST /api/upload - Upload Excel file\n");
        result.append("- POST /api/search - Search with job description\n");
        result.append("</pre>");

        return result.toString();
    }

    @Bean
    CommandLineRunner runner() {
        return args -> {
            log.info("=== Dependency Check Started ===");
            log.info("Spring Boot: ✅ Running");
            log.info("Visit http://localhost:8080/check to test all dependencies");
        };
    }
}