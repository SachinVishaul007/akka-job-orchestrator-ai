package com.joborchestratorai.akkajoborchestratorai.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import com.joborchestratorai.akkajoborchestratorai.models.ResumeData;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MasterActor extends AbstractBehavior<MasterActor.Command> {

    public interface Command {}

    // Messages
    public static class ProcessExcelFile implements Command {
        public final String filePath;
        public ProcessExcelFile(String filePath) { this.filePath = filePath; }
    }

    public static class ProcessJobDescription implements Command {
        public final String jobDescription;
        public final int topK;
        public final CompletableFuture<List<SearchResult>> resultFuture;

        public ProcessJobDescription(String jobDescription, int topK, CompletableFuture<List<SearchResult>> resultFuture) {
            this.jobDescription = jobDescription;
            this.topK = topK;
            this.resultFuture = resultFuture;
        }
    }

    // FIXED: Added "implements Command"
    public static class StoreResumeData implements Command {
        public final ResumeData resumeData;
        public StoreResumeData(ResumeData resumeData) { this.resumeData = resumeData; }
    }

    public static class SearchWithOpenAI {
        public final String jobDescription;
        public final int limit;
        public final ActorRef<SearchComplete> replyTo;

        public SearchWithOpenAI(String jobDescription, int limit, ActorRef<SearchComplete> replyTo) {
            this.jobDescription = jobDescription;
            this.limit = limit;
            this.replyTo = replyTo;
        }
    }

    public static class SearchComplete {
        public final List<SearchResult> results;
        public SearchComplete(List<SearchResult> results) { this.results = results; }
    }

    public static class AdaptedSearchComplete implements Command {
        public final SearchComplete searchComplete;
        public AdaptedSearchComplete(SearchComplete searchComplete) { this.searchComplete = searchComplete; }
    }

    private final ActorRef<Object> storageActor;
    private final ActorRef<Object> openAIActor;
    private final ActorRef<ProcessExcelFile> fileReaderActor;
    private ProcessJobDescription currentJobSearch;

    public static Behavior<Command> create() {
        return Behaviors.setup(MasterActor::new);
    }

    private MasterActor(ActorContext<Command> context) {
        super(context);
        this.storageActor = context.spawn(StorageActor.create(), "storage-actor");
        this.openAIActor = context.spawn(OpenAIActor.create(), "openai-actor");
        this.fileReaderActor = context.spawn(FileReaderActor.create(storageActor), "file-reader-actor");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessExcelFile.class, msg -> {
                    fileReaderActor.tell(msg);
                    return this;
                })
                .onMessage(ProcessJobDescription.class, this::onProcessJobDescription)
                .onMessage(AdaptedSearchComplete.class, this::onSearchComplete)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onProcessJobDescription(ProcessJobDescription msg) {
        currentJobSearch = msg;
        ActorRef<SearchComplete> responseAdapter = getContext().messageAdapter(SearchComplete.class, AdaptedSearchComplete::new);
        openAIActor.tell(new SearchWithOpenAI(msg.jobDescription, msg.topK, responseAdapter));
        return this;
    }

    private Behavior<Command> onSearchComplete(AdaptedSearchComplete msg) {
        if (currentJobSearch != null) {
            currentJobSearch.resultFuture.complete(msg.searchComplete.results);
            currentJobSearch = null;
        }
        return this;
    }

    private Behavior<Command> onPostStop() {
        return this;
    }
}