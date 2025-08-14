package com.joborchestratorai.akkajoborchestratorai.controllers;

import akka.actor.typed.ActorSystem;
import akka.cluster.typed.Cluster;
import com.joborchestratorai.akkajoborchestratorai.actors.ClusteredMasterActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Profile({"node1", "node2", "node3", "node4", "clustered"}) // Add node3 and node4
public class SimpleClusterTestController {

    @Autowired
    private ActorSystem<ClusteredMasterActor.Command> actorSystem;

    @GetMapping("/cluster/status")
    public Map<String, Object> getClusterStatus() {
        try {
            Cluster cluster = Cluster.get(actorSystem);

            Map<String, Object> status = new HashMap<>();
            status.put("selfNode", cluster.selfMember().address().toString());
            status.put("membersCount", cluster.state().getMembers().toString()); // Just convert to string for now
            status.put("unreachableCount", cluster.state().getUnreachable().toString());
            status.put("leader", cluster.state().getLeader() != null ?
                    cluster.state().getLeader().toString() : "No leader");
            status.put("clusterState", "UP");

            return status;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get cluster status: " + e.getMessage());
            return error;
        }
    }

    @GetMapping("/api/cluster/status")
    public String getSimpleStatus() {
        try {
            return "Cluster ready: " + (actorSystem != null);
        } catch (Exception e) {
            return "Cluster error: " + e.getMessage();
        }
    }

    @GetMapping("/cluster-test")  // Changed from /test to avoid conflict
    public String simpleTest() {
        return "Clustering is working! Node: " +
                Cluster.get(actorSystem).selfMember().address().toString();
    }
}