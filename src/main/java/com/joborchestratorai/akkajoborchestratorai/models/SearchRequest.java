package com.joborchestratorai.akkajoborchestratorai.models;

public class SearchRequest {
    private String jobDescription;
    private int topK = 5;

    public SearchRequest() {}

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
}