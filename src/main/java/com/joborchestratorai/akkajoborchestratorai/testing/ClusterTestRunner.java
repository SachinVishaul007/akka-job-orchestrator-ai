package com.joborchestratorai.akkajoborchestratorai.testing;

import com.joborchestratorai.akkajoborchestratorai.services.ClusteredResumeSearchService;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Profile({"node1", "node2", "clustered"})
public class ClusterTestRunner {

    @Autowired
    private ClusteredResumeSearchService searchService;

    public void runClusterTests() {
        try {
            // Test 1: Index multiple datasets
            System.out.println("=== Testing Dataset Indexing ===");

            CompletableFuture<Void> indexing1 = searchService.indexDatasetFile("tech-resumes", "/path/to/tech-resumes.xlsx");
            CompletableFuture<Void> indexing2 = searchService.indexDatasetFile("marketing-resumes", "/path/to/marketing-resumes.xlsx");
            CompletableFuture<Void> indexing3 = searchService.indexDatasetFile("finance-resumes", "/path/to/finance-resumes.xlsx");

            CompletableFuture.allOf(indexing1, indexing2, indexing3).get();
            System.out.println("All datasets indexed successfully!");

            // Test 2: Search in specific dataset
            System.out.println("\n=== Testing Single Dataset Search ===");

            CompletableFuture<List<SearchResult>> techResults = searchService.searchInDataset(
                    "tech-resumes",
                    "Senior Software Engineer with Java and microservices experience",
                    5
            );

            List<SearchResult> results = techResults.get();
            System.out.println("Found " + results.size() + " results in tech-resumes dataset");
            results.forEach(result ->
                    System.out.println("Score: " + result.getScore() + " - " + result.getContent())
            );

            // Test 3: Search across multiple datasets
            System.out.println("\n=== Testing Multi-Dataset Search ===");

            CompletableFuture<List<SearchResult>> multiResults = searchService.searchAcrossDatasets(
                    List.of("tech-resumes", "marketing-resumes", "finance-resumes"),
                    "Project Manager with technical background",
                    10
            );

            List<SearchResult> allResults = multiResults.get();
            System.out.println("Found " + allResults.size() + " results across all datasets");
            allResults.forEach(result ->
                    System.out.println("Score: " + result.getScore() + " - " + result.getContent())
            );

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}