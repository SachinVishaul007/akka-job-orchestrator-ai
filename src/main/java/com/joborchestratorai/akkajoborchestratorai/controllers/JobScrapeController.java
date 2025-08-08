package com.joborchestratorai.akkajoborchestratorai.controllers;

import com.joborchestratorai.akkajoborchestratorai.services.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@Controller
public class JobScrapeController {

    private final OpenAIService openAIService;

    @Autowired
    public JobScrapeController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @GetMapping("/jobscrape")
    public String jobScrapePage() {
        return "jobscrape";
    }

    @PostMapping("/api/scrape")
    @ResponseBody
    public ResponseEntity<Map<String, String>> scrapeJobDescription(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        Map<String, String> response = new HashMap<>();

        try {
            // Basic URL validation
            if (url == null || url.trim().isEmpty()) {
                response.put("error", "URL cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Fetch the webpage
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            // First, try to extract using common job description selectors
            String description = extractJobDescription(doc);

            if (description == null || description.trim().isEmpty()) {
                // If no specific content found, try to find the largest text block
                description = findLargestTextBlock(doc);
            }

            // Use OpenAI to filter and extract only the job description
            if (description != null && !description.trim().isEmpty()) {
                try {
                    String filteredDescription = filterJobDescription(description);
                    response.put("description", filteredDescription);
                } catch (Exception e) {
                    // If filtering fails, return the raw description
                    response.put("description", description);
                }
            } else {
                response.put("error", "Could not extract job description from the page.");
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Failed to scrape the URL: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String extractJobDescription(Document doc) {
        // Common job description selectors
        String[] selectors = {
                "div.job-description",
                "div.description",
                "div.job-details",
                "div.job-description__container",
                "div.job-description-text",
                "div.job-details__description",
                "div.job-description-content",
                "div.job-description__content",
                "div.job-description__text",
                "div[itemprop='description']",
                "div.description__text"
        };

        // Try each selector until we find content
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                // Return the first matching element's text
                return elements.get(0).text();
            }
        }

        // If no specific selectors matched, try to find the largest text block
        return findLargestTextBlock(doc);
    }

    private String findLargestTextBlock(Document doc) {
        // Find all paragraph and div elements
        Elements elements = doc.select("p, div, section, article, main");
        Element largestElement = null;
        int maxLength = 0;

        for (Element element : elements) {
            // Skip elements that are too small or likely to be navigation/menus
            if (element.text().trim().length() < 100) continue;

            // Skip common non-content elements
            String className = element.className().toLowerCase();
            String id = element.id().toLowerCase();
            if (className.contains("nav") || id.contains("nav") ||
                    className.contains("header") || id.contains("header") ||
                    className.contains("footer") || id.contains("footer") ||
                    className.contains("menu") || id.contains("menu") ||
                    className.contains("sidebar") || id.contains("sidebar")) {
                continue;
            }

            String text = element.text().trim();
            if (text.length() > maxLength) {
                maxLength = text.length();
                largestElement = element;
            }
        }

        return largestElement != null ? largestElement.text() : "";
    }

    private String filterJobDescription(String rawText) {
        // This prompt is designed to extract and format the complete job description
        String prompt = "Extract and format the job description from the following text. Follow these guidelines:\n" +
                "1. Start with the job title in a heading format (e.g., '## Job Title')\n" +
                "2. Include company name, location, and work type (remote/onsite/hybrid) in a clear format\n" +
                "3. List minimum qualifications with bullet points\n" +
                "4. List preferred qualifications with bullet points\n" +
                "5. List key responsibilities with bullet points\n" +
                "6. Include any other relevant sections - any text that is useful for the jobseeker (e.g., 'About the Team', 'About the Job', 'Company Overview' etc,.)\n\n" +
                "Formatting rules:\n" +
                "- Use Markdown for formatting (## for section headers, - for bullet points, ** for emphasis)\n" +
                "- Preserve all technical terms, tools, and specific requirements\n" +
                "- Keep the original language and terminology used in the job description\n" +
                "- Maintain the original order of sections as much as possible\n\n" +
                "Exclude:\n" +
                "- Similar job listings or 'Other jobs you may like'\n" +
                "- Application instructions or 'How to apply' sections\n" +
                "- Company benefits and perks (unless specifically part of the job requirements)\n" +
                "- Equal opportunity statements or legal disclaimers\n\n" +
                "Here is the text to process:\n\n" + rawText;

        try {
            // We'll use the existing OpenAIService to process the text
            // This assumes OpenAIService has a method to process text with a prompt
            return openAIService.processTextWithPrompt(prompt);
        } catch (Exception e) {
            // If there's an error with the API, return the original text
            return rawText;
        }
    }
}