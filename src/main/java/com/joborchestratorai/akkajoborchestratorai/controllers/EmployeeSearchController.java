package com.joborchestratorai.akkajoborchestratorai.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmployeeSearchController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeSearchController.class);

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.search.engine.id}")
    private String searchEngineId;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/search-employees")
    public ResponseEntity<?> searchEmployees(@RequestParam String q) {
        logger.info("Received search request for query: {}", q);

        // Validate API key and search engine ID
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            String errorMsg = "Google API key is not properly configured";
            logger.error(errorMsg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(errorMsg));
        }

        if (searchEngineId == null || searchEngineId.isEmpty() || searchEngineId.startsWith("${")) {
            String errorMsg = "Google Search Engine ID is not properly configured";
            logger.error(errorMsg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(errorMsg));
        }

        try {
            // Build the Google Custom Search API URL with exact parameters that work in Postman
            // Note: Using the exact format that works in Postman - no URL encoding of the query
            String url = String.format(
                    "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=10",
                    apiKey,
                    searchEngineId,
                    q  // Not encoding here since Postman doesn't encode the query
            );

            logger.info("Using raw query parameter (not URL encoded)");

            // Log the actual URL being called (with key masked for security)
            String maskedUrl = String.format(
                    "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=10",
                    "[REDACTED]",
                    searchEngineId,
                    q  // Not encoding here to match the actual request
            );

            logger.info("Calling Google Custom Search API with URL (key redacted):\n{}", maskedUrl);
            logger.debug("Full URL with key: {}", url);  // Be careful with this in production

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // Make the API request
            logger.info("Sending request to Google API...");
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            logger.info("Received response with status: {}", response.getStatusCode());
            Map<String, Object> responseBody = response.getBody();

            // Log the response details
            if (responseBody != null) {
                logger.info("API Response - Search Information: {}", responseBody.get("searchInformation"));

                // Log items count if available
                if (responseBody.containsKey("items")) {
                    List<?> items = (List<?>) responseBody.get("items");
                    logger.info("Found {} results for query: {}", items.size(), q);

                    // Log first item as sample (if available)
                    if (!items.isEmpty()) {
                        logger.debug("First result: {}", items.get(0));
                    }
                } else {
                    logger.warn("No 'items' field in the response. Full response: {}", responseBody);
                }
            } else {
                logger.warn("Received null response body from Google API");
            }

            return ResponseEntity.ok(responseBody);

        } catch (HttpClientErrorException e) {
            String errorMsg = String.format("Google API error: %s - %s", e.getStatusCode(), e.getStatusText());
            logger.error("{} - Response: {}", errorMsg, e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode())
                    .body(createErrorResponse(errorMsg));

        } catch (Exception e) {
            String errorMsg = "Failed to search for employees: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(errorMsg));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }
}