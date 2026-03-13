package com.sk.openspecai.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sk.openspecai.excpetion.SpecValidationException;

@Service
public class ValidationService {

    @Value("${openspecai.spectral.rules}")
    private String spectralRulesPath;

    public String validateYamlWithSpectral(String yamlContent) {
        try {
            String spectralExecutable = resolveSpectralExecutable();

            // Create a temporary file to store the YAML content
            Path tempYamlFile = Files.createTempFile("openapi-", ".yaml");
            Files.writeString(tempYamlFile, yamlContent);

            // Build the process to run Spectral CLI
            ProcessBuilder processBuilder = new ProcessBuilder(
                    spectralExecutable,
                    "lint",
                    tempYamlFile.toAbsolutePath().toString(),
                    "--format",
                    "json",
                    "-r",
                    spectralRulesPath);

            // Merge error stream with standard output
            processBuilder.redirectErrorStream(true);
            Process spectralProcess = processBuilder.start();

            // Read Spectral output
            String spectralOutput = new String(spectralProcess.getInputStream().readAllBytes());

            // Wait for the process to complete
            spectralProcess.waitFor();

            // Delete the temporary YAML file
            Files.deleteIfExists(tempYamlFile);

            return spectralOutput;
        } catch (Exception e) {
            throw new SpecValidationException("Failed to validate OpenAPI spec with Spectral");
        }
    }

    private String resolveSpectralExecutable() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "spectral.cmd";
        }

        return "spectral";
    }
}