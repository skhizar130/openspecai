package com.sk.openspecai.service;

import java.util.Iterator;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.sk.openspecai.dto.SearchResultDTO;
import com.sk.openspecai.model.Endpoint;
import com.sk.openspecai.model.SpecInfo;

@Service
public class ParsingService {

    private final EmbeddingService embeddingService;
    private final ObjectMapper yamlMapper;

    // Constructor initializes YAML mapper and embedding service
    public ParsingService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;

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
    public String parseAndStoreChunks(String yamlContent, String specId, String name)
            throws JsonMappingException, JsonProcessingException {

        JsonNode rootNode = this.yamlMapper.readTree(yamlContent);

        storeGlobalInfo(rootNode, specId, name);
        storeServers(rootNode, specId);
        storeOperations(rootNode, specId);
        storeSchemas(rootNode, specId);

        return yamlMapper.writeValueAsString(rootNode);
    }

    // Store path-method chunks
    public void storeOperations(JsonNode rootNode, String specId)
            throws JsonMappingException, JsonProcessingException {
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
    public void storeSchemas(JsonNode rootNode, String specId)
            throws JsonMappingException, JsonProcessingException {
        JsonNode schemasNode = rootNode.path("components").path("schemas");
        Iterator<String> schemaIterator = schemasNode.fieldNames();

        while (schemaIterator.hasNext()) {
            String schemaName = schemaIterator.next();
            JsonNode schemaBody = schemasNode.get(schemaName);
            String schemaYaml = this.yamlMapper.writeValueAsString(schemaBody);

            Map<String, String> metadata = Map.of(
                    "chunkType", "SCHEMA",
                    "schemaName", schemaName,
                    "specId", specId);

            embeddingService.add(schemaYaml, metadata);
        }
    }

    // Store servers chunk
    private void storeServers(JsonNode rootNode, String specId) throws JsonProcessingException {
        JsonNode serversNode = rootNode.path("servers");
        String serversYaml = this.yamlMapper.writeValueAsString(serversNode);

        Map<String, String> metadata = Map.of(
                "chunkType", "SERVERS",
                "specId", specId);

        embeddingService.add(serversYaml, metadata);
    }

    // Store global info chunk
    private void storeGlobalInfo(JsonNode rootNode, String specId, String name)
            throws JsonProcessingException {
        JsonNode infoNode = rootNode.path("info");
        String openApiVersion = rootNode.path("openapi").asText();
        String infoYaml = yamlMapper.writeValueAsString(infoNode);

        embeddingService.add(infoYaml, Map.of(
                "chunkType", "INFO",
                "specId", specId,
                "openapi", openApiVersion,
                "name", name));
    }

    // Merge a single updated operation chunk into the full YAML spec
    @SuppressWarnings("unchecked")
    public String mergeOperation(String currentYaml, String updatedOperationYaml, Endpoint endpoint)
            throws JsonMappingException, JsonProcessingException {
        Map<String, Object> specMap = yamlMapper.readValue(currentYaml, Map.class);
        Map<String, Object> pathsMap = (Map<String, Object>) specMap.get("paths");
        Map<String, Object> targetPathMap = (Map<String, Object>) pathsMap.get(endpoint.path());

        Map<String, Object> updatedOperationMap = yamlMapper.readValue(updatedOperationYaml, Map.class);
        targetPathMap.put(endpoint.method().toLowerCase(), updatedOperationMap);

        String mergedYaml = yamlMapper.writeValueAsString(specMap);

        return mergedYaml;
    }

    public SpecInfo retriveInfo(String specId) throws Exception {
        SearchResultDTO resultDTO = embeddingService.retrieveInfoChunk(specId);
        String name = (String) resultDTO.metadata().get("name");
        String version = this.yamlMapper
                .readTree(resultDTO.content())
                .path("version").asText();

        return new SpecInfo(name, version);
    }
}