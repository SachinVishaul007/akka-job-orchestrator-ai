package com.joborchestratorai.akkajoborchestratorai.config;

import akka.actor.typed.ActorSystem;
import com.joborchestratorai.akkajoborchestratorai.actors.MasterActor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Configuration
public class AkkaConfig {

    @Bean
    public Config akkaConfiguration() {
        return ConfigFactory.load();
    }

    @Bean(destroyMethod = "terminate")
    public ActorSystem<MasterActor.Command> actorSystem() {
        return ActorSystem.create(MasterActor.create(), "resume-search-system", akkaConfiguration());
    }
}