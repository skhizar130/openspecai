package com.sk.openspecai.service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sk.openspecai.model.Endpoint;

@Service
public class UpdateSpecService {

    private final EmbeddingService embeddingService;
    private final SpecAssistant specsAssistant;
    private final SpecStorageService specStorageService;
    private final ParsingService parsingService;
    private final DiffService diffService;

    // Constructor injection of required services
    public UpdateSpecService(EmbeddingService embeddingService,
            SpecAssistant specsAssistant,
            SpecStorageService specStorageService,
            ParsingService parsingService,
            DiffService diffService) {
        this.embeddingService = embeddingService;
        this.specsAssistant = specsAssistant;
        this.specStorageService = specStorageService;
        this.parsingService = parsingService;
        this.diffService = diffService;
    }

    /**
     * Updates the OpenAPI spec for a given specId based on a user instruction.
     * Fetches current operation YAML, sends it to the LLM for updates,
     * merges it back into the spec, stores the updated spec, and returns the diff.
     */
    public String updateSpec(String specId, String userInstruction) throws IOException {
        // Extract endpoint info (path + method) from instruction
        Endpoint endpoint = extractEndpoint(userInstruction);

        // Retrieve current YAML operation for the endpoint
        String currentOperationYaml = embeddingService.retrievePathChunks(specId,
                endpoint.path(),
                endpoint.method());

        // Build prompt for LLM
        String llmPrompt = """
                OPERATION METADATA:
                Path: %s
                Method: %s

                USER INSTRUCTION:
                %s

                CURRENT YAML OPERATION:
                %s
                """.formatted(endpoint.path(), endpoint.method(), userInstruction, currentOperationYaml);

        // Generate updated operation YAML using SpecsAssistant
        String updatedOperationYaml = specsAssistant.update(llmPrompt);

        // Read full spec YAML
        String currentSpecYaml = specStorageService.readYaml(specId, true);

        // Merge updated operation into full spec
        String mergedSpecYaml = parsingService.mergeOperation(currentSpecYaml, updatedOperationYaml, endpoint);

        // Save updated spec
        specStorageService.saveYaml(specId + "-updated", mergedSpecYaml);

        // Generate and return diff for user review
        return diffService.generateUnifiedDiff(specId);
    }

    /**
     * Extracts the intended HTTP method and path from a user instruction.
     */
    public Endpoint extractEndpoint(String instruction) {
        // Match HTTP method (GET, POST, etc.)
        String methodRegex = "\\b(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)\\b";
        Matcher methodMatcher = Pattern.compile(methodRegex, Pattern.CASE_INSENSITIVE).matcher(instruction);
        String method = methodMatcher.find() ? methodMatcher.group() : "Not Found";

        // Match API path starting with /
        String pathRegex = "(?<![a-zA-Z0-9])(/[a-zA-Z0-9/\\-._~{}%]+)";
        Matcher pathMatcher = Pattern.compile(pathRegex).matcher(instruction);
        String path = pathMatcher.find() ? pathMatcher.group() : "Not Found";

        return new Endpoint(path, method);
    }
}