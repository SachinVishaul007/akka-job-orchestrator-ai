package com.joborchestratorai.akkajoborchestratorai.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import com.joborchestratorai.akkajoborchestratorai.models.ResumeData;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import com.joborchestratorai.akkajoborchestratorai.services.ClusteredStorageService;
import com.joborchestratorai.akkajoborchestratorai.services.OpenAIService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.List;

public class ResumeShardActor extends AbstractBehavior<ResumeShardActor.Command> {

    public static final EntityTypeKey<Command> TYPE_KEY =
            EntityTypeKey.create(Command.class, "ResumeDataset");

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ProcessDataset.class, name = "processDataset"),
            @JsonSubTypes.Type(value = SearchInDataset.class, name = "searchInDataset")
    })
    public interface Command extends Serializable {
        String getDatasetId();
    }

    public static class ProcessDataset implements Command {
        private static final long serialVersionUID = 1L;

        public final String datasetId;
        public final String filePath;

        @JsonCreator
        public ProcessDataset(@JsonProperty("datasetId") String datasetId,
                              @JsonProperty("filePath") String filePath) {
            this.datasetId = datasetId;
            this.filePath = filePath;
        }

        @Override
        public String getDatasetId() { return datasetId; }
    }

    public static class SearchInDataset implements Command {
        private static final long serialVersionUID = 1L;

        public final String datasetId;
        public final String jobDescription;
        public final int topK;
        public final ActorRef<SearchResponse> replyTo;

        @JsonCreator
        public SearchInDataset(@JsonProperty("datasetId") String datasetId,
                               @JsonProperty("jobDescription") String jobDescription,
                               @JsonProperty("topK") int topK,
                               @JsonProperty("replyTo") ActorRef<SearchResponse> replyTo) {
            this.datasetId = datasetId;
            this.jobDescription = jobDescription;
            this.topK = topK;
            this.replyTo = replyTo;
        }

        @Override
        public String getDatasetId() { return datasetId; }
    }

    public static class SearchResponse implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String datasetId;
        public final List<SearchResult> results;

        @JsonCreator
        public SearchResponse(@JsonProperty("datasetId") String datasetId,
                              @JsonProperty("results") List<SearchResult> results) {
            this.datasetId = datasetId;
            this.results = results;
        }
    }

    private final String datasetId;
    private final ClusteredStorageService storageService;
    private final OpenAIService openAIService;
    private ResumeData cachedResumeData;

    public static Behavior<Command> create(String datasetId) {
        return Behaviors.setup(context -> new ResumeShardActor(context, datasetId));
    }

    private ResumeShardActor(ActorContext<Command> context, String datasetId) {
        super(context);
        this.datasetId = datasetId;
        this.storageService = new ClusteredStorageService();
        this.openAIService = new OpenAIService(new com.joborchestratorai.akkajoborchestratorai.services.LocalStorageService());

        getContext().getLog().info("ResumeShardActor started for dataset: {}", datasetId);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessDataset.class, this::onProcessDataset)
                .onMessage(SearchInDataset.class, this::onSearchInDataset)
                .build();
    }

    private Behavior<Command> onProcessDataset(ProcessDataset msg) {
        try {
            getContext().getLog().info("Processing dataset {} from file: {}", datasetId, msg.filePath);

            // Here you would integrate with your FileReaderActor logic
            // to process the Excel file and store it with the dataset ID

            // For now, simulate successful processing
            getContext().getLog().info("Dataset {} processed successfully", datasetId);
        } catch (Exception e) {
            getContext().getLog().error("Error processing dataset {}: {}", datasetId, e.getMessage());
        }
        return this;
    }

    private Behavior<Command> onSearchInDataset(SearchInDataset msg) {
        try {
            // Load cached data or fetch from storage
            if (cachedResumeData == null) {
                cachedResumeData = storageService.getDatasetResumeData(datasetId);
            }

            if (cachedResumeData == null) {
                getContext().getLog().warn("No data found for dataset: {}", datasetId);
                msg.replyTo.tell(new SearchResponse(datasetId, List.of()));
                return this;
            }

            List<SearchResult> results = openAIService.findMatchingPoints(msg.jobDescription, msg.topK);
            msg.replyTo.tell(new SearchResponse(datasetId, results));

            getContext().getLog().info("Search completed for dataset {} with {} results", datasetId, results.size());
        } catch (Exception e) {
            getContext().getLog().error("Error searching dataset {}: {}", datasetId, e.getMessage());
            msg.replyTo.tell(new SearchResponse(datasetId, List.of()));
        }
        return this;
    }
}