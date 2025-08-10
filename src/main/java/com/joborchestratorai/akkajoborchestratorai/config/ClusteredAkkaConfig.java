package com.joborchestratorai.akkajoborchestratorai.config;

import akka.actor.typed.ActorSystem;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.joborchestratorai.akkajoborchestratorai.actors.ClusteredMasterActor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Configuration
@Profile({"node1", "node2", "clustered"})  // Activate for node1, node2, or clustered profiles
public class ClusteredAkkaConfig {

    @Value("${akka.remote.artery.canonical.port:2551}")
    private int akkaPort;

    @Value("${akka.management.http.port:8558}")
    private int managementPort;

    @Bean
    public Config clusteredAkkaConfiguration() {  // Changed name to avoid conflicts
        return ConfigFactory.parseString(String.format("""
            akka {
              actor {
                provider = cluster
                
                serializers {
                  java = "akka.serialization.JavaSerializer"
                }
                
                serialization-bindings {
                  "com.joborchestratorai.akkajoborchestratorai.actors.ResumeShardActor$Command" = java
                  "com.joborchestratorai.akkajoborchestratorai.actors.ClusteredMasterActor$Command" = java
                  "java.io.Serializable" = java
                }
              }
              
              remote.artery {
                canonical.hostname = "127.0.0.1"
                canonical.port = %d
              }
              
              cluster {
                seed-nodes = [
                  "akka://resume-search-system@127.0.0.1:2551",
                  "akka://resume-search-system@127.0.0.1:2552"
                ]
                downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
                split-brain-resolver {
                  active-strategy = "keep-oldest"
                  stable-after = 10s
                }
                min-nr-of-members = 1
              }
              
              management {
                http {
                  hostname = "127.0.0.1"
                  port = %d
                }
              }
            }
            """, akkaPort, managementPort));
    }

    @Bean(destroyMethod = "terminate")
    public ActorSystem<ClusteredMasterActor.Command> clusteredActorSystem() {
        ActorSystem<ClusteredMasterActor.Command> system = ActorSystem.create(
                ClusteredMasterActor.create(),
                "resume-search-system",
                clusteredAkkaConfiguration()  // Use the renamed method
        );

        // Start Akka Management
        AkkaManagement.get(system).start();

        // Start Cluster Bootstrap
        ClusterBootstrap.get(system).start();

        return system;
    }
}