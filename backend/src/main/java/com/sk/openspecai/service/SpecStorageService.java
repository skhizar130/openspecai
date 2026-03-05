package com.sk.openspecai.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpecStorageService {

    @Value("${openapi.storage-path}")
    private String storagePath;

    public void saveYaml(String specId, String yamlContent) throws IOException {
        Path directory = Paths.get(storagePath);

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path filePath = directory.resolve(specId + ".yaml");

        Files.writeString(filePath, yamlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public String readYaml(String specId, boolean current) throws IOException {
        Path directory = Paths.get(storagePath);

        if (!Files.exists(directory)) {
            throw new FileNotFoundException("YAML with " + specId + " does not exist");
        }

        Path filePath = directory.resolve(current ? specId + ".yaml" : specId + "-updated.yaml");

        return Files.readString(filePath);
    }

    public List<String> readYamlLines(String specId, boolean current) throws IOException {
        Path directory = Paths.get(storagePath);

        if (!Files.exists(directory)) {
            throw new FileNotFoundException("YAML with " + specId + " does not exist");
        }

        Path filePath = directory.resolve(current ? specId + ".yaml" : specId + "-updated.yaml");

        return Files.readAllLines(filePath);
    }

    public String overwriteCurrentYaml(String specId) throws IOException {
        Path directory = Paths.get(storagePath);
        Path currentFilePath = directory.resolve(specId + ".yaml");
        Path updatedFilePath = directory.resolve(specId + "-updated.yaml");

        if (!Files.exists(currentFilePath)) {
            throw new FileNotFoundException("No specification exists for " + specId);
        }

        if (!Files.exists(updatedFilePath)) {
            throw new FileNotFoundException("No updated specification exists for " + specId);
        }

        String updatedContent = Files.readString(updatedFilePath);
        Files.writeString(currentFilePath, updatedContent, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        Files.delete(updatedFilePath);

        return updatedContent;
    }

}
