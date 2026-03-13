package com.sk.openspecai.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sk.openspecai.excpetion.SpecNotFoundException;
import com.sk.openspecai.excpetion.SpecReadException;
import com.sk.openspecai.excpetion.SpecWriteException;

@Service
public class SpecStorageService {

    @Value("${openapi.storage-path}")
    private String storagePath;

    public void saveYaml(String specId, String yamlContent) {
        Path directory = Paths.get(storagePath);

        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(specId + ".yaml");

            Files.writeString(filePath, yamlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new SpecWriteException("Failed to write spec file for id: " + specId);
        }
    }

    public String readYaml(String specId, boolean current) {
        Path directory = Paths.get(storagePath);
        Path filePath = directory.resolve(current ? specId + ".yaml" : specId + "-updated.yaml");

        if (!Files.exists(filePath)) {
            throw new SpecNotFoundException("No spec file found for id: " + specId);
        }
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new SpecReadException("Failed to read spec file for id: " + specId);
        }
    }

    public List<String> readYamlLines(String specId, boolean current) {
        Path directory = Paths.get(storagePath);
        Path filePath = directory.resolve(current ? specId + ".yaml" : specId + "-updated.yaml");

        if (!Files.exists(filePath)) {
            throw new SpecNotFoundException("No spec file found for id: " + specId);
        }

        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new SpecReadException("Failed to read spec file for id: " + specId);
        }
    }

    public String overwriteCurrentYaml(String specId) {
        Path directory = Paths.get(storagePath);
        Path currentFilePath = directory.resolve(specId + ".yaml");
        Path updatedFilePath = directory.resolve(specId + "-updated.yaml");

        if (!Files.exists(currentFilePath)) {
            throw new SpecNotFoundException("No spec file found for id: " + specId);
        }

        if (!Files.exists(updatedFilePath)) {
            throw new SpecNotFoundException("No updated spec file found for id: " + specId);
        }
        try {
            String updatedContent = Files.readString(updatedFilePath);
            Files.writeString(currentFilePath, updatedContent, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Files.delete(updatedFilePath);

            return updatedContent;
        } catch (IOException e) {
            throw new SpecWriteException("Failed to overwriting spec file for id: " + specId);
        }
    }

}
