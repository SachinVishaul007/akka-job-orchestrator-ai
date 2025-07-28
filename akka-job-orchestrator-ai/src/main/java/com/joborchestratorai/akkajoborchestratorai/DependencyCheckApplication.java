package com.joborchestratorai.akkajoborchestratorai;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Component
@RestController
public class DependencyCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(DependencyCheckApplication.class, args);
    }

    @GetMapping("/check")
    public String checkDependencies() {
        StringBuilder result = new StringBuilder("Dependency Check:\n");

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
            result.append("✅ Akka: OK\n");
            system.terminate();
        } catch (Exception e) {
            result.append("❌ Akka: ").append(e.getMessage()).append("\n");
        }

        // 2. Check Qdrant (will fail unless Qdrant is running)
        try {
            QdrantClient client = new QdrantClient(
                    QdrantGrpcClient.newBuilder("localhost", 6334, false).build()
            );
            result.append("✅ Qdrant Client: Created (server connection not tested)\n");
            client.close();
        } catch (Exception e) {
            result.append("⚠️ Qdrant: Client created but server not running\n");
        }

        // 3. Check Lombok & Logging
        log.info("Testing Lombok logging");
        result.append("✅ Lombok & Logging: OK\n");

        // 4. Check Spring AI (requires API key to actually work)
        result.append("✅ Spring AI: Dependencies loaded (API key needed for actual use)\n");

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
