package com.joborchestratorai.akkajoborchestratorai.models;

import java.time.LocalDateTime;
import java.util.List;

public class ResumeData {
    private String id;
    private String fileName;
    private List<String> resumePoints;
    private LocalDateTime uploadedAt;

    public ResumeData() {}

    public ResumeData(String id, String fileName, List<String> resumePoints) {
        this.id = id;
        this.fileName = fileName;
        this.resumePoints = resumePoints;
        this.uploadedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public List<String> getResumePoints() { return resumePoints; }
    public void setResumePoints(List<String> resumePoints) { this.resumePoints = resumePoints; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}