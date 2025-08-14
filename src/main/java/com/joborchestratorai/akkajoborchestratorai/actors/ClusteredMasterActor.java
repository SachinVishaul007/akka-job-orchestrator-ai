package com.joborchestratorai.akkajoborchestratorai.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClusteredMasterActor extends AbstractBehavior<ClusteredMasterActor.Command> {

    public interface Command {}

    public static class ProcessDatasetFile implements Command {
        public final String datasetId;
        public final String filePath;

        public ProcessDatasetFile(String datasetId, String filePath) {
            this.datasetId = datasetId;
            this.filePath = filePath;
        }
    }

    public static class SearchAcrossDatasets implements Command {
        public final List<String> datasetIds;
        public final String jobDescription;
        public final int topK;
        public final CompletableFuture<List<SearchResult>> resultFuture;

        public SearchAcrossDatasets(List<String> datasetIds, String jobDescription, int topK,
                                    CompletableFuture<List<SearchResult>> resultFuture) {
            this.datasetIds = datasetIds;
            this.jobDescription = jobDescription;
            this.topK = topK;
            this.resultFuture = resultFuture;
        }
    }

    public static class CollectedSearchResults implements Command {
        public final List<SearchResult> allResults;
        public final CompletableFuture<List<SearchResult>> originalFuture;

        public CollectedSearchResults(List<SearchResult> allResults, CompletableFuture<List<SearchResult>> originalFuture) {
            this.allResults = allResults;
            this.originalFuture = originalFuture;
        }
    }

    private final ClusterSharding sharding;

    public static Behavior<Command> create() {
        return Behaviors.setup(ClusteredMasterActor::new);
    }

    private ClusteredMasterActor(ActorContext<Command> context) {
        super(context);
        this.sharding = ClusterSharding.get(context.getSystem());

        // Initialize the sharded entity
        sharding.init(Entity.of(ResumeShardActor.TYPE_KEY, entityContext ->
                ResumeShardActor.create(entityContext.getEntityId())
        ));

        getContext().getLog().info("ClusteredMasterActor initialized with sharding");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessDatasetFile.class, this::onProcessDatasetFile)
                .onMessage(SearchAcrossDatasets.class, this::onSearchAcrossDatasets)
                .onMessage(CollectedSearchResults.class, this::onCollectedSearchResults)
                .build();
    }

    private Behavior<Command> onProcessDatasetFile(ProcessDatasetFile msg) {
        EntityRef<ResumeShardActor.Command> entityRef = sharding.entityRefFor(
                ResumeShardActor.TYPE_KEY, msg.datasetId
        );

        entityRef.tell(new ResumeShardActor.ProcessDataset(msg.datasetId, msg.filePath));
        getContext().getLog().info("Sent dataset {} for processing to shard", msg.datasetId);

        return this;
    }

    private Behavior<Command> onSearchAcrossDatasets(SearchAcrossDatasets msg) {
        List<SearchResult> allResults = new ArrayList<>();
        int[] responsesReceived = {0};

        ActorRef<ResumeShardActor.SearchResponse> responseCollector =
                getContext().spawn(Behaviors.receive((context, response) -> {
                    allResults.addAll(response.results);
                    responsesReceived[0]++;

                    if (responsesReceived[0] >= msg.datasetIds.size()) {
                        // Sort by score and take top K
                        List<SearchResult> topResults = allResults.stream()
                                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                                .limit(msg.topK)
                                .toList();

                        getContext().getSelf().tell(new CollectedSearchResults(topResults, msg.resultFuture));
                    }

                    return Behaviors.same();
                }), "response-collector");

        // Send search requests to all datasets
        for (String datasetId : msg.datasetIds) {
            EntityRef<ResumeShardActor.Command> entityRef = sharding.entityRefFor(
                    ResumeShardActor.TYPE_KEY, datasetId
            );

            entityRef.tell(new ResumeShardActor.SearchInDataset(
                    datasetId, msg.jobDescription, msg.topK, responseCollector
            ));
        }

        return this;
    }

    private Behavior<Command> onCollectedSearchResults(CollectedSearchResults msg) {
        msg.originalFuture.complete(msg.allResults);
        return this;
    }
}