package com.sk.openspecai.service;

import java.util.Iterator;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.sk.openspecai.excpetion.SpecParsingException;
import com.sk.openspecai.model.Endpoint;
import com.sk.openspecai.model.SearchResultDTO;
import com.sk.openspecai.model.SpecInfo;

@Service
public class ParsingService {

    private EmbeddingService embeddingService;
    private ObjectMapper yamlMapper;
    private SpecStorageService specStorageService;

    public ParsingService(
            EmbeddingService embeddingService,
            SpecStorageService specStorageService) {
        this.embeddingService = embeddingService;
        this.specStorageService = specStorageService;

        YAMLFactory factory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
                .build();

        this.yamlMapper = new ObjectMapper(factory);
        this.yamlMapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        this.yamlMapper.setNodeFactory(new com.fasterxml.jackson.databind.node.JsonNodeFactory(true));
        this.yamlMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    }

    // Parse YAML and store all chunks into embedding store
    public String parseAndStoreChunks(String yamlContent, String specId, String name) {
        try {
            JsonNode rootNode = this.yamlMapper.readTree(yamlContent);

            storeInfo(rootNode, specId, name);
            storeServers(rootNode, specId);
            storeOperations(rootNode, specId);
            storeSchemas(rootNode, specId);

            return yamlMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new SpecParsingException("Failed to parse OpenAPI spec");
        }
    }

    // Store path-method chunks
    public void storeOperations(JsonNode rootNode, String specId) {
        JsonNode pathsNode = rootNode.path("paths");
        Iterator<String> pathIterator = pathsNode.fieldNames();

        while (pathIterator.hasNext()) {
            String path = pathIterator.next();
            JsonNode methodsNode = pathsNode.get(path);
            Iterator<String> methodIterator = methodsNode.fieldNames();

            while (methodIterator.hasNext()) {
                String methodName = methodIterator.next();
                JsonNode methodNode = methodsNode.get(methodName).deepCopy();

                Map<String, String> metadata = Map.of(
                        "chunkType", "PATH_METHOD",
                        "path", path,
                        "method", methodName,
                        "specId", specId);

                embeddingService.add(methodNode.toString(), metadata);
            }
        }
    }

    // Store schema chunks
    public void storeSchemas(JsonNode rootNode, String specId) {
        JsonNode schemasNode = rootNode.path("components").path("schemas");
        Iterator<String> schemaIterator = schemasNode.fieldNames();

        while (schemaIterator.hasNext()) {
            String schemaName = schemaIterator.next();
            JsonNode schemaBody = schemasNode.get(schemaName);
            try {

                String schemaYaml = this.yamlMapper.writeValueAsString(schemaBody);

                Map<String, String> metadata = Map.of(
                        "chunkType", "SCHEMA",
                        "schemaName", schemaName,
                        "specId", specId);

                embeddingService.add(schemaYaml, metadata);
            } catch (JsonProcessingException e) {
                throw new SpecParsingException("Failed to parse OpenAPI spec");
            }
        }
    }

    // Store servers chunk
    private void storeServers(JsonNode rootNode, String specId) {
        JsonNode serversNode = rootNode.path("servers");
        try {
            String serversYaml = this.yamlMapper.writeValueAsString(serversNode);

            Map<String, String> metadata = Map.of(
                    "chunkType", "SERVERS",
                    "specId", specId);

            embeddingService.add(serversYaml, metadata);
        } catch (JsonProcessingException e) {
            throw new SpecParsingException("Failed to parse OpenAPI spec");
        }
    }

    // Store info chunk
    private void storeInfo(JsonNode rootNode, String specId, String name) {
        JsonNode infoNode = rootNode.path("info");
        String openApiVersion = rootNode.path("openapi").asText();

        try {
            String infoYaml = yamlMapper.writeValueAsString(infoNode);

            embeddingService.add(infoYaml, Map.of(
                    "chunkType", "INFO",
                    "specId", specId,
                    "openapi", openApiVersion,
                    "name", name));
        } catch (JsonProcessingException e) {
            throw new SpecParsingException("Failed to parse OpenAPI spec");
        }
    }

    // Merge a single updated operation chunk into the full YAML spec
    @SuppressWarnings("unchecked")
    public String mergeOperation(String currentYaml, String updatedOperationYaml, Endpoint endpoint) {
        try {
            Map<String, Object> specMap = yamlMapper.readValue(currentYaml, Map.class);
            Map<String, Object> pathsMap = (Map<String, Object>) specMap.get("paths");
            Map<String, Object> targetPathMap = (Map<String, Object>) pathsMap.get(endpoint.path());

            Map<String, Object> updatedOperationMap = yamlMapper.readValue(updatedOperationYaml, Map.class);
            targetPathMap.put(endpoint.method().toLowerCase(), updatedOperationMap);

            String mergedYaml = yamlMapper.writeValueAsString(specMap);

            return mergedYaml;
        } catch (JsonProcessingException e) {
            throw new SpecParsingException("Failed to parse OpenAPI spec");
        }
    }

    // Retrive name and version of spec
    public SpecInfo retriveInfo(String specId) {
        SearchResultDTO resultDTO = embeddingService.retrieveInfoChunk(specId);
        String name = (String) resultDTO.metadata().get("name");
        try {
            String version = this.yamlMapper
                    .readTree(resultDTO.content())
                    .path("version").asText();

            return new SpecInfo(name, version);
        } catch (JsonProcessingException e) {
            throw new SpecParsingException("Failed to parse OpenAPI spec");
        }
    }

    // Delete existing chunks and store chunks from updated YAML
    public void overwriteChunks(String specId) {
        String name = retriveInfo(specId).name();

        embeddingService.deletaAllChunks(specId);

        String updatedYaml = specStorageService.readYaml(specId, false);

        parseAndStoreChunks(updatedYaml, specId, name);
    }
}