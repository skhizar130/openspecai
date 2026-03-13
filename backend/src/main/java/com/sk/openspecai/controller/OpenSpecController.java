package com.sk.openspecai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.sk.openspecai.service.SwaggerhubService;
import com.sk.openspecai.excpetion.IdGenerationException;
import com.sk.openspecai.model.PromptRequest;
import com.sk.openspecai.model.UpdateRequest;
import com.sk.openspecai.service.ParsingService;
import com.sk.openspecai.service.SpecStorageService;
import com.sk.openspecai.service.SpecAssistant;
import com.sk.openspecai.service.UpdateSpecService;
import com.sk.openspecai.service.ValidationService;

import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class OpenSpecController {

    private SpecAssistant specsAssistant;
    private SpecStorageService specStorageService;
    private ParsingService parsingService;
    private UpdateSpecService updateSpecService;
    private ValidationService validationService;
    private SwaggerhubService swaggerhubService;

    public OpenSpecController(
            SpecAssistant specsAssistant,
            SpecStorageService specStorageService,
            ParsingService parsingService,
            UpdateSpecService updateSpecService,
            ValidationService validationService,
            SwaggerhubService swaggerhubService) {
        this.specsAssistant = specsAssistant;
        this.specStorageService = specStorageService;
        this.parsingService = parsingService;
        this.updateSpecService = updateSpecService;
        this.validationService = validationService;
        this.swaggerhubService = swaggerhubService;
    }

    @GetMapping(value = "api/specs/{id}", produces = "text/plain")
    public ResponseEntity<String> getSpecById(@PathVariable String id) {
        String yaml = specStorageService.readYaml(id, true);
        // String reconstructedYaml = parsingService.reconstructSpecs(specId);
        return ResponseEntity.ok(yaml);
    }

    @PostMapping(value = "/api/specs", produces = "text/plain")
    public ResponseEntity<String> generateSpec(@Valid @RequestBody PromptRequest request) {
        // Send user prompt to AI model for OpenAPI specs generation
        String modelRes = specsAssistant.generate(request.instruction());

        try {
            // Create deterministic UUID
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(modelRes.getBytes(StandardCharsets.UTF_8));
            String specId = HexFormat.of().formatHex(hash);
            String shortSpecId = specId.substring(0, 16);

            String yaml = parsingService.parseAndStoreChunks(modelRes, shortSpecId, request.name());

            specStorageService.saveYaml(shortSpecId, yaml);

            String response = "specsId: " + shortSpecId + "\n---\n" + yaml;
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchAlgorithmException e) {
            throw new IdGenerationException("Failed to generate ID for new spec");
        }

    }

    @PostMapping(value = "/api/specs/{id}", produces = "text/plain")
    public ResponseEntity<String> updateSpec(@PathVariable String id,
            @Valid @RequestBody UpdateRequest request) {
        String instruction = request.instruction();

        String unifiedDiff = updateSpecService.updateSpec(id, instruction);

        return ResponseEntity.status(HttpStatus.CREATED).body(unifiedDiff);
    }

    @GetMapping("/api/specs/validate/{id}")
    public String validateSpec(@PathVariable String id,
            @RequestParam(value = "fix", defaultValue = "false") boolean fix) {
        String yaml = specStorageService.readYaml(id, true);
        String spectalOutput = validationService.validateYamlWithSpectral(yaml);

        if (!fix) {
            return spectalOutput;
        }

        String prompt = """
                OpenAPI YAML:
                --------------------
                %s
                --------------------

                Spectral Validation Output:
                --------------------
                %s
                --------------------
                        """.formatted(yaml, spectalOutput);

        String fixedYaml = specsAssistant.fix(prompt);

        specStorageService.saveYaml(id, fixedYaml);

        return fixedYaml;
    }

    @GetMapping("/api/swaggerhub/connect")
    public Map<String, String> connectToSwaggerhub() {
        swaggerhubService.connect();

        return Map.of("Status", "Connected");
    }

    @PostMapping("/api/specs/{id}/publish")
    public String postMethodName(@PathVariable String id) {
        swaggerhubService.publish(id);

        return "Published Successfully";
    }

    @GetMapping("api/specs/{id}/preview")
    public String previewSwaggerhub(@PathVariable String id) {
        return swaggerhubService.preview(id);
    }

}
