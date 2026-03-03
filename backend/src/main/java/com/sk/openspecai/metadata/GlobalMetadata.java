package com.sk.openspecai.metadata;

public record GlobalMetadata(
        String specId,
        String chunkType,
        String openApiVersion) implements ChunkMetadata {
}
