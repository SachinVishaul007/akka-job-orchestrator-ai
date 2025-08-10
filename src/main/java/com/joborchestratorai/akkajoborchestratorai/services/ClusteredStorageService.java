package com.joborchestratorai.akkajoborchestratorai.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joborchestratorai.akkajoborchestratorai.models.ResumeData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ClusteredStorageService {

    @Value("${resume.storage.path:./resume-data}")
    private String storagePath;

    private final ObjectMapper objectMapper;
    private final Map<String, ResumeData> datasetCache = new HashMap<>();

    public ClusteredStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    private String getStoragePath() {
        return (storagePath != null && !storagePath.trim().isEmpty())
                ? storagePath : "./resume-data";
    }

    public void storeDatasetResumeData(String datasetId, ResumeData resumeData) throws IOException {
        Path storageDir = Paths.get(getStoragePath(), "datasets");
        java.nio.file.Files.createDirectories(storageDir);

        // Set the dataset-specific ID
        resumeData.setId(datasetId);

        Path dataPath = storageDir.resolve(datasetId + ".json");
        objectMapper.writeValue(dataPath.toFile(), resumeData);

        // Cache for quick access
        datasetCache.put(datasetId, resumeData);

        System.out.println("Dataset " + datasetId + " stored at: " + dataPath.toString());
    }

    public ResumeData getDatasetResumeData(String datasetId) throws IOException {
        // Check cache first
        if (datasetCache.containsKey(datasetId)) {
            return datasetCache.get(datasetId);
        }

        Path dataPath = Paths.get(getStoragePath(), "datasets", datasetId + ".json");
        File file = dataPath.toFile();

        if (!file.exists()) {
            System.out.println("No data found for dataset: " + datasetId);
            return null;
        }

        ResumeData data = objectMapper.readValue(file, ResumeData.class);
        datasetCache.put(datasetId, data);
        return data;
    }

    public Set<String> getAllDatasetIds() {
        File datasetsDir = new File(getStoragePath(), "datasets");
        if (!datasetsDir.exists()) {
            return Collections.emptySet();
        }

        File[] files = datasetsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return Collections.emptySet();
        }

        Set<String> datasetIds = new HashSet<>();
        for (File file : files) {
            String fileName = file.getName().replace(".json", "");
            datasetIds.add(fileName);
        }
        return datasetIds;
    }

    public void clearDataset(String datasetId) throws IOException {
        datasetCache.remove(datasetId);
        Path dataPath = Paths.get(getStoragePath(), "datasets", datasetId + ".json");
        File file = dataPath.toFile();
        if (file.exists() && !file.delete()) {
            throw new IOException("Failed to delete dataset file: " + dataPath);
        }
    }

    public boolean hasDatasetData(String datasetId) {
        File file = new File(getStoragePath(), "datasets/" + datasetId + ".json");
        return file.exists();
    }

    public int getDatasetCount() {
        return getAllDatasetIds().size();
    }
}