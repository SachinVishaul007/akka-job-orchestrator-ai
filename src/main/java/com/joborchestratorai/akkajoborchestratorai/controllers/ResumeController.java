package com.joborchestratorai.akkajoborchestratorai.controllers;


import com.joborchestratorai.akkajoborchestratorai.models.SearchRequest;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import com.joborchestratorai.akkajoborchestratorai.services.ResumeSearchService;
import com.joborchestratorai.akkajoborchestratorai.services.OpenAIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResumeController {

    private final ResumeSearchService resumeSearchService;
    private final OpenAIService openAIService;

    public ResumeController(ResumeSearchService resumeSearchService, OpenAIService openAIService) {
        this.resumeSearchService = resumeSearchService;
        this.openAIService = openAIService;
    }

    // Upload endpoint
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please select a file to upload"));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null ||
                    (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please upload a valid Excel file (.xlsx or .xls)"));
            }

            Path tempFile = Files.createTempFile("resume-", ".xlsx");
            file.transferTo(tempFile.toFile());

            resumeSearchService.indexExcelFile(tempFile.toString());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Resume data uploaded and indexed successfully");
            response.put("filename", originalFilename);
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process file: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Generate tailored LaTeX resume
    @PostMapping("/generate-resume")
    public ResponseEntity<Map<String, String>> generateTailoredResume(@RequestBody Map<String, String> request) {
        try {
            String jobDescription = request.get("jobDescription");

            if (jobDescription == null || jobDescription.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Job description is required", "status", "error"));
            }

            String latexResume = openAIService.generateTailoredResume(jobDescription);

            return ResponseEntity.ok(Map.of(
                    "message", "Resume generated successfully",
                    "latexCode", latexResume,
                    "status", "success"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to generate resume: " + e.getMessage(),
                            "status", "error"
                    ));
        }
    }

    // Legacy search for resume points (keeping for compatibility)
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<List<SearchResult>>> search(@RequestBody SearchRequest request) {
        return resumeSearchService.searchResumes(request.getJobDescription(), request.getTopK())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.internalServerError().build());
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "resume-tailoring"));
    }
}
