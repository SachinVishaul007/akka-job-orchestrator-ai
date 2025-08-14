package com.joborchestratorai.akkajoborchestratorai.services;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import com.joborchestratorai.akkajoborchestratorai.actors.ClusteredMasterActor;
import com.joborchestratorai.akkajoborchestratorai.actors.ResumeShardActor;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import com.joborchestratorai.akkajoborchestratorai.services.LocalStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Profile({"node1", "node2", "node3", "node4", "clustered"})
public class ClusteredResumeSearchService {

    private final ActorSystem<ClusteredMasterActor.Command> actorSystem;
    private final ClusterSharding sharding;

    public ClusteredResumeSearchService(ActorSystem<ClusteredMasterActor.Command> actorSystem) {
        this.actorSystem = actorSystem;
        this.sharding = ClusterSharding.get(actorSystem);
    }

    public CompletableFuture<Void> indexDatasetFile(String datasetId, String filePath) {
        return CompletableFuture.runAsync(() -> {
            actorSystem.tell(new ClusteredMasterActor.ProcessDatasetFile(datasetId, filePath));
        });
    }

    public CompletableFuture<List<SearchResult>> searchInDataset(String datasetId, String jobDescription, int topK) {
        CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();

        EntityRef<ResumeShardActor.Command> entityRef = sharding.entityRefFor(
                ResumeShardActor.TYPE_KEY, datasetId
        );

        // Simplified approach using Behaviors.receiveMessage
        ActorRef<ResumeShardActor.SearchResponse> responseHandler =
                actorSystem.systemActorOf(
                        Behaviors.setup(context ->
                                Behaviors.receiveMessage(response -> {
                                    future.complete(response.results);
                                    return Behaviors.stopped();
                                })
                        ),
                        "search-response-" + System.currentTimeMillis(),
                        akka.actor.typed.Props.empty()
                );

        entityRef.tell(new ResumeShardActor.SearchInDataset(datasetId, jobDescription, topK, responseHandler));

        return future.orTimeout(30, TimeUnit.SECONDS);
    }

    public CompletableFuture<List<SearchResult>> searchAcrossDatasets(List<String> datasetIds,
                                                                      String jobDescription, int topK) {
        CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();

        actorSystem.tell(new ClusteredMasterActor.SearchAcrossDatasets(
                datasetIds, jobDescription, topK, future
        ));

        return future.orTimeout(60, TimeUnit.SECONDS);
    }

    // Convenience method to get all available dataset IDs
    public List<String> getAllDatasetIds() {
        try {
            LocalStorageService storageService = new LocalStorageService();
            return storageService.getAllDatasetIds().stream().toList();
        } catch (Exception e) {
            System.err.println("Error getting dataset IDs: " + e.getMessage());
            return List.of();
        }
    }

    // Test method to verify cluster connectivity
    public boolean isClusterReady() {
        try {
            return actorSystem != null && !actorSystem.whenTerminated().isCompleted();
        } catch (Exception e) {
            return false;
        }
    }
}