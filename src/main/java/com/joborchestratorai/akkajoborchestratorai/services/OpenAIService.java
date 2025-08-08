package com.joborchestratorai.akkajoborchestratorai.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joborchestratorai.akkajoborchestratorai.models.ResumeData;
import com.joborchestratorai.akkajoborchestratorai.models.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenAIService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;


    @Value("${spring.ai.openai.chat.model:gpt-4}")
    private String model;

    private static final double DEFAULT_TEMPERATURE = 0.1;
    private static final int DEFAULT_MAX_TOKENS = 3000;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LocalStorageService storageService;

    public OpenAIService(LocalStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Unified method to call the OpenAI Chat Completion API.
     */
    private String callOpenAI(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        try {
            // Always trim API key to avoid hidden whitespace errors
            String cleanApiKey = Optional.ofNullable(apiKey)
                    .map(String::trim)
                    .orElseThrow(() -> new IllegalStateException("OpenAI API key is missing"));

            // Prepare request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cleanApiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // API call
            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL, HttpMethod.POST, requestEntity, String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Invalid response from OpenAI: " + response.getStatusCode());
            }

            // Parse JSON safely
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("No choices returned from OpenAI");
            }

            return choices.get(0).path("message").path("content").asText("");

        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
        }
    }

    public String generateTailoredResume(String jobDescription) throws IOException {
        ResumeData resumeData = storageService.getLatestResumeData();

        if (resumeData == null || resumeData.getResumePoints().isEmpty()) {
            throw new RuntimeException("No resume data available. Please upload a resume first.");
        }

        String systemPrompt = createSystemPrompt();
        String userPrompt = createUserPrompt(jobDescription, resumeData);

        return callOpenAI(systemPrompt, userPrompt, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS);
    }

    public String processTextWithPrompt(String prompt) {
        return callOpenAI("", prompt, 0.3, 4000);
    }

    public List<SearchResult> findMatchingPoints(String jobDescription, int topK) throws IOException {
        ResumeData resumeData = storageService.getLatestResumeData();

        if (resumeData == null || resumeData.getResumePoints().isEmpty()) {
            return Collections.emptyList();
        }

        String systemPrompt = String.format(
                "You are a resume analyzer. Given a job description and resume points, " +
                        "return the TOP %d most relevant points. Return ONLY exact text from the resume, unaltered. " +
                        "Format: 1. [0.95] Exact resume point", topK
        );

        String userPrompt = String.format(
                "Job Description:\n%s\n\nResume Points:\n%s\n\nReturn top %d matches with scores.",
                jobDescription,
                String.join("\n", resumeData.getResumePoints()),
                topK
        );

        String response = callOpenAI(systemPrompt, userPrompt, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS);
        return parseResponse(response);
    }

    private String createSystemPrompt() {
        return """
                You are a professional resume writer. Your task is to generate a tailored LaTeX resume based on the provided job description and existing resume data.
                
                STRICT REQUIREMENTS:
                1. Use ONLY the exact information provided in the original resume - DO NOT hallucinate or add fake information
                2. Keep the same structure: Education, Skills, Experience, Projects sections
                3. Maintain the same personal details (name, contact info, education details)
                4. Select and reorder bullet points to best match the job description
                5. Use the exact same LaTeX formatting and class structure as the original
                6. Ensure the resume fits on ONE page
                7. Do not modify education details, personal information, or company names
                8. Only reorder and select existing bullet points - never create new ones
                
                OUTPUT FORMAT: Return ONLY the complete LaTeX code, ready to compile.
                """;
    }

    private String createUserPrompt(String jobDescription, ResumeData resumeData) {
        return String.format("""
                Job Description:
                %s
                
                Original Resume Data (LaTeX format):
                %s
                
                Instructions:
                - Analyze the job description and identify key requirements
                - From the original resume, select and reorder bullet points that best match the job requirements
                - Keep all sections (Education, Skills, Experience, Projects) but prioritize relevant content
                - Maintain exact formatting, company names, dates, and personal information
                - Do not add any information not present in the original resume
                - Ensure the output is a complete, compilable LaTeX document
                
                Generate the tailored LaTeX resume:
                """, jobDescription, String.join("\n", resumeData.getResumePoints()));
    }

    private List<SearchResult> parseResponse(String response) {
        return Arrays.stream(response.split("\n"))
                .filter(line -> line.matches("\\d+\\.\\s*\\[\\d\\.\\d+\\].*"))
                .map(line -> {
                    try {
                        int start = line.indexOf("[") + 1;
                        int end = line.indexOf("]");
                        float score = Float.parseFloat(line.substring(start, end));
                        String content = line.substring(end + 1).trim();
                        return new SearchResult(content, score, "Matched");
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
