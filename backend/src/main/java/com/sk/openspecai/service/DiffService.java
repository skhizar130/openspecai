package com.sk.openspecai.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

@Service
public class DiffService {

    private final SpecStorageService specStorageService;

    public DiffService(SpecStorageService specStorageService) {
        this.specStorageService = specStorageService;
    }

    public String generateUnifiedDiff(String specId) {
        // Read current and updated YAML as lines
        List<String> currentYamlLines = specStorageService.readYamlLines(specId, true);
        List<String> updatedYamlLines = specStorageService.readYamlLines(specId, false);

        // Compute the patch/diff
        Patch<String> patch = DiffUtils.diff(currentYamlLines, updatedYamlLines);

        // Generate unified diff format (like git diff)
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                "current.yaml",
                "updated.yaml",
                currentYamlLines,
                patch,
                3 // context lines
        );

        return String.join("\n", unifiedDiff);
    }

    public Map<String, String> getYamlDiffForEditor(String specId) throws IOException {
        String currentYaml = specStorageService.readYaml(specId, true);
        String updatedYaml = specStorageService.readYaml(specId, false);

        return Map.of(
                "original", currentYaml,
                "modified", updatedYaml);
    }
}