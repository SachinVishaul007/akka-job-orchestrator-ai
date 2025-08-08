package com.joborchestratorai.akkajoborchestratorai.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.joborchestratorai.akkajoborchestratorai.services.LocalStorageService;

public class StorageActor extends AbstractBehavior<Object> {
    private final LocalStorageService storageService;

    public static Behavior<Object> create() {
        return Behaviors.setup(StorageActor::new);
    }

    private StorageActor(ActorContext<Object> context) {
        super(context);
        this.storageService = new LocalStorageService();
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(MasterActor.StoreResumeData.class, this::onStoreResumeData)
                .build();
    }

    private Behavior<Object> onStoreResumeData(MasterActor.StoreResumeData msg) {
        try {
            storageService.storeResumeData(msg.resumeData);
            getContext().getLog().info("Stored resume data: {}", msg.resumeData.getFileName());
        } catch (Exception e) {
            getContext().getLog().error("Error storing resume data", e);
        }
        return this;
    }
}