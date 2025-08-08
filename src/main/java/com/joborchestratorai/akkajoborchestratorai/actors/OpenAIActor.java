package com.joborchestratorai.akkajoborchestratorai.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.joborchestratorai.akkajoborchestratorai.services.OpenAIService;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class OpenAIActor extends AbstractBehavior<Object> {
    private final OpenAIService openAIService;

    public static Behavior<Object> create() {
        return Behaviors.setup(OpenAIActor::new);
    }

    private OpenAIActor(ActorContext<Object> context) {
        super(context);
        // Note: In real app, inject this properly
        this.openAIService = new OpenAIService(new com.joborchestratorai.akkajoborchestratorai.services.LocalStorageService());
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(MasterActor.SearchWithOpenAI.class, this::onSearchWithOpenAI)
                .build();
    }

    private Behavior<Object> onSearchWithOpenAI(MasterActor.SearchWithOpenAI msg) {
        try {
            List<SearchResult> results = openAIService.findMatchingPoints(msg.jobDescription, msg.limit);
            msg.replyTo.tell(new MasterActor.SearchComplete(results));
            getContext().getLog().info("OpenAI search completed with {} results", results.size());
        } catch (Exception e) {
            getContext().getLog().error("Error in OpenAI search", e);
            msg.replyTo.tell(new MasterActor.SearchComplete(List.of()));
        }
        return this;
    }
}