package com.joborchestratorai.akkajoborchestratorai.models;

public class SearchResult {
    private String content;
    private float score;
    private String category;

    public SearchResult() {}

    public SearchResult(String content, float score, String category) {
        this.content = content;
        this.score = score;
        this.category = category;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}