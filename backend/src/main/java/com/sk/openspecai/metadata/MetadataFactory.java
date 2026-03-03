package com.sk.openspecai.metadata;

import java.util.Map;

public class MetadataFactory {

    public static ChunkMetadata from(Map<String, Object> metadataMap) {
        String chunkType = (String) metadataMap.get("chunkType");
        String specId = (String) metadataMap.get("specId");

        return switch (chunkType) {
            case "GLOBAL_INFO" -> new GlobalMetadata(specId,
                    chunkType,
                    (String) metadataMap.getOrDefault("openapi", ""));

            case "SERVERS" -> new GlobalMetadata(specId, chunkType, "");

            case "SCHEMA" -> new SchemaMetadata(
                    specId,
                    chunkType,
                    (String) metadataMap.get("schemaName"));

            case "PATH_METHOD" -> new PathMetadata(
                    specId,
                    chunkType,
                    (String) metadataMap.get("path"),
                    (String) metadataMap.get("method"));

            default -> throw new IllegalArgumentException("Unknown chunkType: " + chunkType);

        };
    }
}
