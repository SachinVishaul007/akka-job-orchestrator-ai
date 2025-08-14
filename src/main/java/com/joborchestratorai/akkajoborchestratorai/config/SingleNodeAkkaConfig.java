package com.joborchestratorai.akkajoborchestratorai.config;

import akka.actor.typed.ActorSystem;
import com.joborchestratorai.akkajoborchestratorai.actors.MasterActor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Configuration
@Profile("single-node")
public class SingleNodeAkkaConfig {

    @Bean
    public Config singleNodeAkkaConfiguration() {  // Changed name to avoid conflicts
        return ConfigFactory.parseString("""
            akka {
              actor {
                provider = local
              }
            }
            """).withFallback(ConfigFactory.load());
    }

    @Bean(destroyMethod = "terminate")
    public ActorSystem<MasterActor.Command> singleNodeActorSystem() {
        return ActorSystem.create(
                MasterActor.create(),
                "resume-search-system",
                singleNodeAkkaConfiguration()  // Use the renamed method
        );
    }
}