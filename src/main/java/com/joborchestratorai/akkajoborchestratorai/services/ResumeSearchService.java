package com.joborchestratorai.akkajoborchestratorai.services;

import akka.actor.typed.ActorSystem;
import com.joborchestratorai.akkajoborchestratorai.actors.MasterActor;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ResumeSearchService {

    private final ActorSystem<MasterActor.Command> actorSystem;

    public ResumeSearchService(ActorSystem<MasterActor.Command> actorSystem) {
        this.actorSystem = actorSystem;
    }

    public CompletableFuture<Void> indexExcelFile(String filePath) {
        return CompletableFuture.runAsync(() -> {
            actorSystem.tell(new MasterActor.ProcessExcelFile(filePath));
        });
    }

    public CompletableFuture<List<SearchResult>> searchResumes(String jobDescription, int topK) {
        CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();
        actorSystem.tell(new MasterActor.ProcessJobDescription(jobDescription, topK, future));
        return future.orTimeout(30, TimeUnit.SECONDS);
    }
}