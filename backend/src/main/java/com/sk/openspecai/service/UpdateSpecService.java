package com.sk.openspecai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sk.openspecai.excpetion.EndpointNotFoundException;
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

    // Modify current spec using LLM and chunks from ChromaDB
    public String updateSpec(String specId, String userInstruction) {
        Endpoint endpoint = extractEndpoint(userInstruction);

        String currentOperationYaml = embeddingService.retrievePathChunks(specId,
                endpoint.path(),
                endpoint.method());

        if (currentOperationYaml == null) {
            throw new EndpointNotFoundException(
                    endpoint.method() + " " + endpoint.path() + " endpoint not found for specId: " + specId);
        }

        String llmPrompt = """
                OPERATION METADATA:
                Path: %s
                Method: %s

                USER INSTRUCTION:
                %s

                CURRENT YAML OPERATION:
                %s
                """.formatted(endpoint.path(), endpoint.method(), userInstruction, currentOperationYaml);

        String updatedOperationYaml = specsAssistant.update(llmPrompt);

        String currentSpecYaml = specStorageService.readYaml(specId, true);

        String mergedSpecYaml = parsingService.mergeOperation(currentSpecYaml, updatedOperationYaml, endpoint);

        specStorageService.saveYaml(specId + "-updated", mergedSpecYaml);

        String unifiedDiff = diffService.generateUnifiedDiff(specId);

        parsingService.overwriteChunks(specId);

        specStorageService.overwriteCurrentYaml(specId);

        return unifiedDiff;
    }

    // Extract Path and Method from instruction
    public Endpoint extractEndpoint(String instruction) {
        String methodRegex = "\\b(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)\\b";
        Matcher methodMatcher = Pattern.compile(methodRegex, Pattern.CASE_INSENSITIVE).matcher(instruction);
        String method = methodMatcher.find() ? methodMatcher.group() : "Not Found";

        String pathRegex = "(?<![a-zA-Z0-9])(/[a-zA-Z0-9/\\-._~{}%]+)";
        Matcher pathMatcher = Pattern.compile(pathRegex).matcher(instruction);
        String path = pathMatcher.find() ? pathMatcher.group() : "Not Found";

        return new Endpoint(path, method);
    }
}